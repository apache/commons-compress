/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class GeneralPurposeBitTest {

    @Test
    public void testDefaults() {
        assertFalse(new GeneralPurposeBit().usesDataDescriptor());
        assertFalse(new GeneralPurposeBit().usesUTF8ForNames());
        assertFalse(new GeneralPurposeBit().usesEncryption());
        assertFalse(new GeneralPurposeBit().usesStrongEncryption());
        final byte[] b = new byte[2];
        assertTrue(Arrays.equals(b, new GeneralPurposeBit().encode()));
    }

    @Test
    public void testParseEdgeCases() {
        assertFalse(GeneralPurposeBit.parse(new byte[2], 0)
                    .usesDataDescriptor());
        assertFalse(GeneralPurposeBit.parse(new byte[2], 0)
                    .usesUTF8ForNames());
        assertFalse(GeneralPurposeBit.parse(new byte[2], 0)
                    .usesEncryption());
        assertFalse(GeneralPurposeBit.parse(new byte[2], 0)
                    .usesStrongEncryption());
        assertTrue(GeneralPurposeBit.parse(new byte[] {(byte) 255, (byte) 255},
                                           0)
                   .usesDataDescriptor());
        assertTrue(GeneralPurposeBit.parse(new byte[] {(byte) 255, (byte) 255},
                                           0)
                   .usesUTF8ForNames());
        assertTrue(GeneralPurposeBit.parse(new byte[] {(byte) 255, (byte) 255},
                                           0)
                   .usesEncryption());
        assertTrue(GeneralPurposeBit.parse(new byte[] {(byte) 255, (byte) 255},
                                           0)
                   .usesStrongEncryption());
    }

    @Test
    public void testDataDescriptor() {
        final byte[] flags = new byte[] {(byte) 8, (byte) 0};
        assertTrue(GeneralPurposeBit.parse(flags, 0).usesDataDescriptor());
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useDataDescriptor(true);
        assertTrue(Arrays.equals(flags, b.encode()));
    }

    @Test
    public void testLanguageEncodingFlag() {
        final byte[] flags = new byte[] {(byte) 0, (byte) 8};
        assertTrue(GeneralPurposeBit.parse(flags, 0).usesUTF8ForNames());
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useUTF8ForNames(true);
        assertTrue(Arrays.equals(flags, b.encode()));
    }

    @Test
    public void testEncryption() {
        final byte[] flags = new byte[] {(byte) 1, (byte) 0};
        assertTrue(GeneralPurposeBit.parse(flags, 0).usesEncryption());
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useEncryption(true);
        assertTrue(Arrays.equals(flags, b.encode()));
    }

    @Test
    public void testStrongEncryption() {
        byte[] flags = new byte[] {(byte) 65, (byte) 0};
        assertTrue(GeneralPurposeBit.parse(flags, 0).usesStrongEncryption());
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useStrongEncryption(true);
        assertTrue(b.usesEncryption());
        assertTrue(Arrays.equals(flags, b.encode()));

        flags = new byte[] {(byte) 64, (byte) 0};
        assertFalse(GeneralPurposeBit.parse(flags, 0).usesStrongEncryption());
    }

    @Test
    public void testClone() {
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useStrongEncryption(true);
        b.useUTF8ForNames(true);
        assertEquals(b, b.clone());
        assertNotSame(b, b.clone());
    }
}
