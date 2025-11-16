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
package org.apache.commons.compress.harmony.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.IntegerRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttributeLayoutUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            // Invalid reference_type
            "AB", "ABI", "CD", "EF",
            // Missing uint_type
            "KI", "RC", "RU", "KIN", "RCN", "RUN",
            // Extra characters
            "KIBB", "RCBB", "RUNNH"})
    void testCheckReferenceTag_Invalid(String tag) {
        assertThrows(IllegalArgumentException.class, () -> {
            AttributeLayoutUtils.checkReferenceTag(tag);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"KIB", "KJH", "KFI", "KDV", "KSB", "KQV", "KMI", "KTI", "KLV", "RCB", "RCH", "RDI", "RFV", "RMI", "RII", "RYB", "RBNI"})
    void testCheckReferenceTag_Valid(String tag) {
        assertEquals(tag, AttributeLayoutUtils.checkReferenceTag(tag));
    }

    @Test
    void testToRanges() {
        final List<Integer> integers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final List<IntegerRange> ranges = AttributeLayoutUtils.toRanges(integers);
        assertEquals(ranges.size(), integers.size());
        for (int i = 0; i < ranges.size(); i++) {
            assertEquals(IntegerRange.of(integers.get(i), integers.get(i)), ranges.get(i), "Range at index " + i + " should be a single value range.");
        }
    }

    @Test
    void testUnionCaseMatches() {
        final List<IntegerRange> ranges = Arrays.asList(IntegerRange.of(1, 2), IntegerRange.of(4, 5), IntegerRange.of(7, 8));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 1));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 2));
        assertFalse(AttributeLayoutUtils.unionCaseMatches(ranges, 3));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 4));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 5));
        assertFalse(AttributeLayoutUtils.unionCaseMatches(ranges, 6));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 7));
        assertTrue(AttributeLayoutUtils.unionCaseMatches(ranges, 8));
        assertFalse(AttributeLayoutUtils.unionCaseMatches(ranges, 9));
    }
}
