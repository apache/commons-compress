/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.compressors.zstandard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ZstdUtilsTest {

    @Test
    public void testMatchesSkippableFrame() {
        final byte[] data = {
            0, (byte) 0x2A, (byte) 0x4D, (byte) 0x18,
        };
        assertFalse(ZstdUtils.matches(data, 4));
        for (byte b = (byte) 0x50; b < 0x60; b++) {
            data[0] = b;
            assertTrue(ZstdUtils.matches(data, 4));
        }
        assertFalse(ZstdUtils.matches(data, 3));
        assertTrue(ZstdUtils.matches(data, 5));
    }

    @Test
    public void testMatchesZstandardFrame() {
        final byte[] data = {
            (byte) 0x28, (byte) 0xB5, (byte) 0x2F, (byte) 0xFD,
        };
        assertFalse(ZstdUtils.matches(data, 3));
        assertTrue(ZstdUtils.matches(data, 4));
        assertTrue(ZstdUtils.matches(data, 5));
        data[3] = '0';
        assertFalse(ZstdUtils.matches(data, 4));
    }
}
