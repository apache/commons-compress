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
package org.apache.commons.compress.archivers.cpio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.Test;

public class CpioArchiveOutputStreamTest extends AbstractTestCase {

    @Test
    public void testWriteOldBinary() throws Exception {
        final File f = getFile("test1.xml");
        final File output = new File(dir, "test.cpio");
        try (final OutputStream out = Files.newOutputStream(output.toPath());
                CpioArchiveOutputStream os = new CpioArchiveOutputStream(out, CpioConstants.FORMAT_OLD_BINARY)) {
            os.putArchiveEntry(new CpioArchiveEntry(CpioConstants.FORMAT_OLD_BINARY, f, "test1.xml"));
            Files.copy(f.toPath(), os);
            os.closeArchiveEntry();
        }

        try (CpioArchiveInputStream in = new CpioArchiveInputStream(Files.newInputStream(output.toPath()))) {
            final CpioArchiveEntry e = in.getNextCPIOEntry();
            assertEquals("test1.xml", e.getName());
            assertNull(in.getNextEntry());
        } finally {
        }
    }
}
