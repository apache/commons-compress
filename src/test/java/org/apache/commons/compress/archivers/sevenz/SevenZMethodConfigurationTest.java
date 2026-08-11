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
package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.LZMA2Options;

class SevenZMethodConfigurationTest {

    @Test
    void testShouldAllowLZMA2OptionsForLZMA() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA, new LZMA2Options()).getOptions());
    }

    @Test
    void testShouldAllowLZMA2OptionsForLZMA2() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2, new LZMA2Options()).getOptions());
    }

    @Test
    void testShouldAllowNullOptions() throws ArchiveException {
        assertNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2, null).getOptions());
    }

    @Test
    void testShouldAllowNumberForBzip2() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.BZIP2, 42).getOptions());
    }

    @Test
    void testShouldAllowNumberForDeflate() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.DEFLATE, 42).getOptions());
    }

    @Test
    void testShouldAllowNumberForLZMA() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA, 42).getOptions());
    }

    @Test
    void testShouldAllowNumberForLZMA2() throws ArchiveException {
        assertNotNull(new SevenZMethodConfiguration(SevenZMethod.LZMA2, 42).getOptions());
    }

    @Test
    void testShouldNotAllowStringOptionsForLZMA() {
        assertThrows(ArchiveException.class, () -> new SevenZMethodConfiguration(SevenZMethod.LZMA, StringUtils.EMPTY));
    }

    @Test
    void testShouldNotAllowStringOptionsForLZMA2() {
        assertThrows(ArchiveException.class, () -> new SevenZMethodConfiguration(SevenZMethod.LZMA2, StringUtils.EMPTY));
    }

}
