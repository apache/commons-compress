/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link ChecksumCalculatingInputStream org.apache.commons.compress.utils.ChecksumCalculatingInputStream}.
 *
 * @see ChecksumCalculatingInputStream
 */
public class ChecksumCalculatingInputStreamTest {

    @Test
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionOne() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(null, null));
    }

    @Test
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionThree() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(new CRC32(), null));
    }

    @Test
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionTwo() {
        assertThrows(NullPointerException.class, () -> new ChecksumCalculatingInputStream(null, new ByteArrayInputStream(new byte[1])));
    }


    @Test
    public void testReadTakingByteArray() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        final ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        final int readResult = checksumCalculatingInputStream.read(byteArray);
        assertEquals(6, readResult);
        assertEquals(0, byteArrayInputStream.available());
        assertEquals(393217L, checksumCalculatingInputStream.getValue());
    }

    @Test
    public void testReadTakingNoArguments() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        final ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(checksumCalculatingInputStream);
        final int inputStreamReadResult = bufferedInputStream.read(byteArray, 0, 1);
        final int checkSumCalculationReadResult = checksumCalculatingInputStream.read();
        assertNotEquals(checkSumCalculationReadResult, inputStreamReadResult);
        assertEquals((-1), checkSumCalculationReadResult);
        assertEquals(0, byteArrayInputStream.available());
        assertEquals(393217L, checksumCalculatingInputStream.getValue());
    }

    @Test
    public void testSkipReturningPositive() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] byteArray = new byte[6];
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        final ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        final long skipResult = checksumCalculatingInputStream.skip((byte)0);
        assertEquals(1L, skipResult);
        assertEquals(65537L, checksumCalculatingInputStream.getValue());
    }

    @Test
    public void testSkipReturningZero() throws IOException {
        final Adler32 adler32 = new Adler32();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY);
        final ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        final long skipResult = checksumCalculatingInputStream.skip(60L);
        assertEquals(0L, skipResult);
        assertEquals(1L, checksumCalculatingInputStream.getValue());
    }

}