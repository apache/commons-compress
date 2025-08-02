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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link AES256SHA256Decoder}.
 *
 * @see AES256SHA256Decoder
 **/
class SevenZAES256SHA256DecoderTest {

    @Test
    void testDecodeWithNonEmptyString() throws IOException {
        final AES256SHA256Decoder aES256SHA256Decoder = new AES256SHA256Decoder();
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(null, 3138)) {
            final byte[] byteArray = new byte[8];
            byteArray[1] = (byte) -72;
            final Coder coder = new Coder(null, 0, 0, byteArray);
            try (InputStream inputStream = aES256SHA256Decoder.decode("x", bufferedInputStream, 3138, coder, coder.properties, Integer.MAX_VALUE)) {
                final IOException e = assertThrows(ArchiveException.class, () -> new ObjectInputStream(inputStream), "Expecting exception: IOException");
                assertEquals("Salt size + IV size too long in 'x'", e.getMessage());
                assertEquals("org.apache.commons.compress.archivers.sevenz.AES256SHA256Decoder$AES256SHA256DecoderInputStream",
                        e.getStackTrace()[0].getClassName());
            }
        }
    }
}
