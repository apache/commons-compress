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

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.unpack200.IcTuple;

public class ICTupleTest extends TestCase {

    public void testPredictedClassTupleParsing() {
        IcTuple tuple = new IcTuple(
                "orw/SimpleHelloWorld$SimpleHelloWorldInner", 0, null, null,
                -1, -1, -1, -1);
        assertEquals("SimpleHelloWorldInner", tuple.simpleClassName());
        assertEquals("orw/SimpleHelloWorld", tuple.outerClassString());

        tuple = new IcTuple("java/util/AbstractList$2$Local", 0, null, null,
                -1, -1, -1, -1);
        assertEquals("Local", tuple.simpleClassName());
        assertEquals("java/util/AbstractList$2", tuple.outerClassString());

        tuple = new IcTuple("java/util/AbstractList#2#Local", 0, null, null,
                -1, -1, -1, -1);
        assertEquals("Local", tuple.simpleClassName());
        assertEquals("java/util/AbstractList$2", tuple.outerClassString());

        tuple = new IcTuple("java/util/AbstractList$1", 0, null, null, -1, -1,
                -1, -1);
        assertEquals("1", tuple.simpleClassName());
        assertEquals("java/util/AbstractList", tuple.outerClassString());
    }

    public void testExplicitClassTupleParsing() {
        IcTuple tuple = new IcTuple("Foo$$2$Local", IcTuple.NESTED_CLASS_FLAG,
                null, "$2$Local", -1, -1, -1, -1);
        assertEquals("$2$Local", tuple.simpleClassName());
        assertEquals("Foo$$2", tuple.outerClassString());

        tuple = new IcTuple("Red$Herring", IcTuple.NESTED_CLASS_FLAG,
                "Red$Herring", null, -1, -1, -1, -1);
        assertEquals("Herring", tuple.simpleClassName());
        assertEquals("Red$Herring", tuple.outerClassString());

        tuple = new IcTuple("X$1$Q", IcTuple.NESTED_CLASS_FLAG, "X$1", "Q", -1,
                -1, -1, -1);
        assertEquals("Q", tuple.simpleClassName());
        assertEquals("X$1", tuple.outerClassString());
    }
}
