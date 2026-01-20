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

package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpioArchiveEntryTest {

    @ParameterizedTest
    @ValueSource(shorts = {CpioConstants.FORMAT_NEW, CpioConstants.FORMAT_NEW_CRC, CpioConstants.FORMAT_OLD_BINARY})
    void testCpioEntrySize_notOver4GiB_nonOldAsciiFormat(short format) {
        final CpioArchiveEntry entry = new CpioArchiveEntry(format);
        assertThrows(IllegalArgumentException.class, () -> entry.setSize(0x1FFFFFFFFL));
    }

    @Test
    void testCpioEntrySize_oldAsciiFormat_allowsOver4GiB() {
        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_OLD_ASCII);
        entry.setSize(0x1FFFFFFFFL);
    }

    @Test
    void testGetHeaderPadCountOverflow() throws Exception {
        final CpioArchiveEntry entry = new CpioArchiveEntry(CpioConstants.FORMAT_NEW);
        entry.setName("test name");
        assertThrows(ArchiveException.class, () -> entry.getHeaderPadCount(Long.MAX_VALUE));
    }
}
