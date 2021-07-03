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

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.unpack200.bytecode.CPDouble;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFloat;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInteger;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPLong;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMember;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.SourceFileAttribute;

public class ClassFileEntryTest extends TestCase {

    public void testUTF8() {
        CPUTF8 u1 = new CPUTF8(new String("thing"), 1); //$NON-NLS-1$
        CPUTF8 u2 = new CPUTF8(new String("thing"), 1); //$NON-NLS-1$
        CPUTF8 u3 = new CPUTF8(new String("otherthing"), 2); //$NON-NLS-1$
        checkEquality(u1, u2, "thing", u3);
    }

    private void checkEquality(Object equal1, Object equal2, String toString,
            Object unequal) {
        assertEquals(equal1, equal2);
        assertEquals(equal1.hashCode(), equal2.hashCode());
        assertTrue(equal1.toString().indexOf(toString) >= 0);
        assertFalse(equal1.equals(unequal));
        assertFalse(equal2.equals(unequal));
        assertFalse(unequal.equals(equal1));
        assertFalse(unequal.equals(equal2));
    }

    public void testSourceAttribute() {
        SourceFileAttribute sfa1 = new SourceFileAttribute(new CPUTF8(
                new String("Thing.java"), 1)); //$NON-NLS-1$
        SourceFileAttribute sfa2 = new SourceFileAttribute(new CPUTF8(
                new String("Thing.java"), 1)); //$NON-NLS-1$
        SourceFileAttribute sfa3 = new SourceFileAttribute(new CPUTF8(
                new String("OtherThing.java"), 2)); //$NON-NLS-1$
        checkEquality(sfa1, sfa2, "Thing.java", sfa3); //$NON-NLS-1$
    }

    public void testCPInteger() {
        CPInteger cp1 = new CPInteger(new Integer(3), 3);
        CPInteger cp2 = new CPInteger(new Integer(3), 3);
        CPInteger cp3 = new CPInteger(new Integer(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    public void testCPLong() {
        CPLong cp1 = new CPLong(new Long(3), 3);
        CPLong cp2 = new CPLong(new Long(3), 3);
        CPLong cp3 = new CPLong(new Long(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    public void testCPDouble() {
        CPDouble cp1 = new CPDouble(new Double(3), 3);
        CPDouble cp2 = new CPDouble(new Double(3), 3);
        CPDouble cp3 = new CPDouble(new Double(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    public void testCPFloat() {
        CPFloat cp1 = new CPFloat(new Float(3), 3);
        CPFloat cp2 = new CPFloat(new Float(3), 3);
        CPFloat cp3 = new CPFloat(new Float(5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    public void testCPString() {
        CPString cp1 = new CPString(new CPUTF8(new String("3"), 3), 3);
        CPString cp2 = new CPString(new CPUTF8(new String("3"), 3), 3);
        CPString cp3 = new CPString(new CPUTF8(new String("5"), 5), 5);
        checkEquality(cp1, cp2, "3", cp3); //$NON-NLS-1$
    }

    public void testCPField() {
        CPMember cp1 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("I", 4),
                0, null);
        CPMember cp2 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("I", 4),
                0, null);
        CPMember cp3 = new CPMember(new CPUTF8("Name", 3), new CPUTF8("Z", 5),
                0, null);
        CPMember cp4 = new CPMember(new CPUTF8("GName", 6), new CPUTF8("I", 4),
                0, null);
        checkEquality(cp1, cp2, "Name", cp3); //$NON-NLS-1$
        checkEquality(cp1, cp2, "I", cp4); //$NON-NLS-1$
    }

}
