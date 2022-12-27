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
package org.apache.commons.compress.harmony.pack200.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands;
import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands.AttributeDefinition;
import org.apache.commons.compress.harmony.pack200.CPUTF8;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.CpBands;
import org.apache.commons.compress.harmony.pack200.NewAttribute;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.AttributeLayoutElement;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Call;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Callable;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Integral;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.LayoutElement;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Reference;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Replication;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Union;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands.UnionCase;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.SegmentHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for pack200 support for non-predefined attributes
 */
public class NewAttributeBandsTest {

    private class MockNewAttributeBands extends NewAttributeBands {

        public MockNewAttributeBands(final int effort, final CpBands cpBands,
                final SegmentHeader header, final AttributeDefinition def)
                throws IOException {
            super(effort, cpBands, header, def);
        }

        public List<AttributeLayoutElement> getLayoutElements() {
            return attributeLayoutElements;
        }
    }

    @Test
    public void testAddAttributes() throws IOException, Pack200Exception {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8("B");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        newAttributeBands.addAttribute(new NewAttribute(null, "TestAttribute",
                "B", new byte[] { 27 }, null, 0, null));
        newAttributeBands.addAttribute(new NewAttribute(null, "TestAttribute",
                "B", new byte[] { 56 }, null, 0, null));
        newAttributeBands.addAttribute(new NewAttribute(null, "TestAttribute",
                "B", new byte[] { 3 }, null, 0, null));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        newAttributeBands.pack(out);
        // BYTE1 is used for B layouts so we don't need to unpack to test the
        // results
        final byte[] bytes = out.toByteArray();
        assertEquals(3, bytes.length);
        assertEquals(27, bytes[0]);
        assertEquals(56, bytes[1]);
        assertEquals(3, bytes[2]);
    }

    @Test
    public void testAddAttributesWithReplicationLayout() throws IOException,
            Pack200Exception {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8("NB[SH]");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        newAttributeBands.addAttribute(new NewAttribute(null, "TestAttribute",
                "B", new byte[] { 1, 0, 100 }, null, 0, null));
        final short s = -50;
        final byte b1 = (byte) (s >>> 8);
        final byte b2 = (byte) s;
        newAttributeBands.addAttribute(new NewAttribute(null, "TestAttribute",
                "B", new byte[] { 3, 0, 5, 0, 25, b1, b2 }, null, 0, null));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        newAttributeBands.pack(out);
        final byte[] bytes = out.toByteArray();
        assertEquals(1, bytes[0]);
        assertEquals(3, bytes[1]);
        final byte[] band = new byte[bytes.length - 2];
        System.arraycopy(bytes, 2, band, 0, band.length);
        final int[] decoded = Codec.SIGNED5.decodeInts(4, new ByteArrayInputStream(
                band));
        assertEquals(4, decoded.length);
        assertEquals(100, decoded[0]);
        assertEquals(5, decoded[1]);
        assertEquals(25, decoded[2]);
        assertEquals(-50, decoded[3]);
    }

