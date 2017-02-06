/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.lz77support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AbstractLZ77CompressorInputStreamTest {

    private static class TestStream extends AbstractLZ77CompressorInputStream {
        private boolean literal;
        TestStream(InputStream in) throws IOException {
            super(in, 1024);
        }
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (literal) {
                return readLiteral(b, off, len);
            }
            return readBackReference(b, off, len);
        }
        void literal(int len) {
            startLiteral(len);
            literal = true;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void cantPrefillAfterDataHasBeenRead() throws IOException {
        byte[] data = new byte[] { 1, 2, 3, 4 };
        try (TestStream s = new TestStream(new ByteArrayInputStream(data))) {
            s.literal(3);
            assertEquals(1, s.read());
            s.prefill(new byte[] { 1, 2, 3 });
        }
    }

    @Test
    public void prefillCanBeUsedForBackReferences() throws IOException {
        byte[] data = new byte[] { 1, 2, 3, 4 };
        try (TestStream s = new TestStream(new ByteArrayInputStream(new byte[0]))) {
            s.prefill(data);
            s.startBackReference(2, 4);
            byte[] r = new byte[4];
            assertEquals(4, s.read(r));
            assertArrayEquals(new byte[] { 3, 4, 3, 4 }, r);
        }
    }

    @Test
    public void ifPrefillExceedsWindowSizeTheLastBytesAreUsed() throws IOException {
        byte[] data = new byte[2048];
        data[2046] = 3;
        data[2047] = 4;
        try (TestStream s = new TestStream(new ByteArrayInputStream(new byte[0]))) {
            s.prefill(data);
            s.startBackReference(2, 4);
            byte[] r = new byte[4];
            assertEquals(4, s.read(r));
            assertArrayEquals(new byte[] { 3, 4, 3, 4 }, r);
        }
    }
}
