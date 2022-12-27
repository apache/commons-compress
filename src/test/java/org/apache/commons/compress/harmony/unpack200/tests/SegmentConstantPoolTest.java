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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.harmony.unpack200.CpBands;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.junit.jupiter.api.Test;

/**
 * Tests for org.apache.commons.compress.harmony.unpack200.SegmentConstantPool.
 */
public class SegmentConstantPoolTest {

    public class MockSegmentConstantPool extends SegmentConstantPool {

        public MockSegmentConstantPool() {
            super(new CpBands(new Segment()));
        }

        @Override
        public int matchSpecificPoolEntryIndex(final String[] classNameArray,
                final String desiredClassName, final int desiredIndex) {
            return super.matchSpecificPoolEntryIndex(classNameArray,
                    desiredClassName, desiredIndex);
        }

        @Override
        public int matchSpecificPoolEntryIndex(final String[] classNameArray,
                final String[] methodNameArray, final String desiredClassName,
                final String desiredMethodRegex, final int desiredIndex) {
            return super.matchSpecificPoolEntryIndex(classNameArray,
                    methodNameArray, desiredClassName, desiredMethodRegex,
                    desiredIndex);
        }

        public boolean regexMatchesVisible(final String regexString,
                final String compareString) {
            return SegmentConstantPool.regexMatches(regexString, compareString);
        }
    }

    String[] testClassArray = { "Object", "Object", "java/lang/String",
            "java/lang/String", "Object", "Other" };
    String[] testMethodArray = { "<init>()", "clone()", "equals()", "<init>",
            "isNull()", "Other" };

    @Test
    public void testMatchSpecificPoolEntryIndex_DoubleArray() {
        final MockSegmentConstantPool mockInstance = new MockSegmentConstantPool();
        // Elements should be found at the proper position.
        assertEquals(0, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "Object", "^<init>.*", 0));
        assertEquals(2, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "java/lang/String", ".*", 0));
        assertEquals(3, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "java/lang/String",
                "^<init>.*", 0));
        assertEquals(5, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "Other", ".*", 0));

        // Elements that don't exist shouldn't be found
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "NotThere", "^<init>.*", 0));

        // Elements that exist but don't have the requisite number
        // of hits shouldn't be found.
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, testMethodArray, "java/lang/String",
                "^<init>.*", 1));
    }

    @Test
    public void testMatchSpecificPoolEntryIndex_SingleArray() {
        final MockSegmentConstantPool mockInstance = new MockSegmentConstantPool();
        // Elements should be found at the proper position.
        assertEquals(0, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "Object", 0));
        assertEquals(1, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "Object", 1));
        assertEquals(2, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "java/lang/String", 0));
        assertEquals(3, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "java/lang/String", 1));
        assertEquals(4, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "Object", 2));
        assertEquals(5, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "Other", 0));

        // Elements that don't exist shouldn't be found
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "NotThere", 0));

        // Elements that exist but don't have the requisite number
        // of hits shouldn't be found.
        assertEquals(-1, mockInstance.matchSpecificPoolEntryIndex(
                testClassArray, "java/lang/String", 2));
    }

    @Test
    public void testRegexReplacement() {
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
