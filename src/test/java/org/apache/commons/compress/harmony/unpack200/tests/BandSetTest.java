/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.BandSet;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentHeader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BandSetTest {

    public class MockSegment extends Segment {

        @Override
        public SegmentHeader getSegmentHeader() {
            return new SegmentHeader(this);
        }
    }

    private final BandSet bandSet = new BandSet(new MockSegment()) {

        @Override
        public void read(final InputStream inputStream) throws IOException,
                Pack200Exception {
        }

        @Override
        public void unpack() throws IOException, Pack200Exception {
        }

    };

    @Test
    public void testDecodeBandInt() throws IOException, Pack200Exception {
        final BHSDCodec codec = Codec.BYTE1;
        final byte[] bytes = { (byte) 3, (byte) 56, (byte) 122, (byte) 78 };
        final InputStream in = new ByteArrayInputStream(bytes);
        final int[] ints = bandSet.decodeBandInt("Test Band", in, codec, 4);
        for (int i = 0; i < ints.length; i++) {
            assertEquals(ints[i], bytes[i], "Wrong value in position " + i);
        }
    }

    @Test
    @Disabled("TODO: Implement")
    public void testParseFlags1() {

    }

    @Test
    @Disabled("TODO: Implement")
    public void testParseFlags2() {

    }

    @Test
    @Disabled("TODO: Implement")
    public void testParseFlags3() {

    }

    @Test
    @Disabled("TODO: Implement")
    public void testParseReferences1() {

    }

    @Test
    @Disabled("TODO: Implement")
    public void testParseReferences2() {

    }

}
