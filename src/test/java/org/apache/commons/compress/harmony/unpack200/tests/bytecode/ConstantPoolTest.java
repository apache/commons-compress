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

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMember;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassConstantPool;

public class ConstantPoolTest extends TestCase {

    private ClassConstantPool pool;

    @Override
    public void setUp() {
        pool = new ClassConstantPool();
    }

    public void testDuplicateUTF8() {
        CPUTF8 u1 = new CPUTF8("thing", 1);
        CPUTF8 u2 = new CPUTF8("thing", 1);
        pool.add(u1);
        pool.add(u2);
        assertEquals(1, pool.size());
    }

    public void testDuplicateField() {
        CPMember cp1 = new CPMember(new CPUTF8("name", 1), new CPUTF8("I", 2),
                0, null);
        pool.add(cp1);
        pool.addNestedEntries();
        assertEquals(2, pool.size());
        CPMember cp2 = new CPMember(new CPUTF8("name", 1), new CPUTF8("I", 2),
                0, null);
        pool.add(cp2);
        pool.addNestedEntries();
        assertEquals(2, pool.size());
    }

    public void testIndex() {
        pool.add(new CPUTF8("OtherThing", 1));
        CPUTF8 u1 = new CPUTF8("thing", 2);
        pool.add(u1);
        pool.resolve(new Segment());
        assertTrue(pool.indexOf(u1) > 0);
    }

    public void testEntries() {
        pool.add(new CPClass(new CPUTF8("RandomClass", 1), 10));
        pool.add(new CPClass(new CPUTF8("RandomClass2", 2), 20));
        assertEquals(2, pool.entries().size());
    }
}
