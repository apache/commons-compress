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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTest;
import org.junit.jupiter.api.Test;

class CpioArchiveOutputStreamTest extends AbstractTest {

    @Test
    void testWriteOldBinary() throws Exception {
        final File file = getFile("test1.xml");
        final File output = newTempFile("test.cpio");
        final CpioArchiveOutputStream ref;
        try (CpioArchiveOutputStream outputStream = new CpioArchiveOutputStream(Files.newOutputStream(output.toPath()), CpioConstants.FORMAT_OLD_BINARY)) {
            ref = outputStream;
            outputStream.putArchiveEntry(new CpioArchiveEntry(CpioConstants.FORMAT_OLD_BINARY, file, "test1.xml"));
            outputStream.write(file);
            outputStream.closeArchiveEntry();
        }
        assertTrue(ref.isClosed());
        try (CpioArchiveInputStream in = CpioArchiveInputStream.builder().setFile(output).get()) {
            final CpioArchiveEntry e = in.getNextCPIOEntry();
            assertEquals("test1.xml", e.getName());
            assertNull(in.getNextEntry());
        }
    }
}
