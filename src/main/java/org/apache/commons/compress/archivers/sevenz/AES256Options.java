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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Options for {@link SevenZMethod#AES256SHA256} encoder
 *
 * @since 1.23
 * @see AES256SHA256Decoder
 */
class AES256Options {

    static final String ALGORITHM = "AES";

    static final String TRANSFORMATION = "AES/CBC/NoPadding";

    static SecretKeySpec newSecretKeySpec(final byte[] bytes) {
        return new SecretKeySpec(bytes, ALGORITHM);
    }
    private static byte[] randomBytes(final int size) {
        final byte[] bytes = new byte[size];
        try {
            SecureRandom.getInstanceStrong().nextBytes(bytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("No strong secure random available to generate strong AES key", e);
        }
        return bytes;
    }
    private final byte[] salt;
    private final byte[] iv;

    private final int numCyclesPower;

    private final Cipher cipher;

    /**
     * @param password password used for encryption
     */
    public AES256Options(final char[] password) {
        this(password, new byte[0], randomBytes(16), 19);
    }

    /**
     * @param password password used for encryption
     * @param salt for password hash salting (enforce password security)
     * @param iv Initialization Vector (IV) used by cipher algorithm
     * @param numCyclesPower another password security enforcer parameter that controls the cycles of password hashing. More the
     *                       this number is high, more security you'll have but also high CPU usage
     */
    public AES256Options(final char[] password, final byte[] salt, final byte[] iv, final int numCyclesPower) {
        this.salt = salt;
        this.iv = iv;
        this.numCyclesPower = numCyclesPower;

        // NOTE: for security purposes, password is wrapped in a Cipher as soon as possible to not stay in memory
        final byte[] aesKeyBytes = AES256SHA256Decoder.sha256Password(password, numCyclesPower, salt);
        final SecretKey aesKey = newSecretKeySpec(aesKeyBytes);

        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
        } catch (final GeneralSecurityException generalSecurityException) {
            throw new IllegalStateException(
                "Encryption error (do you have the JCE Unlimited Strength Jurisdiction Policy Files installed?)",
                generalSecurityException
            );
        }
    }

    Cipher getCipher() {
        return cipher;
    }

    byte[] getIv() {
        return iv;
    }

    int getNumCyclesPower() {
        return numCyclesPower;
    }

    byte[] getSalt() {
        return salt;
    }
}
