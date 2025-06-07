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

package org.apache.commons.compress.compressors.lzma;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tukaani.xz.LZMA2Options;

/**
 * Tests {@link LZMACompressorOutputStream}.
 */
public class LZMACompressorOutputStreamTest {

    @TempDir
    static Path tempDir;

    private void roundtrip(final Path outPath, final LZMA2Options options) throws IOException {
        final String data = "Hello World!";
        // @formatter:off
        try (LZMACompressorOutputStream out = LZMACompressorOutputStream.builder()
                .setPath(outPath)
                .setLzma2Options(options)
                .get()) {
            out.writeUtf8(data);
        }
        // @formatter:on
        try (LZMACompressorInputStream out = LZMACompressorInputStream.builder().setPath(outPath).setMemoryLimitKiB(-1).get()) {
            assertEquals(data, IOUtils.toString(out, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testBuilderOptionsAll() throws IOException {
        final int dictSize = LZMA2Options.DICT_SIZE_MIN;
        final int lc = LZMA2Options.LC_LP_MAX - 4;
        final int lp = LZMA2Options.LC_LP_MAX - 4;
        final int pb = LZMA2Options.PB_MAX;
        final int mode = LZMA2Options.MODE_NORMAL;
        final int niceLen = LZMA2Options.NICE_LEN_MIN;
        final int mf = LZMA2Options.MF_BT4;
        final int depthLimit = 50;
        roundtrip(tempDir.resolve("out.lzma"), new LZMA2Options(dictSize, lc, lp, pb, mode, niceLen, mf, depthLimit));
    }

    @Test
    void testBuilderOptionsDefault() throws IOException {
        roundtrip(tempDir.resolve("out.lzma"), new LZMA2Options());
    }

    @Test
    void testBuilderOptionsPreset() throws IOException {
        roundtrip(tempDir.resolve("out.lzma"), new LZMA2Options(LZMA2Options.PRESET_MAX));
    }

    @Test
    void testBuilderPath() throws IOException {
        // This test does not use LZMA2Options
        final Path outPath = tempDir.resolve("out.lzma");
        try (LZMACompressorOutputStream out = LZMACompressorOutputStream.builder().setPath(outPath).get()) {
            out.writeUtf8("Hello World!");
        }
    }
}
