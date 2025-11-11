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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AttributeLayoutParserTest {

    private static class AttributeLayoutElement {
        private AttributeLayoutElement() {
        }

        private AttributeLayoutElement(String body) {
        }
    }

    private static class AttributeLayoutFactory implements AttributeLayoutParser.Factory<AttributeLayoutElement, LayoutElement> {

        @Override
        public LayoutElement createCall(int callableIndex) {
            return new LayoutElement(callableIndex);
        }

        @Override
        public AttributeLayoutElement createCallable(String body) throws Pack200Exception {
            return new AttributeLayoutElement(body);
        }

        @Override
        public LayoutElement createIntegral(String tag) {
            return new LayoutElement(tag);
        }

        @Override
        public LayoutElement createReference(String tag) {
            return new LayoutElement(tag);
        }

        @Override
        public LayoutElement createReplication(String unsignedInt, String body) throws Pack200Exception {
            return new LayoutElement(unsignedInt, body);
        }

        @Override
        public LayoutElement createUnion(String anyInt, List<AttributeLayoutParser.UnionCaseData> cases, String body) throws Pack200Exception {
            return new LayoutElement(anyInt, cases, body);
        }
    }

    private static class LayoutElement extends AttributeLayoutElement {
        private LayoutElement(int callableIndex) {
        }

        private LayoutElement(String body) {
        }

        private LayoutElement(String anyInt, List<AttributeLayoutParser.UnionCaseData> cases, String body) {
        }

        private LayoutElement(String unsignedInt, String body) {
        }
    }

    private static final AttributeLayoutFactory FACTORY = new AttributeLayoutFactory();

    static Stream<String> validCallableLayouts() {
        return Stream.of(
                // Valid callable layouts
                "[SIK]", "[NI[SIK]O]");
    }

    static Stream<String> validLayouts() {
        return Stream.of(
                // Valid integral layouts
                "B", "H", "I", "V", "SI", "PI", "OI", "FI", "OSI", "POI",
                // Valid replication layouts
                "NI[I]", "NI[SI]", "NI[NI[I]]",
                // Valid reference layouts
                "KII", "KINI", "RSI", "RUH", "RQB",
                // Valid union layouts
                "TI()[]", "TSI()[]", "TI(1)[]()[]", "TI(1-3)[](4)[]()[]", "TI(1-3,5)[](4,6)[]()[]",
                // Valid call layouts
                "(1)", "(-2)");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Invalid letters
            ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?", "@", "A", "C", "D", "E", "F",
            "G", "J", "L", "M", "N", "Q", "U",
            // Invalid integral layouts
            "S", "SA", "P", "PA", "PO", "POA", "O", "OA", "F", "FA",
            // Invalid replication layouts
            "N", "NSI", "NIA", "NI[I", "NI[]",
            // Invalid reference layouts
            "K", "KA", "KI", "KIA", "KIN", "KINA",
            // Invalid callable layouts
            "[", "[]",
            // Invalid union layouts
            "T", "TA", "TI", "TSA", "TI(A)[]()[]",
            // Invalid call layouts
            "()", "(A)", "(-A)", "(9999999999999999999999)", "(1234"})
    void testInvalidLayout(String layout) {
        final AttributeLayoutParser<AttributeLayoutElement, LayoutElement> parser = new AttributeLayoutParser<>(layout, FACTORY);
        final Pack200Exception ex = assertThrows(Pack200Exception.class, () -> parser.readAttributeLayoutElement());
        assertTrue(ex.getMessage().contains("Corrupted Pack200 archive"), "Unexpected exception message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(layout), "Unexpected exception message: " + ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource({"validLayouts", "validCallableLayouts"})
    void testReadAttributeLayoutElement(String layout) throws Pack200Exception {
        final List<AttributeLayoutElement> result = AttributeLayoutUtils.readAttributeLayout(layout, FACTORY);
        assertFalse(result.isEmpty(), "Expected at least one LayoutElement for layout: " + layout);
    }

    @ParameterizedTest
    @MethodSource("validLayouts")
    void testReadBody(String layout) throws Pack200Exception {
        final List<LayoutElement> result = AttributeLayoutUtils.readBody(layout, FACTORY);
        assertFalse(result.isEmpty(), "Expected at least one LayoutElement for layout: " + layout);
    }
}
