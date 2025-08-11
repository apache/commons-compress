/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveException;

final class AES256SHA256Decoder extends AbstractCoder {

    private static final class AES256SHA256DecoderInputStream extends InputStream {

        /**
         * See {@code 7z2500-src/CPP/7zip/Crypto/7zAes.cpp}.
         *
         * <pre>static const unsigned k_NumCyclesPower_Supported_MAX = 24;</pre>
         */
        private static final int NUM_CYCLES_POWER_MAX = 24;
        private static final int NUM_CYCLES_POWER_SPECIAL = 0x3f;

        private final InputStream in;
        private final Coder coder;
        private final String archiveName;
        private final byte[] passwordBytes;
        private boolean isInitialized;
        private CipherInputStream cipherInputStream;

        private AES256SHA256DecoderInputStream(final InputStream in, final Coder coder, final String archiveName, final byte[] passwordBytes) {
            this.in = in;
            this.coder = coder;
            this.archiveName = archiveName;
            this.passwordBytes = passwordBytes;
        }

        @Override
        public void close() throws IOException {
            if (cipherInputStream != null) {
                cipherInputStream.close();
            }
        }

        private CipherInputStream init() throws IOException {
            if (isInitialized) {
                return cipherInputStream;
            }
            if (coder.properties == null) {
                throw new ArchiveException("Missing AES256 properties in '%s'", archiveName);
            }
            if (coder.properties.length < 2) {
                throw new ArchiveException("AES256 properties too short in '%s'", archiveName);
            }
            final int byte0 = 0xff & coder.properties[0];
            final int numCyclesPower = byte0 & NUM_CYCLES_POWER_SPECIAL;
            if (numCyclesPower > NUM_CYCLES_POWER_MAX && numCyclesPower != NUM_CYCLES_POWER_SPECIAL) {
                throw new ArchiveException("numCyclesPower %,d exceeds supported limit (%d) in '%s'", numCyclesPower, NUM_CYCLES_POWER_MAX, archiveName);
            }
            final int byte1 = 0xff & coder.properties[1];
            final int ivSize = (byte0 >> 6 & 1) + (byte1 & 0x0f);
            final int saltSize = (byte0 >> 7 & 1) + (byte1 >> 4);
            if (2 + saltSize + ivSize > coder.properties.length) {
                throw new ArchiveException("Salt size + IV size too long in '%s'", archiveName);
            }
            final byte[] salt = new byte[saltSize];
            System.arraycopy(coder.properties, 2, salt, 0, saltSize);
            final byte[] iv = new byte[16];
            System.arraycopy(coder.properties, 2 + saltSize, iv, 0, ivSize);
            if (passwordBytes == null) {
                throw new PasswordRequiredException(archiveName);
            }
            final byte[] aesKeyBytes;
            if (numCyclesPower == NUM_CYCLES_POWER_SPECIAL) {
                aesKeyBytes = new byte[32];
                System.arraycopy(salt, 0, aesKeyBytes, 0, saltSize);
                System.arraycopy(passwordBytes, 0, aesKeyBytes, saltSize, Math.min(passwordBytes.length, aesKeyBytes.length - saltSize));
            } else {
                aesKeyBytes = sha256Password(passwordBytes, numCyclesPower, salt);
            }
            final SecretKey aesKey = AES256Options.newSecretKeySpec(aesKeyBytes);
            try {
                final Cipher cipher = Cipher.getInstance(AES256Options.TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
                cipherInputStream = new CipherInputStream(in, cipher);
                isInitialized = true;
                return cipherInputStream;
            } catch (final GeneralSecurityException generalSecurityException) {
                throw new IllegalStateException("Decryption error (do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
                        generalSecurityException);
            }
        }

        @SuppressWarnings("resource") // Closed in close()
        @Override
        public int read() throws IOException {
            return init().read();
        }

        @SuppressWarnings("resource") // Closed in close()
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return init().read(b, off, len);
        }
    }

    private static final class AES256SHA256DecoderOutputStream extends OutputStream {
        private final CipherOutputStream cipherOutputStream;
        // Ensures that data are encrypt in respect of cipher block size and pad with '0' if smaller
        // NOTE: As "AES/CBC/PKCS5Padding" is weak and should not be used, we use "AES/CBC/NoPadding" with this
        // manual implementation for padding possible thanks to the size of the file stored separately
        private final int cipherBlockSize;
        private final byte[] cipherBlockBuffer;
        private int count;

