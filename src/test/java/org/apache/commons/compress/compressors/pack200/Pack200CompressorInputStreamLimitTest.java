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
package org.apache.commons.compress.compressors.pack200;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.BombGuardCompressorInputStream;
import org.apache.commons.compress.compressors.DecompressionBombException;
import org.junit.jupiter.api.Test;

/**
 * Pack200 is the one format with no compressed-byte count ({@link Pack200CompressorInputStream#getCompressedCount()} is
 * {@code -1}). Wrapped in a {@link BombGuardCompressorInputStream} the absolute cap still applies, because the guard counts the
 * output itself; the ratio guard is skipped because the compressed count is unknown.
 */
class Pack200CompressorInputStreamLimitTest {

    private static InputStream open() {
        final InputStream in = Pack200CompressorInputStreamLimitTest.class.getResourceAsStream("/pack200/HelloWorld.pack");
        assertNotNull(in, "missing test resource /pack200/HelloWorld.pack");
        return in;
    }

    private static long drain(final InputStream in) throws IOException {
        final byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
        }
        return total;
    }

    @Test
    void compressedCountIsUnknown() throws Exception {
        try (Pack200CompressorInputStream in = new Pack200CompressorInputStream(open())) {
            assertEquals(-1, in.getCompressedCount());
        }
    }

    @Test
    void guardCountsOutput() throws Exception {
        try (BombGuardCompressorInputStream in = new BombGuardCompressorInputStream(new Pack200CompressorInputStream(open()), -1)) {
            final long read = drain(in);
            assertTrue(read > 0, "expected non-empty output");
            assertEquals(read, in.getUncompressedCount());
        }
    }

    @Test
    void absoluteCapFires() throws Exception {
        try (BombGuardCompressorInputStream in = new BombGuardCompressorInputStream(new Pack200CompressorInputStream(open()), 100)) {
            assertThrows(DecompressionBombException.class, () -> drain(in));
        }
    }

    @Test
    void ratioGuardSkippedWithoutCompressedCount() throws Exception {
        try (BombGuardCompressorInputStream in = new BombGuardCompressorInputStream(new Pack200CompressorInputStream(open()), -1, 1, 0)) {
            assertDoesNotThrow(() -> drain(in));
        }
    }
}
