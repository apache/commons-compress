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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.commons.compress.harmony.unpack200.IcTuple;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ICTupleTest {

    static Stream<Arguments> explicit() {
        return Stream.of(
                Arguments.of("Foo$$2$Local", null, "$2$Local", "$2$Local", "Foo$$2"),
                Arguments.of("Red$Herring", "Red$Herring", null, "Herring", "Red$Herring"),
                Arguments.of("X$1$Q", "X$1", "Q", "Q", "X$1")
        );
    }

    static Stream<Arguments> predicted() {
        return Stream.of(
                Arguments.of("orw/SimpleHelloWorld$SimpleHelloWorldInner", "SimpleHelloWorldInner", "orw/SimpleHelloWorld"),
                Arguments.of("java/util/AbstractList$2$Local", "Local", "java/util/AbstractList$2"),
                Arguments.of("java/util/AbstractList#2#Local", "Local", "java/util/AbstractList$2"),
                Arguments.of("java/util/AbstractList$1", "1", "java/util/AbstractList")
        );
    }

    @ParameterizedTest
    @MethodSource("explicit")
    public void testExplicitClassTupleParsing(final String c, final String c2, final String n, final String expectedSimpleClassName, final String expectedOuterClass) {
        final IcTuple tuple = new IcTuple(c, IcTuple.NESTED_CLASS_FLAG, c2, n, -1, -1, -1, -1);
        assertAll(
                () -> assertEquals(expectedSimpleClassName, tuple.simpleClassName()),
                () -> assertEquals(expectedOuterClass, tuple.outerClassString())
        );
    }

    @ParameterizedTest
    @MethodSource("predicted")
    public void testPredictedClassTupleParsing(final String c, final String expectedSimpleClass, final String expectedOuterClass) {
        final IcTuple tuple = new IcTuple(c, 0, null, null, -1, -1, -1, -1);
        assertAll(
                () -> assertEquals(expectedSimpleClass, tuple.simpleClassName()),
                () -> assertEquals(expectedOuterClass, tuple.outerClassString())
        );
    }
}
