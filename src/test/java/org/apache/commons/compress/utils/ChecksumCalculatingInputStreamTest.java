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

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link ChecksumCalculatingInputStream org.apache.commons.compress.utils.ChecksumCalculatingInputStream}.
 *
 * @date 13.06.2017
 * @see ChecksumCalculatingInputStream
 **/
public class ChecksumCalculatingInputStreamTest {



    @Test
    public void testSkipReturningZero() throws IOException {

        Adler32 adler32 = new Adler32();
        byte[] byteArray = new byte[0];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        long skipResult = checksumCalculatingInputStream.skip(60L);

        assertEquals(0L, skipResult);

        assertEquals(1L, checksumCalculatingInputStream.getValue());


    }


    @Test
    public void testSkipReturningPositive() throws IOException {

        Adler32 adler32 = new Adler32();
        byte[] byteArray = new byte[6];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        long skipResult = checksumCalculatingInputStream.skip((byte)0);

        assertEquals(1L, skipResult);

        assertEquals(65537L, checksumCalculatingInputStream.getValue());

    }


    @Test
    public void testReadTakingNoArguments() throws IOException {

        Adler32 adler32 = new Adler32();
        byte[] byteArray = new byte[6];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(checksumCalculatingInputStream);
        int inputStreamReadResult = bufferedInputStream.read(byteArray, 0, 1);
        int checkSumCalculationReadResult = checksumCalculatingInputStream.read();

        assertFalse(checkSumCalculationReadResult == inputStreamReadResult);
        assertEquals((-1), checkSumCalculationReadResult);

        assertEquals(0, byteArrayInputStream.available());

        assertEquals(393217L, checksumCalculatingInputStream.getValue());

    }


    @Test
    public void testReadTakingByteArray() throws IOException {

        Adler32 adler32 = new Adler32();
        byte[] byteArray = new byte[6];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(adler32, byteArrayInputStream);
        int readResult = checksumCalculatingInputStream.read(byteArray);

        assertEquals(6, readResult);

        assertEquals(0, byteArrayInputStream.available());
        assertEquals(393217L, checksumCalculatingInputStream.getValue());

    }


    @Test(expected = NullPointerException.class)
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionOne() {

        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(null,null);


    }


    @Test(expected = NullPointerException.class)
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionTwo() {

        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(null,new ByteArrayInputStream(new byte[1]));


    }


    @Test(expected = NullPointerException.class)
    public void testClassInstantiationWithParameterBeingNullThrowsNullPointerExceptionThree() {

        ChecksumCalculatingInputStream checksumCalculatingInputStream = new ChecksumCalculatingInputStream(new CRC32(),null);

    }


}