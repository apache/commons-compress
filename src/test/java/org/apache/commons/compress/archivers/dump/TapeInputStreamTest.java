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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TapeInputStreamTest extends AbstractTest {

    @Test
    void testResetBlockSizeBadSignature() throws IOException {
        assertThrows(ArchiveException.class,
                () -> SevenZFile.builder().setPath("src/test/resources/org/apache/commons/compress/dump/resetBlockSize.bin").get().close());
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, Integer.MAX_VALUE / 1000, Integer.MAX_VALUE })
    void testResetBlockSizeWithInvalidValues(final int recsPerBlock) throws Exception {
        try (TapeInputStream tapeInputStream = new TapeInputStream(new ByteArrayInputStream(new byte[1]))) {
            assertThrows(ArchiveException.class, () -> tapeInputStream.resetBlockSize(recsPerBlock, true));
        }
    }
}
