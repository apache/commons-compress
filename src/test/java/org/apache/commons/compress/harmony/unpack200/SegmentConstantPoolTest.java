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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.api.Test;

/**
 * Tests for org.apache.commons.compress.harmony.unpack200.SegmentConstantPool.
 */
class SegmentConstantPoolTest extends AbstractBandsTest {

    private final class CpUTF8Header extends MockSegmentHeader {

        CpUTF8Header(final Segment segment) {
            super(segment);
        }

        @Override
        public int getCpUTF8Count() {
            return 2;
        }
    }

    private final class CpUTF8Segment extends MockSegment {

        private final SegmentHeader header = new CpUTF8Header(this);

        @Override
        public SegmentHeader getSegmentHeader() {
            return header;
        }
    }

    /**
     * Populates a CpBands with a two-entry UTF-8 pool ("", "a") and nothing else.
     */
    private CpBands utf8Bands() throws Exception {
        final CpBands bands = new CpBands(new CpUTF8Segment());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // cpUTF8Prefix has count cpUTF8Count - 2 == 0, so no bytes.
        baos.write(Codec.UNSIGNED5.encode(new int[] { 1 })); // cpUTF8Suffix (count cpUTF8Count - 1)
        baos.write(Codec.CHAR3.encode(new int[] { 'a' }));   // cp_Utf8_chars
        bands.read(new ByteArrayInputStream(baos.toByteArray()));
        return bands;
    }

    public class MockSegmentConstantPool extends SegmentConstantPool {

        MockSegmentConstantPool() {
            super(new CpBands(new Segment()));
        }

        @Override
        public int matchSpecificPoolEntryIndex(final String[] classNameArray, final String desiredClassName, final int desiredIndex) {
            return super.matchSpecificPoolEntryIndex(classNameArray, desiredClassName, desiredIndex);
        }

        @Override
        public int matchSpecificPoolEntryIndex(final String[] classNameArray, final String[] methodNameArray, final String desiredClassName,
                final String desiredMethodRegex, final int desiredIndex) {
            return super.matchSpecificPoolEntryIndex(classNameArray, methodNameArray, desiredClassName, desiredMethodRegex, desiredIndex);
        }

        public boolean regexMatchesVisible(final String regexString, final String compareString) {
            return SegmentConstantPool.regexMatches(regexString, compareString);
        }
    }

    String[] testClassArray = { "Object", "Object", "java/lang/String", "java/lang/String", "Object", "Other" };
    String[] testMethodArray = { "<init>()", "clone()", "equals()", "<init>", "isNull()", "Other" };

    @Test
    void testMatchSpecificPoolEntryIndex_DoubleArray() {
        final MockSegmentConstantPool mockInstance = new MockSegmentConstantPool();
        // Elements should be found at the proper position.
        assertEquals(0, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "Object", "^<init>.*", 0));
        assertEquals(2, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "java/lang/String", ".*", 0));
        assertEquals(3, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "java/lang/String", "^<init>.*", 0));
        assertEquals(5, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "Other", ".*", 0));

        // Elements that don't exist shouldn't be found
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "NotThere", "^<init>.*", 0));

        // Elements that exist but don't have the requisite number
        // of hits shouldn't be found.
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(testClassArray, testMethodArray, "java/lang/String", "^<init>.*", 1));
    }

    @Test
    void testMatchSpecificPoolEntryIndex_SingleArray() {
        final MockSegmentConstantPool mockInstance = new MockSegmentConstantPool();
        // Elements should be found at the proper position.
        assertEquals(0, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "Object", 0));
        assertEquals(1, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "Object", 1));
        assertEquals(2, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "java/lang/String", 0));
        assertEquals(3, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "java/lang/String", 1));
        assertEquals(4, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "Object", 2));
        assertEquals(5, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "Other", 0));

        // Elements that don't exist shouldn't be found
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "NotThere", 0));

        // Elements that exist but don't have the requisite number
        // of hits shouldn't be found.
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(testClassArray, "java/lang/String", 2));
    }

    @Test
    void testGetConstantPoolEntryRejectsOutOfRangeIndex() throws Exception {
        // A bytecode reference operand is an unvalidated constant-pool index; getConstantPoolEntry and getValue
        // only rejected negative indices (toIndex) before handing the value to CpBands.cpUTF8Value etc., which
        // read cpUTF8[index] with no upper bound. An index past the pool size raised a raw
        // ArrayIndexOutOfBoundsException out of the declared Pack200Exception contract.
        final SegmentConstantPool pool = new SegmentConstantPool(utf8Bands());
        // A valid index still resolves.
        assertNotNull(pool.getConstantPoolEntry(SegmentConstantPool.UTF_8, 1));
        assertNotNull(pool.getValue(SegmentConstantPool.UTF_8, 1));
        // An out-of-range index is rejected as corrupt input.
        assertThrows(Pack200Exception.class, () -> pool.getConstantPoolEntry(SegmentConstantPool.UTF_8, 5));
        assertThrows(Pack200Exception.class, () -> pool.getValue(SegmentConstantPool.UTF_8, 5));
    }

    @Test
    void testRegexReplacement() {
        final MockSegmentConstantPool mockPool = new MockSegmentConstantPool();
        assertTrue(mockPool.regexMatchesVisible(".*", "anything"));
        assertTrue(mockPool.regexMatchesVisible(".*", ""));
        assertTrue(mockPool.regexMatchesVisible("^<init>.*", "<init>"));
        assertTrue(mockPool.regexMatchesVisible("^<init>.*", "<init>stuff"));
        assertFalse(mockPool.regexMatchesVisible("^<init>.*", "init>stuff"));
        assertFalse(mockPool.regexMatchesVisible("^<init>.*", "<init"));
        assertFalse(mockPool.regexMatchesVisible("^<init>.*", ""));
    }
}
