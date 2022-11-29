/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.compress.PasswordRequiredException;

class AES256SHA256Decoder extends CoderBase {
    
    AES256SHA256Decoder() {
        super(AES256Options.class);
    }

    @Override
    InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength,
            final Coder coder, final byte[] passwordBytes, final int maxMemoryLimitInKb) {
        return new InputStream() {
            private boolean isInitialized;
            private CipherInputStream cipherInputStream;

            private CipherInputStream init() throws IOException {
                if (isInitialized) {
                    return cipherInputStream;
                }
                if (coder.properties == null) {
                    throw new IOException("Missing AES256 properties in " + archiveName);
                }
                if (coder.properties.length < 2) {
                    throw new IOException("AES256 properties too short in " + archiveName);
                }
                final int byte0 = 0xff & coder.properties[0];
                final int numCyclesPower = byte0 & 0x3f;
                final int byte1 = 0xff & coder.properties[1];
                final int ivSize = ((byte0 >> 6) & 1) + (byte1 & 0x0f);
                final int saltSize = ((byte0 >> 7) & 1) + (byte1 >> 4);
                if (2 + saltSize + ivSize > coder.properties.length) {
                    throw new IOException("Salt size + IV size too long in " + archiveName);
                }
                final byte[] salt = new byte[saltSize];
                System.arraycopy(coder.properties, 2, salt, 0, saltSize);
                final byte[] iv = new byte[16];
                System.arraycopy(coder.properties, 2 + saltSize, iv, 0, ivSize);

                if (passwordBytes == null) {
                    throw new PasswordRequiredException(archiveName);
                }
                final byte[] aesKeyBytes;
                if (numCyclesPower == 0x3f) {
                    aesKeyBytes = new byte[32];
                    System.arraycopy(salt, 0, aesKeyBytes, 0, saltSize);
                    System.arraycopy(passwordBytes, 0, aesKeyBytes, saltSize,
                                     Math.min(passwordBytes.length, aesKeyBytes.length - saltSize));
                } else {
                    aesKeyBytes = sha256Password(passwordBytes, numCyclesPower, salt);
                }

                final SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
                try {
                    final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
                    cipherInputStream = new CipherInputStream(in, cipher);
                    isInitialized = true;
                    return cipherInputStream;
                } catch (final GeneralSecurityException generalSecurityException) {
                    throw new IOException("Decryption error " +
                        "(do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
                        generalSecurityException);
                    }
            }

            @Override
            public int read() throws IOException {
                return init().read();
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                return init().read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                if (cipherInputStream != null) {
                    cipherInputStream.close();
                }
            }
        };
    }

    @Override
    OutputStream encode(OutputStream out, Object options) throws IOException {
        AES256Options opts = (AES256Options) options;
        final byte[] aesKeyBytes = sha256Password(opts.password, opts.numCyclesPower, opts.salt);
        final SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        final Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(opts.iv));
        } catch (final GeneralSecurityException generalSecurityException) {
            throw new IOException(
                "Encryption error " + "(do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
                generalSecurityException
            );
        }
        return new OutputStream() {
            final CipherOutputStream cipherOutputStream = new CipherOutputStream(out, cipher);

            // Ensures that data are encrypt in respect of cipher block size and pad with '0' if smaller
            // NOTE: As "AES/CBC/PKCS5Padding" is weak and should not be used, we use "AES/CBC/NoPadding" with this 
            // manual implementation for padding possible thanks to the size of the file stored separately
            final int cipherBlockSize = cipher.getBlockSize();
            final byte[]  cipherBlockBuffer = new byte[cipherBlockSize];
            private int count = 0;

            @Override
            public void write(int b) throws IOException {
                cipherBlockBuffer[count++] = (byte) b;
                if (count == cipherBlockSize) {
                    flushBuffer();
                } 
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                int gap = len + count > cipherBlockSize ? cipherBlockSize - count : len;
                System.arraycopy(b, off, cipherBlockBuffer, count, gap);
                count += gap;

                if (count == cipherBlockSize) {
                    flushBuffer();

                    if (len - gap >= cipherBlockSize) {
                        // skip buffer to encrypt data chunks big enought to fit cipher block size
                        int multipleCipherBlockSizeLen = (len - gap) / cipherBlockSize * cipherBlockSize;
                        cipherOutputStream.write(b, off + gap, multipleCipherBlockSizeLen);
                        gap += multipleCipherBlockSizeLen;
                    }
                    System.arraycopy(b, off + gap, cipherBlockBuffer, 0, len - gap);
                    count = len - gap;
                }
            }

            private void flushBuffer() throws IOException {
                cipherOutputStream.write(cipherBlockBuffer);
                count = 0;
                Arrays.fill(cipherBlockBuffer, (byte) 0);
            }

            @Override
            public void flush() throws IOException {
                cipherOutputStream.flush();
            }

            @Override
            public void close() throws IOException {
                if (count > 0) {
                    cipherOutputStream.write(cipherBlockBuffer);
                }
                cipherOutputStream.close();
            }
        };
    }

    private byte[] sha256Password(final byte[] password, final int numCyclesPower, final byte[] salt) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IOException("SHA-256 is unsupported by your Java implementation", noSuchAlgorithmException);
        }
        final byte[] extra = new byte[8];
        for (long j = 0; j < (1L << numCyclesPower); j++) {
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

    @Override
    byte[] getOptionsAsProperties(Object options) throws IOException {
        AES256Options opts = (AES256Options) options;
        byte[] props = new byte[2 + opts.salt.length + opts.iv.length];

        // First byte : control (numCyclesPower + flags of salt or iv presence)
        props[0] = (byte) (opts.numCyclesPower | (opts.salt.length == 0 ? 0 : (1 << 7)) | (opts.iv.length == 0 ? 0 : (1 << 6)));

        if (opts.salt.length != 0 || opts.iv.length != 0) {
            // second byte : size of salt/iv data
            props[1] = (byte) (((opts.salt.length == 0 ? 0 : opts.salt.length - 1) << 4) | (opts.iv.length == 0 ? 0 : opts.iv.length - 1));

            // remain bytes : salt/iv data
            System.arraycopy(opts.salt, 0, props, 2, opts.salt.length);
            System.arraycopy(opts.iv, 0, props, 2 + opts.salt.length, opts.iv.length);
        }

        return props;
    }
}
