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
package org.apache.commons.compress.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link ChecksumCalculatingInputStream org.apache.commons.compress.utils.ChecksumCalculatingInputStream}.
 *
 * @see ChecksumCalculatingInputStream
 */
class ChecksumCalculatingInputStreamTest {

    @Test
    void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionOne() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(null, null));
    }

    @Test
    void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionThree() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(new CRC32(), null));
    }

    @Test
    void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionTwo() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(null, new ByteArrayInputStream(new byte[1])));
    }

    @Test
    void testReadTakingByteArray() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        try (ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream)) {
            final int readResult = checksumCalculatingInputStream.read(byteArray);
            assertEquals(6, readResult);
            assertEquals(0, byteArrayInputStream.available());
            assertEquals(393217L, checksumCalculatingInputStream.getValue());
        }
    }

    @Test
    void testReadTakingByteArraySanityCheck() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        try (CheckedInputStream checksumCalculatingInputStream = new CheckedInputStream(byteArrayInputStream, adler32)) {
            final int readResult = checksumCalculatingInputStream.read(byteArray);
            assertEquals(6, readResult);
            assertEquals(0, byteArrayInputStream.available());
            assertEquals(393217L, checksumCalculatingInputStream.getChecksum().getValue());
        }
    }

    @Test
    void testReadTakingNoArguments() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        final ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(checksumCalculatingInputStream)) {
            final int inputStreamReadResult = bufferedInputStream.read(byteArray, 0, 1);
            final int checkSumCalculationReadResult = checksumCalculatingInputStream.read();
            assertNotEquals(checkSumCalculationReadResult, inputStreamReadResult);
            assertEquals(-1, checkSumCalculationReadResult);
            assertEquals(0, byteArrayInputStream.available());
            assertEquals(393217L, checksumCalculatingInputStream.getValue());
        }
    }

    @Test
    void testReadTakingNoArgumentsSanityCheck() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        final CheckedInputStream checksumCalculatingInputStream = new CheckedInputStream(byteArrayInputStream, adler32);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(checksumCalculatingInputStream)) {
            final int inputStreamReadResult = bufferedInputStream.read(byteArray, 0, 1);
            final int checkSumCalculationReadResult = checksumCalculatingInputStream.read();
            assertNotEquals(checkSumCalculationReadResult, inputStreamReadResult);
            assertEquals(-1, checkSumCalculationReadResult);
            assertEquals(0, byteArrayInputStream.available());
            assertEquals(393217L, checksumCalculatingInputStream.getChecksum().getValue());
        }
    }

    @Test
    void testSkipReturningPositive() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        try (ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream)) {
            final long skipResult = checksumCalculatingInputStream.skip((byte) 0);
            assertEquals(0, skipResult);
            assertEquals(1, checksumCalculatingInputStream.getValue());
        }
    }

    @Test
    void testSkipReturningPositiveSanityCheck() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        try (CheckedInputStream checksumCalculatingInputStream = new CheckedInputStream(byteArrayInputStream, adler32)) {
            final long skipResult = checksumCalculatingInputStream.skip((byte) 0);
            assertEquals(0, skipResult);
            assertEquals(1, checksumCalculatingInputStream.getChecksum().getValue());
        }
    }

    @Test
    void testSkipReturningZero() throws IOException {
        final Adler32 adler32 = new Adler32();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY);
        try (ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream)) {
            final long skipResult = checksumCalculatingInputStream.skip(60L);
            assertEquals(0L, skipResult);
            assertEquals(1L, checksumCalculatingInputStream.getValue());
        }
    }

    @Test
    void testSkipReturningZeroSanityCheck() throws IOException {
        final Adler32 adler32 = new Adler32();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY);
        try (CheckedInputStream checksumCalculatingInputStream = new CheckedInputStream(byteArrayInputStream, adler32)) {
            final long skipResult = checksumCalculatingInputStream.skip(60L);
            assertEquals(0L, skipResult);
            assertEquals(1L, checksumCalculatingInputStream.getChecksum().getValue());
        }
    }

}
