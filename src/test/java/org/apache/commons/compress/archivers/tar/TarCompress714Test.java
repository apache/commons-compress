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

package org.apache.commons.compress.archivers.tar;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

/**
 * Tests https://issues.apache.org/jira/browse/COMPRESS-714
 */
public class TarCompress714Test {

    @Test
    public void testIllegalPosition() throws IOException {
        final byte[] data = { 46, 101, 97, 115, 97, 47, 120, -64, 72, 98, -18, 2, 53, 101, 112, 0, 0, 115, 8, 0, 0, 0, 112, 115, 40, 1, 0, 36, 2, 108, 0, -1,
                -1, 0, 0, 0, 74, 0, 0, 0, 0, -1, 0, 1, 67, -2, 8, 0, 0, 0, 0, -64, -1, -1, -34, 9, 0, -120, -120, -120, -120, -120, -120, -120, -120, -120,
                -120, -120, -120, -120, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 63, 0, 0, 0, 0, 0, 0, -65, -1, -126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128,
                -1, -1, -1, -1, 0, -122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 35, 0, 53, 0, 0, 0, 0, 0, 0, 32, 56, 0, 0, 0, 0, 0, 0, -120, 55, 55,
                55, 55, 55, 55, 55, 55, 55, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -105, 0, 0, 0, 49, 36, -1, -1, -1, -1, -1, -1, -126, 0, 0, 0, 0, 0,
                0, 0, 53, 0, 0, 0, -128, 0, 0, 0, 0, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 46, 101, 97, 115, 97, 47, 120, -64, 72, 98, -18, 2, 53,
                101, 112, 0, 0, 115, 8, 0, 0, 0, 112, 115, 40, 1, 0, 2, 108, 0, -1, -1, 0, 0, 0, 74, 0, 0, 0, 0, -1, 0, 1, 67, -2, 8, 0, 0, 0, 0, -64, -1, -1,
                -34, 9, 0, -120, -120, -120, -120, -120, -120, -120, -120, -120, -120, -120, -120, -120, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 63, 0, 0,
                0, 0, 0, 0, -65, -1, -126, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, -1, -1, -1, -1, 0, -122, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 35, 0, 53, 0,
                0, 0, 0, 0, 0, 32, 56, 0, 0, 0, 0, 0, 0, -120, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -105, 0, 0,
                0, 49, 36, -1, -1, -1, -1, -1, -1, -126, 0, 0, 0, 0, 0, 0, 0, 53, 0, 0, 0, -128, 0, 0, 0, 0, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -65,
                -1, -126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, -1, -1, -1, -1, 0, -122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 35, 0, 53, 0, 0, 0,
                0, 0, 0, 32, 56, 0, 0, 0, 0, 0, 0, 0 };
        assertThrows(ArchiveException.class, () -> TarFile.builder().setChannel(new SeekableInMemoryByteChannel(data)).get());
        assertThrows(ArchiveException.class, () -> TarFile.builder().setByteArray(data).get());
    }
}
