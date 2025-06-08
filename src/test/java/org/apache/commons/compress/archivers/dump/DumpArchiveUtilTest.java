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
package org.apache.commons.compress.archivers.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class DumpArchiveUtilTest {

    @Test
    void testConvert16() {
        assertEquals(0xABCD, DumpArchiveUtil.convert16(new byte[] { (byte) 0xCD, (byte) 0xAB }, 0));
    }

    @Test
    void testConvert32() {
        assertEquals(0xABCDEF01, DumpArchiveUtil.convert32(new byte[] { 1, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB }, 0));
    }

    @Test
    void testConvert64() {
        assertEquals(0xABCDEF0123456780L, DumpArchiveUtil.convert64(new byte[] { (byte) 0x80, 0x67, 0x45, 0x23, 1, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB }, 0));
    }

    @Test
    void testDecodeInvalidArguments() {
        assertThrows(IOException.class, () -> DumpArchiveUtil.decode(null, new byte[10], 10, -1));
    }

    @Test
    void testVerifyNoMagic() {
        assertFalse(DumpArchiveUtil.verify(new byte[32]));
    }

    @Test
    void testVerifyNullArgument() {
        assertFalse(DumpArchiveUtil.verify(null));
    }

}

