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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for class {@link ChecksumVerifyingInputStream org.apache.commons.compress.utils.ChecksumVerifyingInputStream}.
 *
 * @date 13.06.2017
 * @see ChecksumVerifyingInputStream
 **/
public class ChecksumVerifyingInputStreamTest {



    @Test(expected = IOException.class)
    public void testReadTakingByteArrayThrowsIOException() throws IOException {

        Adler32 adler32 = new Adler32();
        byte[] byteArray = new byte[3];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumVerifyingInputStream checksumVerifyingInputStream = new ChecksumVerifyingInputStream(adler32, byteArrayInputStream, (-1859L), (byte) (-68));

        checksumVerifyingInputStream.read(byteArray);

    }


    @Test(expected = IOException.class)
    public void testReadTakingNoArgumentsThrowsIOException() throws IOException {

        CRC32 cRC32_ = new CRC32();
        byte[] byteArray = new byte[9];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumVerifyingInputStream checksumVerifyingInputStream = new ChecksumVerifyingInputStream(cRC32_, byteArrayInputStream, (byte)1, (byte)1);

        checksumVerifyingInputStream.read();

    }


    @Test
    public void testSkip() throws IOException {

        CRC32 cRC32_ = new CRC32();
        byte[] byteArray = new byte[4];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ChecksumVerifyingInputStream checksumVerifyingInputStream = new ChecksumVerifyingInputStream(cRC32_, byteArrayInputStream, (byte)33, 2303L);
        int intOne = checksumVerifyingInputStream.read(byteArray);

        long skipReturnValue = checksumVerifyingInputStream.skip((byte)1);

        assertEquals(558161692L, cRC32_.getValue());
        assertEquals(0, byteArrayInputStream.available());

        assertArrayEquals(new byte[] {(byte)0, (byte)0, (byte)0, (byte)0}, byteArray);
        assertEquals(0L, skipReturnValue);

    }



}