        private AES256SHA256DecoderOutputStream(final AES256Options opts, final OutputStream out) {
            cipherOutputStream = new CipherOutputStream(out, opts.getCipher());
            cipherBlockSize = opts.getCipher().getBlockSize();
            cipherBlockBuffer = new byte[cipherBlockSize];
        }

        @Override
        public void close() throws IOException {
            if (count > 0) {
                cipherOutputStream.write(cipherBlockBuffer);
            }
            cipherOutputStream.close();
        }

        @Override
        public void flush() throws IOException {
            cipherOutputStream.flush();
        }

        private void flushBuffer() throws IOException {
            cipherOutputStream.write(cipherBlockBuffer);
            count = 0;
            Arrays.fill(cipherBlockBuffer, (byte) 0);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            int gap = len + count > cipherBlockSize ? cipherBlockSize - count : len;
            System.arraycopy(b, off, cipherBlockBuffer, count, gap);
            count += gap;
            if (count == cipherBlockSize) {
                flushBuffer();
                if (len - gap >= cipherBlockSize) {
                    // skip buffer to encrypt data chunks big enough to fit cipher block size
                    final int multipleCipherBlockSizeLen = (len - gap) / cipherBlockSize * cipherBlockSize;
                    cipherOutputStream.write(b, off + gap, multipleCipherBlockSizeLen);
                    gap += multipleCipherBlockSizeLen;
                }
                System.arraycopy(b, off + gap, cipherBlockBuffer, 0, len - gap);
                count = len - gap;
            }
        }

        @Override
        public void write(final int b) throws IOException {
            cipherBlockBuffer[count++] = (byte) b;
            if (count == cipherBlockSize) {
                flushBuffer();
            }
        }
    }

    static byte[] sha256Password(final byte[] password, final int numCyclesPower, final byte[] salt) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 is unsupported by your Java implementation", noSuchAlgorithmException);
        }
        final byte[] extra = new byte[8];
        for (long j = 0; j < 1L << numCyclesPower; j++) {
            digest.update(salt);
            digest.update(password);
            digest.update(extra);
            for (int k = 0; k < extra.length; k++) {
                ++extra[k];
                if (extra[k] != 0) {
                    break;
                }
            }
        }
        return digest.digest();
    }

    static byte[] sha256Password(final char[] password, final int numCyclesPower, final byte[] salt) {
        return sha256Password(utf16Decode(password), numCyclesPower, salt);
    }

    /**
     * Convenience method that encodes Unicode characters into bytes in UTF-16 (little-endian byte order) charset
     *
     * @param chars characters to encode
     * @return encoded characters
     */
    static byte[] utf16Decode(final char[] chars) {
        if (chars == null) {
            return null;
        }
        final ByteBuffer encoded = StandardCharsets.UTF_16LE.encode(CharBuffer.wrap(chars));
        if (encoded.hasArray()) {
            return encoded.array();
        }
        final byte[] e = new byte[encoded.remaining()];
        encoded.get(e);
        return e;
    }

    AES256SHA256Decoder() {
        super(AES256Options.class);
    }

    @Override
    InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] passwordBytes,
            final int maxMemoryLimitKiB) {
        return new AES256SHA256DecoderInputStream(in, coder, archiveName, passwordBytes);
    }

    @Override
    OutputStream encode(final OutputStream out, final Object options) throws IOException {
        return new AES256SHA256DecoderOutputStream((AES256Options) options, out);
    }

    @Override
    byte[] getOptionsAsProperties(final Object options) throws IOException {
        final AES256Options opts = (AES256Options) options;
        final byte[] salt = opts.getSalt();
        final int saltLen = salt.length;
        final byte[] iv = opts.getIv();
        final int ivLen = iv.length;
        final byte[] props = new byte[2 + saltLen + ivLen];
        // First byte : control (numCyclesPower + flags of salt or iv presence)
        props[0] = (byte) (opts.getNumCyclesPower() | (saltLen == 0 ? 0 : 1 << 7) | (ivLen == 0 ? 0 : 1 << 6));
        if (saltLen != 0 || ivLen != 0) {
            // second byte : size of salt/iv data
            props[1] = (byte) ((saltLen == 0 ? 0 : saltLen - 1) << 4 | (ivLen == 0 ? 0 : ivLen - 1));
            // remain bytes : salt/iv data
            System.arraycopy(salt, 0, props, 2, saltLen);
            System.arraycopy(iv, 0, props, 2 + saltLen, ivLen);
        }
        return props;
    }
}
