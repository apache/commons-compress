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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.AttributeLayout;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AttributeLayoutTest {

    public class TestSegment extends Segment {

        private ClassFileEntry entry(final String string) {
            return new CPUTF8(string);
        }

        @Override
        public SegmentConstantPool getConstantPool() {
            final ClassFileEntry[][] data = {
                    {}, // ALL
                    { entry("Zero"), entry("One"), entry("Two"),
                            entry("Three"), entry("Four"), entry("Five"),
                            entry("Six"), entry("Seven"), entry("Eight"),
                            entry("Nine") }, // UTF-8
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { entry("Eins"), entry("Zwei"), entry("Drei"),
                            entry("Vier"), entry("Funf"), entry("Sechs"),
                            entry("Sieben"), entry("Acht"), entry("Neun") }, // Signature
            };
            return new SegmentConstantPool(null) {

                @Override
                public ClassFileEntry getValue(final int cp, final long index) {
                    if (index == -1) {
                        return null;
                    }
                    return data[cp][(int) index];
                }

            };
        }
    }

    static Stream<Arguments> badData() {
        return Stream.of(
                Arguments.of(null, AttributeLayout.CONTEXT_CLASS, ""),
                Arguments.of("", AttributeLayout.CONTEXT_CLASS, ""),
                Arguments.of("name", -1, ""),
                Arguments.of("name", 1234, "")
        );
    }

    static Stream<Arguments> codec() {
        return Stream.of(
                Arguments.of("O", AttributeLayout.CONTEXT_CLASS, "HOBS", Codec.BRANCH5),
                Arguments.of("P", AttributeLayout.CONTEXT_METHOD, "PIN", Codec.BCI5),
                Arguments.of("S", AttributeLayout.CONTEXT_FIELD, "HS", Codec.SIGNED5),
                Arguments.of("RS", AttributeLayout.CONTEXT_CODE, "RRRS", Codec.UNSIGNED5),
                Arguments.of("KS", AttributeLayout.CONTEXT_CLASS, "RKS", Codec.UNSIGNED5),
                Arguments.of("B", AttributeLayout.CONTEXT_CLASS, "TRKSB", Codec.BYTE1)
        );
    }

    static Stream<Arguments> okData() {
        return Stream.of(
                Arguments.of("name", AttributeLayout.CONTEXT_CLASS, ""),
                Arguments.of("name", AttributeLayout.CONTEXT_METHOD, ""),
                Arguments.of("name", AttributeLayout.CONTEXT_FIELD, ""),
                Arguments.of("name", AttributeLayout.CONTEXT_CODE, "")
        );
    }

    @ParameterizedTest
    @MethodSource("badData")
    public void testBadData(final String name, final int context, final String layout) {
        assertThrows(Pack200Exception.class, () -> new AttributeLayout(name, context, layout, -1));
    }

    @ParameterizedTest
    @MethodSource("codec")
    public void testGetCodec(final String name, final int context, final String layout, final Codec expectedCodec) throws Pack200Exception {
        final AttributeLayout attributeLayout = new AttributeLayout(name, context, layout, 1);
        assertEquals(expectedCodec, attributeLayout.getCodec());
    }

    @Test
    public void testLayoutRS() throws Pack200Exception {
        final AttributeLayout layout = new AttributeLayout("RS",
                AttributeLayout.CONTEXT_CLASS, "RS", 1);
        final Segment segment = new TestSegment();
        assertNull(layout.getValue(-1, segment.getConstantPool()));
        assertEquals("Eins", ((CPUTF8)layout.getValue(0, segment.getConstantPool())).underlyingString());
        assertEquals("Zwei", ((CPUTF8)layout.getValue(1, segment.getConstantPool())).underlyingString());
    }

    @Test
    public void testLayoutRSN() throws Pack200Exception {
        final AttributeLayout layout = new AttributeLayout("RSN",
                AttributeLayout.CONTEXT_CLASS, "RSN", 1);
        final Segment segment = new TestSegment();
        assertNull(layout.getValue(0, segment.getConstantPool()));
        assertEquals("Eins", ((CPUTF8)layout.getValue(1, segment.getConstantPool())).underlyingString());
        assertEquals("Zwei", ((CPUTF8)layout.getValue(2, segment.getConstantPool())).underlyingString());
    }

    @Test
    public void testLayoutRU() throws Pack200Exception {
        final AttributeLayout layout = new AttributeLayout("RU",
                AttributeLayout.CONTEXT_CLASS, "RU", 1);
        final Segment segment = new TestSegment();
        assertNull(layout.getValue(-1, segment.getConstantPool()));
        assertEquals("Zero", ((CPUTF8)layout.getValue(0, segment.getConstantPool())).underlyingString());
        assertEquals("One", ((CPUTF8)layout.getValue(1, segment.getConstantPool())).underlyingString());
    }

    @Test
    public void testLayoutRUN() throws Pack200Exception {
        final AttributeLayout layout = new AttributeLayout("RUN",
                AttributeLayout.CONTEXT_CLASS, "RUN", 1);
        final Segment segment = new TestSegment();
        assertNull(layout.getValue(0, segment.getConstantPool()));
        assertEquals("Zero", ((CPUTF8)layout.getValue(1, segment.getConstantPool())).underlyingString());
        assertEquals("One", ((CPUTF8)layout.getValue(2, segment.getConstantPool())).underlyingString());
    }

    @ParameterizedTest
    @MethodSource("okData")
    public void testOkData(final String name, final int context, final String layout) {
        assertDoesNotThrow(() -> new AttributeLayout(name, context, layout, -1));
    }
}
