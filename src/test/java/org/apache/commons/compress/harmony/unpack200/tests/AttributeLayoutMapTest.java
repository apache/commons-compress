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

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.AttributeLayout;
import org.apache.commons.compress.harmony.unpack200.AttributeLayoutMap;

public class AttributeLayoutMapTest extends TestCase {

    public void testRepeatable() throws Pack200Exception {
        // Check we can retrieve a default layout
        AttributeLayoutMap a = new AttributeLayoutMap();
        AttributeLayout layout = a.getAttributeLayout("SourceFile",
                AttributeLayout.CONTEXT_CLASS);
        assertNotNull(layout);
        assertEquals("RUNH", layout.getLayout());
        assertEquals(17, layout.getIndex());
        // and that we can change it
        a.add(new AttributeLayout("SourceFile", AttributeLayout.CONTEXT_CLASS,
                "FROG", 17));
        layout = a.getAttributeLayout("SourceFile",
                AttributeLayout.CONTEXT_CLASS);
        assertNotNull(layout);
        assertEquals("FROG", layout.getLayout());
        assertTrue(layout.matches(1 << 17));
        assertFalse(layout.matches(1 << 16));
        assertTrue(layout.matches(-1));
        assertFalse(layout.matches(0));
        // and that changes don't affect subsequent defaults
        AttributeLayoutMap b = new AttributeLayoutMap();
        layout = b.getAttributeLayout("SourceFile",
                AttributeLayout.CONTEXT_CLASS);
        assertNotNull(layout);
        assertEquals("RUNH", layout.getLayout());

    }
}
