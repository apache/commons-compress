/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.ar;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.AbstractTestCase;
import org.junit.jupiter.api.Test;

public class ArArchiveOutputStreamTest extends AbstractTestCase {

    @Test
    public void testLongFileNamesCauseExceptionByDefault() throws IOException {
        try (ArArchiveOutputStream os = new ArArchiveOutputStream(new ByteArrayOutputStream())) {
            final ArArchiveEntry ae = new ArArchiveEntry("this_is_a_long_name.txt", 0);
            final IOException ex = assertThrows(IOException.class, () -> os.putArchiveEntry(ae),
                    "Expected an exception");
            assertTrue(ex.getMessage().startsWith("File name too long"));
        }
    }

    @Test
    public void testLongFileNamesWorkUsingBSDDialect() throws Exception {
        final File[] df = createTempDirAndFile();
        try (OutputStream fos = Files.newOutputStream(df[1].toPath());
             ArArchiveOutputStream os = new ArArchiveOutputStream(fos)) {
            os.setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            final ArArchiveEntry ae = new ArArchiveEntry("this_is_a_long_name.txt", 14);
            os.putArchiveEntry(ae);
            os.write(new byte[] { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'w', 'o', 'r', 'l', 'd', '!', '\n' });
            os.closeArchiveEntry();

            final List<String> expected = new ArrayList<>();
            expected.add("this_is_a_long_name.txt");
            checkArchiveContent(df[1], expected);
        } finally {
            rmdir(df[0]);
        }
    }
}
