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
package org.apache.commons.compress.harmony.unpack200;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BandSetTest {

    public class MockSegment extends Segment {

        @Override
        public SegmentHeader getSegmentHeader() {
            return new SegmentHeader(this);
        }
    }

    private final BandSet bandSet = new BandSet(new MockSegment()) {

        @Override
        public void read(final InputStream inputStream) throws IOException, Pack200Exception {
        }

        @Override
        public void unpack() throws IOException, Pack200Exception {
        }

    };

    @Test
    void testDecodeBandInt() throws IOException, Pack200Exception {
        final BHSDCodec codec = Codec.BYTE1;
        final byte[] bytes = { (byte) 3, (byte) 56, (byte) 122, (byte) 78 };
        final InputStream in = new ByteArrayInputStream(bytes);
        final int[] ints = bandSet.decodeBandInt("Test Band", in, codec, 4);
        for (int i = 0; i < ints.length; i++) {
            assertEquals(ints[i], bytes[i], "Wrong value in position " + i);
        }
    }

    @Test
    void testDecodeBandIntRejectsNegativeCount() {
        final BHSDCodec codec = Codec.BYTE1;
        // A per-element count decoded from a signed band can be negative. It must be rejected the
        // same way the scalar decodeBandInt(..., int) overload rejects count < 0, instead of
        // reaching new int[]/new long[]/new String[] and surfacing a NegativeArraySizeException.
        assertThrows(Pack200Exception.class,
                () -> bandSet.decodeBandInt("Test", new ByteArrayInputStream(new byte[0]), codec, new int[] { -1, 1 }));
        assertThrows(Pack200Exception.class,
                () -> bandSet.parseFlags("Test", new ByteArrayInputStream(new byte[0]), new int[] { -1, 1 }, codec, false));
        assertThrows(Pack200Exception.class,
                () -> bandSet.parseReferences("Test", new ByteArrayInputStream(new byte[0]), codec, new int[] { -1, 1 }, new String[] { "a" }));
    }

    @Test
    void testGetReferencesRejectsOutOfRangeIndex() throws Exception {
        // getReferences resolves band-decoded indices into a constant-pool array. An index at or past the
        // end of that array must be rejected the same way the sibling parseReferences rejects it, instead of
        // reaching reference[index] and surfacing an ArrayIndexOutOfBoundsException that escapes the declared
        // Pack200Exception contract.
        final String[] reference = { "a", "b" };
        assertThrows(Pack200Exception.class, () -> bandSet.getReferences(new int[] { 2 }, reference));
        assertThrows(Pack200Exception.class, () -> bandSet.getReferences(new int[] { -1 }, reference));
        assertThrows(Pack200Exception.class, () -> bandSet.getReferences(new int[][] { { 2 } }, reference));
        // A valid index still resolves.
        assertEquals("b", bandSet.getReferences(new int[] { 1 }, reference)[0]);
    }

    @Test
    @Disabled("TODO: Implement")
    void testParseFlags1() {

    }

    @Test
    @Disabled("TODO: Implement")
    void testParseFlags2() {

    }

    @Test
    @Disabled("TODO: Implement")
    void testParseFlags3() {

    }

    @Test
    @Disabled("TODO: Implement")
    void testParseReferences1() {

    }

    @Test
    @Disabled("TODO: Implement")
    void testParseReferences2() {

    }

}
