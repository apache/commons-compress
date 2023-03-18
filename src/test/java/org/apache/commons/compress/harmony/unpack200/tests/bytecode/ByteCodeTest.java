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
package org.apache.commons.compress.harmony.unpack200.tests.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ByteCodeTest {

    static Stream<Arguments> byteCode() {
        return Stream.of(
                Arguments.of(0, "nop"),
                Arguments.of(-79, "return"),
                Arguments.of(177, "return")
        );
    }

    @ParameterizedTest
    @MethodSource("byteCode")
    public void testByteCode(final int opCode, final String expectedName) {
        assertEquals(expectedName, ByteCode.getByteCode(opCode).getName());
    }
}
