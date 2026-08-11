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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.api.Test;

class CpBandsTest extends AbstractBandsTest {

    private final class CpUTF8Header extends MockSegmentHeader {

        CpUTF8Header(final Segment segment) {
            super(segment);
        }

        @Override
        public int getCpUTF8Count() {
            return 3;
        }
    }

    private final class CpUTF8Segment extends MockSegment {

        private final SegmentHeader header = new CpUTF8Header(this);

        @Override
        public SegmentHeader getSegmentHeader() {
            return header;
        }
    }

    @Test
    void testParseCpUtf8RejectsOutOfRangePrefix() throws Exception {
        // The cpUTF8Prefix band is decoded with the signed DELTA5 codec, so a corrupt archive can make a
        // prefix larger than the preceding string (or negative). Used directly as a String.substring end
        // index that raised a raw StringIndexOutOfBoundsException out of the declared Pack200Exception
        // contract; it must now be rejected as corrupt input.
        final CpBands bands = new CpBands(new CpUTF8Segment());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Codec.DELTA5.encode(new int[] { 1000 }));    // cpUTF8Prefix (count cpUTF8Count - 2)
        baos.write(Codec.UNSIGNED5.encode(new int[] { 1, 1 })); // cpUTF8Suffix (count cpUTF8Count - 1)
        baos.write(Codec.CHAR3.encode(new int[] { 'a', 'b' }));  // cp_Utf8_chars
        final InputStream in = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(Pack200Exception.class, () -> bands.read(in));
    }
}
