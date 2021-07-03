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

import java.io.IOException;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.AttributeLayout;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Call;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Callable;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Integral;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Reference;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Replication;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.Union;
import org.apache.commons.compress.harmony.unpack200.NewAttributeBands.UnionCase;
import org.apache.commons.compress.harmony.unpack200.Segment;

/**
 * Tests for unpack200 support for non-predefined attributes
 */
public class NewAttributeBandsTest extends AbstractBandsTestCase {

    public void testEmptyLayout() throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_CLASS, "", 25));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(0, layoutElements.size());
    }

    public void testIntegralLayout() throws IOException, Pack200Exception {
        tryIntegral("B");
        tryIntegral("FB");
        tryIntegral("SB");
        tryIntegral("H");
        tryIntegral("FH");
        tryIntegral("SH");
        tryIntegral("I");
        tryIntegral("FI");
        tryIntegral("SI");
        tryIntegral("PB");
        tryIntegral("OB");
        tryIntegral("OSB");
        tryIntegral("POB");
        tryIntegral("PH");
        tryIntegral("OH");
        tryIntegral("OSH");
        tryIntegral("POH");
        tryIntegral("PI");
        tryIntegral("OI");
        tryIntegral("OSI");
        tryIntegral("POI");
    }

    public void tryIntegral(String layout) throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_CLASS, layout, 25));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        Integral element = (Integral) layoutElements.get(0);
        assertEquals(layout, element.getTag());
    }

    public void testReplicationLayout() throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_CLASS, "NH[PHOHRUHRSHH]", 25));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        Replication element = (Replication) layoutElements.get(0);
        Integral countElement = element.getCountElement();
        assertEquals("H", countElement.getTag());
        List replicatedElements = element.getLayoutElements();
        assertEquals(5, replicatedElements.size());
        Integral firstElement = (Integral) replicatedElements.get(0);
        assertEquals("PH", firstElement.getTag());
        Integral secondElement = (Integral) replicatedElements.get(1);
        assertEquals("OH", secondElement.getTag());
        Reference thirdElement = (Reference) replicatedElements.get(2);
        assertEquals("RUH", thirdElement.getTag());
        Reference fourthElement = (Reference) replicatedElements.get(3);
        assertEquals("RSH", fourthElement.getTag());
        Integral fifthElement = (Integral) replicatedElements.get(4);
        assertEquals("H", fifthElement.getTag());
    }

    public void testReferenceLayouts() throws IOException, Pack200Exception {
        tryReference("KIB");
        tryReference("KIH");
        tryReference("KII");
        tryReference("KINH");
        tryReference("KJH");
        tryReference("KDH");
        tryReference("KSH");
        tryReference("KQH");
        tryReference("RCH");
        tryReference("RSH");
        tryReference("RDH");
        tryReference("RFH");
        tryReference("RMH");
        tryReference("RIH");
        tryReference("RUH");
        tryReference("RQH");
        tryReference("RQNH");
        tryReference("RQNI");
    }

    private void tryReference(String layout) throws IOException,
            Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_CODE, layout, 26));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        Reference element = (Reference) layoutElements.get(0);
        assertEquals(layout, element.getTag());
    }

    public void testUnionLayout() throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_CODE,
                        "TB(55)[FH](23)[]()[RSH]", 26));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(1, layoutElements.size());
        Union element = (Union) layoutElements.get(0);
        Integral tag = element.getUnionTag();
        assertEquals("B", tag.getTag());
        List unionCases = element.getUnionCases();
        assertEquals(2, unionCases.size());
        UnionCase firstCase = (UnionCase) unionCases.get(0);
        assertTrue(firstCase.hasTag(55));
        assertFalse(firstCase.hasTag(23));
        List body = firstCase.getBody();
        assertEquals(1, body.size());
        Integral bodyElement = (Integral) body.get(0);
        assertEquals("FH", bodyElement.getTag());
        UnionCase secondCase = (UnionCase) unionCases.get(1);
        assertTrue(secondCase.hasTag(23));
        assertFalse(secondCase.hasTag(55));
        body = secondCase.getBody();
        assertEquals(0, body.size());
        List defaultBody = element.getDefaultCaseBody();
        assertEquals(1, defaultBody.size());
        Reference ref = (Reference) defaultBody.get(0);
        assertEquals("RSH", ref.getTag());
    }

    public void testLayoutWithCalls() throws IOException, Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(),
                new AttributeLayout(
                        "test",
                        AttributeLayout.CONTEXT_FIELD,
                        "[NH[(1)]][RSH NH[RUH(1)]][TB(66,67,73,83,90)[KIH](68)[KDH](70)[KFH](74)[KJH](99)[RSH](101)[RSH RUH](115)[RUH](91)[NH[(0)]](64)[RSH[RUH(0)]]()[]]",
                        26));
        List layoutElements = newAttributeBands.getLayoutElements();
        assertEquals(3, layoutElements.size());
        Callable firstCallable = (Callable) layoutElements.get(0);
        Callable secondCallable = (Callable) layoutElements.get(1);
        Callable thirdCallable = (Callable) layoutElements.get(2);
        List firstBody = firstCallable.getBody();
        assertEquals(1, firstBody.size());
        Replication rep = (Replication) firstBody.get(0);
        List repBody = rep.getLayoutElements();
        assertEquals(1, repBody.size());
        Call call = (Call) repBody.get(0);
        assertEquals(1, call.getCallableIndex());
        assertEquals(secondCallable, call.getCallable());
        assertFalse(firstCallable.isBackwardsCallable());
        assertFalse(secondCallable.isBackwardsCallable());
        assertFalse(thirdCallable.isBackwardsCallable());
    }

    public void testLayoutWithBackwardsCall() throws IOException,
            Pack200Exception {
        MockNewAttributeBands newAttributeBands = new MockNewAttributeBands(
                new MockSegment(), new AttributeLayout("test",
                        AttributeLayout.CONTEXT_METHOD, "[NH[(1)]][KIH][(-1)]",
                        20));
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
                new AttributeLayout("test", AttributeLayout.CONTEXT_METHOD,
                        "[NH[(1)]][KIH][(-2)]", 20));
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
                new AttributeLayout("test", AttributeLayout.CONTEXT_METHOD,
                        "[NH[(1)]][KIH][(0)]", 20));
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

    private class MockNewAttributeBands extends NewAttributeBands {

        public MockNewAttributeBands(Segment segment, AttributeLayout layout)
                throws IOException {
            super(segment, layout);
        }

        public List getLayoutElements() {
            return attributeLayoutElements;
        }
    }
}
