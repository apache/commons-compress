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
package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import org.junit.Test;

public class StreamCompressorTest {

    @Test
    public void storedEntries() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final StreamCompressor sc = StreamCompressor.create( baos);
        sc.deflate(new ByteArrayInputStream("A".getBytes()), ZipEntry.STORED);
        sc.deflate(new ByteArrayInputStream("BAD".getBytes()), ZipEntry.STORED);
        assertEquals(3, sc.getBytesRead());
        assertEquals(3, sc.getBytesWrittenForLastEntry());
        assertEquals(344750961, sc.getCrc32());
        sc.deflate(new ByteArrayInputStream("CAFE".getBytes()), ZipEntry.STORED);
        assertEquals("ABADCAFE", baos.toString());
   }

    @Test
    public void deflatedEntries() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final StreamCompressor sc = StreamCompressor.create( baos);
        sc.deflate(new ByteArrayInputStream("AAAAAABBBBBB".getBytes()), ZipEntry.DEFLATED);
        assertEquals(12, sc.getBytesRead());
        assertEquals(8, sc.getBytesWrittenForLastEntry());
        assertEquals(3299542, sc.getCrc32());

        final byte[] actuals = baos.toByteArray();
        final byte[] expected = new byte[]{115,116,4,1,39,48,0,0};
        // Note that this test really asserts stuff about the java Deflater, which might be a little bit brittle
        assertArrayEquals(expected, actuals);
    }

    @Test
    public void testCreateDataOutputCompressor() throws IOException {
        DataOutput dataOutputStream = new DataOutputStream(new ByteArrayOutputStream());
        try (StreamCompressor streamCompressor = StreamCompressor
            .create(dataOutputStream, new Deflater(9))) {
            assertNotNull(streamCompressor);
        }
    }
}