    @Test
    public void testEmptyLayout() throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8("");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(0, layoutElements.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "B", "FB", "SB", "H", "FH", "SH", "I", "FI", "SI", "PB", "OB", "OSB",
            "POB", "PH", "OH", "OSH", "POH", "PI", "OI", "OSI", "POI"
    })
    public void testIntegralLayouts(final String layoutStr) throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8(layoutStr);
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Integral element = (Integral) layoutElements.get(0);
        assertEquals(layoutStr, element.getTag());
    }

    @Test
    public void testLayoutWithBackwardsCalls() throws Exception {
        CPUTF8 name = new CPUTF8("TestAttribute");
        CPUTF8 layout = new CPUTF8("[NH[(1)]][KIH][(-1)]");
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        Callable firstCallable = (Callable) layoutElements.get(0);
        Callable secondCallable = (Callable) layoutElements.get(1);
        Callable thirdCallable = (Callable) layoutElements.get(2);
        List<LayoutElement> thirdBody = thirdCallable.getBody();
        assertEquals(1, thirdBody.size());
        Call call = (Call) thirdBody.get(0);
        assertEquals(secondCallable, call.getCallable());
        assertTrue(secondCallable.isBackwardsCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());

        name = new CPUTF8("TestAttribute");
        layout = new CPUTF8("[NH[(1)]][KIH][(-2)]");
        newAttributeBands = new MockNewAttributeBands(1, null, null,
                new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        firstCallable = (Callable) layoutElements.get(0);
        secondCallable = (Callable) layoutElements.get(1);
        thirdCallable = (Callable) layoutElements.get(2);
        thirdBody = thirdCallable.getBody();
        assertEquals(1, thirdBody.size());
        call = (Call) thirdBody.get(0);
        assertEquals(firstCallable, call.getCallable());
        assertTrue(firstCallable.isBackwardsCallable());
        assertFalse(secondCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());

        name = new CPUTF8("TestAttribute");
        layout = new CPUTF8("[NH[(1)]][KIH][(0)]");
        newAttributeBands = new MockNewAttributeBands(1, null, null,
                new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        firstCallable = (Callable) layoutElements.get(0);
        secondCallable = (Callable) layoutElements.get(1);
        thirdCallable = (Callable) layoutElements.get(2);
        thirdBody = thirdCallable.getBody();
        assertEquals(1, thirdBody.size());
        call = (Call) thirdBody.get(0);
        assertEquals(thirdCallable, call.getCallable());
        assertTrue(thirdCallable.isBackwardsCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(secondCallable.isBackwardsCallable());
    }

    @Test
    public void testLayoutWithCalls() throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8(
                "[NH[(1)]][RSH NH[RUH(1)]][TB(66,67,73,83,90)[KIH](68)[KDH](70)[KFH](74)[KJH](99)[RSH](101)[RSH RUH](115)[RUH](91)[NH[(0)]](64)[RSH[RUH(0)]]()[]]");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        final Callable firstCallable = (Callable) layoutElements.get(0);
        final Callable secondCallable = (Callable) layoutElements.get(1);
        final Callable thirdCallable = (Callable) layoutElements.get(2);
        final List<LayoutElement> firstBody = firstCallable.getBody();
        assertEquals(1, firstBody.size());
        final Replication rep = (Replication) firstBody.get(0);
        final List<LayoutElement> repBody = rep.getLayoutElements();
        assertEquals(1, repBody.size());
        final Call call = (Call) repBody.get(0);
        assertEquals(1, call.getCallableIndex());
        assertEquals(secondCallable, call.getCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(secondCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "KIB", "KIH", "KII", "KINH", "KJH", "KDH", "KSH", "KQH", "RCH",
            "RSH", "RDH", "RFH", "RMH", "RIH", "RUH", "RQH", "RQNH", "RQNI"
    })
    public void testReferenceLayouts(final String layoutStr) throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8(layoutStr);
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Reference element = (Reference) layoutElements.get(0);
        assertEquals(layoutStr, element.getTag());
    }

    @Test
    public void testReplicationLayouts() throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8("NH[PHOHRUHRSHH]");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Replication element = (Replication) layoutElements.get(0);
        final Integral countElement = element.getCountElement();
        assertEquals("H", countElement.getTag());
        final List<LayoutElement> replicatedElements = element.getLayoutElements();
        assertEquals(5, replicatedElements.size());
        final Integral firstElement = (Integral) replicatedElements.get(0);
        assertEquals("PH", firstElement.getTag());
        final Integral secondElement = (Integral) replicatedElements.get(1);
        assertEquals("OH", secondElement.getTag());
        final Reference thirdElement = (Reference) replicatedElements.get(2);
        assertEquals("RUH", thirdElement.getTag());
        final Reference fourthElement = (Reference) replicatedElements.get(3);
        assertEquals("RSH", fourthElement.getTag());
        final Integral fifthElement = (Integral) replicatedElements.get(4);
        assertEquals("H", fifthElement.getTag());
    }

    @Test
    public void testUnionLayout() throws IOException {
        final CPUTF8 name = new CPUTF8("TestAttribute");
        final CPUTF8 layout = new CPUTF8("TB(55)[FH](23)[]()[RSH]");
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(1,
                null, null, new AttributeDefinition(35,
                        AttributeDefinitionBands.CONTEXT_CLASS, name, layout));
        final List<AttributeLayoutElement> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Union element = (Union) layoutElements.get(0);
        final Integral tag = element.getUnionTag();
        assertEquals("B", tag.getTag());
        final List<UnionCase> unionCases = element.getUnionCases();
        assertEquals(2, unionCases.size());
        final UnionCase firstCase = unionCases.get(0);
        assertTrue(firstCase.hasTag(55));
        assertFalse(firstCase.hasTag(23));
        List<LayoutElement> body = firstCase.getBody();
        assertEquals(1, body.size());
        final Integral bodyElement = (Integral) body.get(0);
        assertEquals("FH", bodyElement.getTag());
        final UnionCase secondCase = unionCases.get(1);
        assertTrue(secondCase.hasTag(23));
        assertFalse(secondCase.hasTag(55));
        body = secondCase.getBody();
        assertEquals(0, body.size());
        final List<LayoutElement> defaultBody = element.getDefaultCaseBody();
        assertEquals(1, defaultBody.size());
        final Reference ref = (Reference) defaultBody.get(0);
        assertEquals("RSH", ref.getTag());
    }

}
