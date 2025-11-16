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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Call;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Callable;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Integral;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Reference;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Replication;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Union;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.UnionCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for unpack200 support for non-predefined attributes
 */
class NewAttributeBandsTest extends AbstractBandsTest {

    private final class MockNewAttributeBands extends NewAttributeBands {

        MockNewAttributeBands(final Segment segment, final AttributeLayout layout) throws IOException {
            super(segment, layout);
        }

        public List<?> getLayoutElements() {
            return attributeLayoutElements;
        }
    }

    private static String createRecursiveLayout(int level, String prefix) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(prefix);
        }
        sb.append("H");
        for (int i = 0; i < level; i++) {
            sb.append("]");
        }
        return sb.toString();
    }

    private MockNewAttributeBands createNewAttributeBands(final String layoutStr) throws IOException, Pack200Exception {
        return new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CLASS, layoutStr, 25));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NH[]", "[]"})
    void testEmptyBodyFails(final String layout) {
        final Pack200Exception ex = assertThrows(Pack200Exception.class, () -> createNewAttributeBands(layout));
        assertTrue(ex.getMessage().contains(layout), "Unexpected exception message: " + ex.getMessage());
        final Throwable cause = ex.getCause();
        assertTrue(cause.getMessage().contains("empty"), "Unexpected exception message: " + cause.getMessage());
    }

    @Test
    void testEmptyLayout() throws IOException, Pack200Exception {
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CLASS, "", 25));
        final List<?> layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(0, layoutElements.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // unsigned_int
            "B", "H", "I", "V",
            // signed_int
            "SB", "SH", "SI", "SV",
            // bc_index
            "PB", "PH", "PI", "PV", "POB", "POH", "POI", "POV",
            // bc_offset
            "OB", "OH", "OI", "OV",
            // flag
            "FB", "FH", "FI", "FV"})
    void testIntegralLayout(final String layoutStr) throws IOException, Pack200Exception {
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CLASS, layoutStr, 25));
        final List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Integral element = (Integral) layoutElements.get(0);
        assertEquals(layoutStr, element.getTag());
    }

    @Test
    void testLayoutWithBackwardsCall() throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_METHOD, "[NH[(1)]][KIH][(-1)]", 20));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        Callable firstCallable = (Callable) layoutElements.get(0);
        Callable secondCallable = (Callable) layoutElements.get(1);
        Callable thirdCallable = (Callable) layoutElements.get(2);
        List thirdBody = thirdCallable.getBody();
        assertEquals(1, thirdBody.size());
        Call call = (Call) thirdBody.get(0);
        assertEquals(secondCallable, call.getCallable());
        assertTrue(secondCallable.isBackwardsCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());

        newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_METHOD, "[NH[(1)]][KIH][(-2)]", 20));
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

        newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_METHOD, "[NH[(1)]][KIH][(0)]", 20));
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
        assertFalse(firstCallable.isBackwardsCallable());
    }

    @Test
    void testLayoutWithCalls() throws IOException, Pack200Exception {
        // @formatter:off
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(), new AttributeLayout("test", AttributeLayout.CONTEXT_FIELD,
            "[NH[(1)]][RSHNH[RUH(1)]][TB(66,67,73,83,90)[KIH](68)[KDH](70)[KFH](74)[KJH](99)[RSH](101)[RSHRUH](115)[RUH](91)[NH[(0)]](64)[RSHNH[RUH(0)]]()[]]",
            26));
        // @formatter:on
        final List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        final Callable firstCallable = (Callable) layoutElements.get(0);
        final Callable secondCallable = (Callable) layoutElements.get(1);
        final Callable thirdCallable = (Callable) layoutElements.get(2);
        final List firstBody = firstCallable.getBody();
        assertEquals(1, firstBody.size());
        final Replication rep = (Replication) firstBody.get(0);
        final List repBody = rep.getLayoutElements();
        assertEquals(1, repBody.size());
        final Call call = (Call) repBody.get(0);
        assertEquals(1, call.getCallableIndex());
        assertEquals(secondCallable, call.getCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(secondCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NH[", "TH()["})
    void testRecursiveReplicationLayout(String prefix) throws IOException {
        final String layout = createRecursiveLayout(8192, prefix);
        createNewAttributeBands(layout);
    }

    @ParameterizedTest
    @ValueSource(strings = { "KIB", "KIH", "KII", "KINH", "KJH", "KDH", "KSH", "KQH", "RCH", "RSH", "RDH", "RFH", "RMH", "RIH", "RUH", "RQH", "RQNH", "RQNI" })
    void testReferenceLayouts(final String layout) throws IOException, Pack200Exception {
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CODE, layout, 26));
        final List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Reference element = (Reference) layoutElements.get(0);
        assertEquals(layout, element.getTag());
    }

    @Test
    void testReplicationLayout() throws IOException, Pack200Exception {
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CLASS, "NH[PHOHRUHRSHH]", 25));
        final List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Replication element = (Replication) layoutElements.get(0);
        final Integral countElement = element.getCountElement();
        assertEquals("H", countElement.getTag());
        final List replicatedElements = element.getLayoutElements();
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
    void testUnionLayout() throws IOException, Pack200Exception {
        final MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(new MockSegment(),
                new AttributeLayout("test", AttributeLayout.CONTEXT_CODE, "TB(55)[FH](23)[]()[RSH]", 26));
        final List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        final Union element = (Union) layoutElements.get(0);
        final Integral tag = element.getUnionTag();
        assertEquals("B", tag.getTag());
        final List unionCases = element.getUnionCases();
        assertEquals(2, unionCases.size());
        final UnionCase firstCase = (UnionCase) unionCases.get(0);
        assertTrue(firstCase.hasTag(55));
        assertFalse(firstCase.hasTag(23));
        List body = firstCase.getBody();
        assertEquals(1, body.size());
        final Integral bodyElement = (Integral) body.get(0);
        assertEquals("FH", bodyElement.getTag());
        final UnionCase secondCase = (UnionCase) unionCases.get(1);
        assertTrue(secondCase.hasTag(23));
        assertFalse(secondCase.hasTag(55));
        body = secondCase.getBody();
        assertEquals(0, body.size());
        final List defaultBody = element.getDefaultCaseBody();
        assertEquals(1, defaultBody.size());
        final Reference ref = (Reference) defaultBody.get(0);
        assertEquals("RSH", ref.getTag());
    }

}
