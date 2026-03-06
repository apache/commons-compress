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

package org.apache.commons.compress.harmony.pack200;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CPIntTest {

    @Test
    void testEquals() {
        final CPInt a = new CPInt(42);
        final CPInt b = new CPInt(42);
        final CPInt c = new CPInt(99);
        // Reflexivity
        assertEquals(a, a);
        // Symmetry
        assertEquals(a, b);
        assertEquals(b, a);
        // Inequality
        assertNotEquals(a, c);
        assertNotEquals(c, a);
        // Null and different type
        assertNotEquals(null, a);
        assertNotEquals(a, "42");
    }

    @Test
    void testEqualsEdgeCases() {
        final CPInt zero = new CPInt(0);
        final CPInt minValue = new CPInt(Integer.MIN_VALUE);
        final CPInt maxValue = new CPInt(Integer.MAX_VALUE);
        assertEquals(zero, new CPInt(0));
        assertEquals(minValue, new CPInt(Integer.MIN_VALUE));
        assertEquals(maxValue, new CPInt(Integer.MAX_VALUE));
        assertNotEquals(zero, minValue);
        assertNotEquals(zero, maxValue);
        assertNotEquals(minValue, maxValue);
    }

    @Test
    void testHashCode() {
        final CPInt a = new CPInt(42);
        final CPInt b = new CPInt(42);
        final CPInt c = new CPInt(99);
        // Equal objects must have equal hash codes
        assertEquals(a.hashCode(), b.hashCode());
        // Unequal objects should (typically) have different hash codes
        assertNotEquals(a.hashCode(), c.hashCode());
    }
}
