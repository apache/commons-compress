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

package org.apache.commons.compress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;

class ChainingTest extends AbstractTest {

    @Test
    void testTarBzip2() throws Exception {
        try (TarArchiveInputStream is = TarArchiveInputStream.builder()
                .setInputStream(new BZip2CompressorInputStream(newInputStream("bla.tar.bz2")))
                .get()) {
            final TarArchiveEntry entry = is.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.xml", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
        }
    }

    @Test
    void testTarGzip() throws Exception {
        try (TarArchiveInputStream is = TarArchiveInputStream.builder()
                .setInputStream(GzipCompressorInputStream.builder()
                        .setURI(getURI("bla.tgz"))
                        .get())
                .get()) {
            final TarArchiveEntry entry = is.getNextEntry();
            assertNotNull(entry);
            assertEquals("test1.xml", entry.getName());
            assertEquals(TarConstants.LF_NORMAL, entry.getLinkFlag());
        }
    }
}
