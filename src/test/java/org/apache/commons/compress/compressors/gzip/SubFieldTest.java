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
package org.apache.commons.compress.compressors.gzip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.commons.compress.compressors.gzip.ExtraField.SubField;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ExtraField.SubField}.
 */
public class SubFieldTest {

    @Test
    public void testEquals() {
        assertEquals(new SubField((byte) 0, (byte) 0, new byte[0]), new SubField((byte) 0, (byte) 0, new byte[0]));
        assertEquals(new SubField((byte) 9, (byte) 9, new byte[9]), new SubField((byte) 9, (byte) 9, new byte[9]));
        // not equals
        assertNotEquals(new SubField((byte) 0, (byte) 9, new byte[9]), new SubField((byte) 9, (byte) 9, new byte[9]));
        assertNotEquals(new SubField((byte) 9, (byte) 0, new byte[9]), new SubField((byte) 9, (byte) 9, new byte[9]));
        assertNotEquals(new SubField((byte) 9, (byte) 9, new byte[0]), new SubField((byte) 9, (byte) 9, new byte[9]));
        assertNotEquals(new SubField((byte) 9, (byte) 9, new byte[9]), new SubField((byte) 9, (byte) 9, new byte[] { 9 }));
    }

    @Test
    public void testHashCode() {
        assertEquals(new SubField((byte) 0, (byte) 0, new byte[0]).hashCode(), new SubField((byte) 0, (byte) 0, new byte[0]).hashCode());
        assertEquals(new SubField((byte) 9, (byte) 9, new byte[9]).hashCode(), new SubField((byte) 9, (byte) 9, new byte[9]).hashCode());
        // not equals
        assertNotEquals(new SubField((byte) 0, (byte) 9, new byte[9]).hashCode(), new SubField((byte) 9, (byte) 9, new byte[9]).hashCode());
        assertNotEquals(new SubField((byte) 9, (byte) 0, new byte[9]).hashCode(), new SubField((byte) 9, (byte) 9, new byte[9]).hashCode());
        assertNotEquals(new SubField((byte) 9, (byte) 9, new byte[0]).hashCode(), new SubField((byte) 9, (byte) 9, new byte[9]).hashCode());
        assertNotEquals(new SubField((byte) 9, (byte) 9, new byte[9]).hashCode(), new SubField((byte) 9, (byte) 9, new byte[] { 9 }).hashCode());
    }
}
