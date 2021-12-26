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
package org.apache.commons.compress.compressors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

public final class ZTestCase extends AbstractTestCase {

    @Test
    public void testZUnarchive() throws Exception {
        testUnarchive(ZCompressorInputStream::new);
    }

    @Test
    public void testZUnarchiveViaFactory() throws Exception {
        testUnarchive(is -> new CompressorStreamFactory()
            .createCompressorInputStream(CompressorStreamFactory.Z, is));
    }

    @Test
    public void testZUnarchiveViaAutoDetection() throws Exception {
        testUnarchive(is -> new CompressorStreamFactory()
            .createCompressorInputStream(new BufferedInputStream(is)));
    }

    @Test
    public void testMatches() {
        assertFalse(ZCompressorInputStream.matches(new byte[] { 1, 2, 3, 4 }, 4));
        assertFalse(ZCompressorInputStream.matches(new byte[] { 0x1f, 2, 3, 4 }, 4));
        assertFalse(ZCompressorInputStream.matches(new byte[] { 1, (byte)0x9d, 3, 4 },
                                                   4));
        assertFalse(ZCompressorInputStream.matches(new byte[] { 0x1f, (byte) 0x9d, 3, 4 },
                                                   3));
        assertTrue(ZCompressorInputStream.matches(new byte[] { 0x1f, (byte) 0x9d, 3, 4 },
                                                  4));
    }

    private void testUnarchive(final StreamWrapper<CompressorInputStream> wrapper) throws Exception {
        final File input = getFile("bla.tar.Z");
        final File output = new File(dir, "bla.tar");
        try (InputStream is = Files.newInputStream(input.toPath())) {
            try (InputStream in = wrapper.wrap(is);
                    OutputStream out = Files.newOutputStream(output.toPath())) {
                IOUtils.copy(in, out);
            }
        }
    }

}
