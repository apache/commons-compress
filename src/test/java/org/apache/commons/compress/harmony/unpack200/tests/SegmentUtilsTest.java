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

import java.util.stream.Stream;

import org.apache.commons.compress.harmony.unpack200.IMatcher;
import org.apache.commons.compress.harmony.unpack200.SegmentUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SegmentUtilsTest {

    private static class MultipleMatches implements IMatcher {

        private final int divisor;

        public MultipleMatches(final int divisor) {
            this.divisor = divisor;
        }

        @Override
        public boolean matches(final long value) {
            return value % divisor == 0;
        }

    }

    public static final IMatcher even = new MultipleMatches(2);
    public static final IMatcher five = new MultipleMatches(5);

    static Stream<Arguments> countArgs() {
        return Stream.of(
                Arguments.of("()V", 0),
                Arguments.of("(D)V", 1),
                Arguments.of("([D)V", 1),
                Arguments.of("([[D)V", 1),
                Arguments.of("(DD)V", 2),
                Arguments.of("(DDD)V", 3),
                Arguments.of("(Lblah/blah;D)V", 2),
                Arguments.of("(Lblah/blah;DLbLah;)V", 3)
        );
    }

    static Stream<Arguments> countInvokeInterfaceArgs() {
        return Stream.of(
                Arguments.of("(Z)V", 1),
                Arguments.of("(D)V", 2),
                Arguments.of("(J)V", 2),
                Arguments.of("([D)V", 1),
                Arguments.of("([[D)V", 1),
                Arguments.of("(DD)V", 4),
                Arguments.of("(Lblah/blah;D)V", 3),
                Arguments.of("(Lblah/blah;DLbLah;)V", 4),
                Arguments.of("([Lblah/blah;DLbLah;)V", 4)
        );
    }

    @ParameterizedTest
    @MethodSource("countArgs")
    public void testCountArgs(final String descriptor, final int expectedArgsCount) {
        assertEquals(expectedArgsCount, SegmentUtils.countArgs(descriptor));
    }

    @ParameterizedTest
    @MethodSource("countInvokeInterfaceArgs")
    public void testCountInvokeInterfaceArgs(final String descriptor, final int expectedCountInvokeInterfaceArgs) {
        assertEquals(expectedCountInvokeInterfaceArgs, SegmentUtils.countInvokeInterfaceArgs(descriptor));
    }

    @Test
    public void testMatches() {
        final long[] oneToTen = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        assertEquals(6, SegmentUtils.countMatches(new long[][] { oneToTen,
                new long[] { 5, 6, 7 } }, even));
        assertEquals(5, SegmentUtils.countMatches(new long[][] { oneToTen },
                even));
        assertEquals(5, SegmentUtils.countMatches(oneToTen, even));
        assertEquals(3, SegmentUtils.countMatches(new long[][] { oneToTen,
                new long[] { 5, 6, 7 } }, five));
        assertEquals(2, SegmentUtils.countMatches(new long[][] { oneToTen },
                five));
        assertEquals(2, SegmentUtils.countMatches(oneToTen, five));
    }
}
