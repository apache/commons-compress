/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.commons.compress.harmony.unpack200.tests.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.harmony.unpack200.bytecode.CPDouble;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFloat;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInteger;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPLong;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMember;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.SourceFileAttribute;
import org.junit.jupiter.api.Test;

public class ClassFileEntryTest {

    private void checkEquality(final Object equal1, final Object equal2, final String toString,
            final Object unequal) {
        assertEquals(equal1, equal2);
        assertEquals(equal1.hashCode(), equal2.hashCode());
        assertTrue(equal1.toString().indexOf(toString) >= 0);
        assertNotEquals(equal1, unequal);
        assertNotEquals(equal2, unequal);
        assertNotEquals(unequal, equal1);
        assertNotEquals(unequal, equal2);
    }

    @Test
    public void testCPDouble() {
        final CPDouble cp1 = new CPDouble(Double.valueOf(3), 3);
        final CPDouble cp2 = new CPDouble(Double.valueOf(3), 3);
        final CPDouble cp3 = new CPDouble(Double.valueOf(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    @Test
    public void testCPField() {
        final CPMember cp1 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("I", 4),
                0, null);
        final CPMember cp2 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("I", 4),
                0, null);
        final CPMember cp3 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("Z", 5),
                0, null);
        final CPMember cp4 = new CPMember(new CPUTF8("GName", 6), new CPUTF8("I", 4),
                0, null);
        checkEquality(cp1, cp2, "Name", cp3); //$NON-NLS-1$
        checkEquality(cp1, cp2, "I", cp4); //$NON-NLS-1$
    }

    @Test
    public void testCPFloat() {
        final CPFloat cp1 = new CPFloat(Float.valueOf(3), 3);
        final CPFloat cp2 = new CPFloat(Float.valueOf(3), 3);
        final CPFloat cp3 = new CPFloat(Float.valueOf(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    @Test
    public void testCPInteger() {
        final CPInteger cp1 = new CPInteger(Integer.valueOf(3), 3);
        final CPInteger cp2 = new CPInteger(Integer.valueOf(3), 3);
        final CPInteger cp3 = new CPInteger(Integer.valueOf(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    @Test
    public void testCPLong() {
        final CPLong cp1 = new CPLong(Long.valueOf(3), 3);
        final CPLong cp2 = new CPLong(Long.valueOf(3), 3);
        final CPLong cp3 = new CPLong(Long.valueOf(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    @Test
    public void testCPString() {
        final CPString cp1 = new CPString(new CPUTF8("3", 3), 3);
        final CPString cp2 = new CPString(new CPUTF8("3", 3), 3);
        final CPString cp3 = new CPString(new CPUTF8("5", 5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    @Test
    public void testSourceAttribute() {
        final SourceFileAttribute sfa1 = new SourceFileAttribute(new CPUTF8(
                "Thing.java", 1)); //$NON-NLS-1$
        final SourceFileAttribute sfa2 = new SourceFileAttribute(new CPUTF8(
                "Thing.java", 1)); //$NON-NLS-1$
        final SourceFileAttribute sfa3 = new SourceFileAttribute(new CPUTF8(
                "OtherThing.java", 2)); //$NON-NLS-1$
        checkEquality(sfa1, sfa2, "Thing.java", sfa3); //$NON-NLS-1$
    }

    @Test
    public void testUTF8() {
        final CPUTF8 u1 = new CPUTF8("thing", 1); //$NON-NLS-1$
        final CPUTF8 u2 = new CPUTF8("thing", 1); //$NON-NLS-1$
        final CPUTF8 u3 = new CPUTF8("otherthing", 2); //$NON-NLS-1$
        checkEquality(u1, u2, "thing", u3);
    }

}
