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

package org.apache.commons.compress.compressors.xz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Tests for class {@link XZCompressorOutputStream} and {@link XZCompressorInputStream}.
 *
 * @see XZCompressorOutputStream
 * @see XZCompressorInputStream
 */
public class XZCompressorRoundtripTest {

    @TempDir
    static Path tempDir;

    private void roundtrip(final Path outPath, final LZMA2Options options, final boolean decompressConcatenated, final int memoryLimitKiB) throws IOException {
        final String data = "Hello World!";
        // @formatter:off
        try (XZCompressorOutputStream out = XZCompressorOutputStream.builder()
                .setPath(outPath)
                .setLzma2Options(options)
                .get()) {
            out.writeUtf8(data);
        }
        try (XZCompressorInputStream out = XZCompressorInputStream.builder()
                .setPath(outPath)
                .setDecompressConcatenated(decompressConcatenated)
                .setMemoryLimitKiB(memoryLimitKiB)
                .get()) {
            assertEquals(data, IOUtils.toString(out, StandardCharsets.UTF_8));
        }
        // @formatter:on
        try (XZCompressorInputStream out = new XZCompressorInputStream(Files.newInputStream(outPath))) {
            assertEquals(data, IOUtils.toString(out, StandardCharsets.UTF_8));
        }
        // deprecated
        try (XZCompressorOutputStream out = new XZCompressorOutputStream(new XZOutputStream(Files.newOutputStream(outPath), options))) {
            out.writeUtf8(data);
        }
    }

    @CartesianTest
    public void testBuilderOptions(@Values(ints = { LZMA2Options.PRESET_MAX, LZMA2Options.PRESET_MIN, LZMA2Options.PRESET_DEFAULT }) final int preset,
            @Values(booleans = { false, true }) final boolean decompressConcatenated, @Values(ints = { -1, 100_000 }) final int memoryLimitKiB)
            throws IOException {
        roundtrip(tempDir.resolve("out.xz"), new LZMA2Options(preset), false, -1);
    }

    @Test
    public void testBuilderOptionsAll() throws IOException {
        final int dictSize = LZMA2Options.DICT_SIZE_MIN;
        final int lc = LZMA2Options.LC_LP_MAX - 4;
        final int lp = LZMA2Options.LC_LP_MAX - 4;
        final int pb = LZMA2Options.PB_MAX;
        final int mode = LZMA2Options.MODE_NORMAL;
        final int niceLen = LZMA2Options.NICE_LEN_MIN;
        final int mf = LZMA2Options.MF_BT4;
        final int depthLimit = 50;
        roundtrip(tempDir.resolve("out.xz"), new LZMA2Options(dictSize, lc, lp, pb, mode, niceLen, mf, depthLimit), false, -1);
    }

    @CartesianTest
    public void testBuilderOptionsDefgault(@Values(booleans = { false, true }) final boolean decompressConcatenated,
            @Values(ints = { -1, 100_000 }) final int memoryLimitKiB) throws IOException {
        roundtrip(tempDir.resolve("out.xz"), new LZMA2Options(), decompressConcatenated, memoryLimitKiB);
    }

    @Test
    public void testBuilderPath() throws IOException {
        // This test does not use LZMA2Options
        final String data = "Hello World!";
        final Path outPath = tempDir.resolve("out.xz");
        try (XZCompressorOutputStream out = XZCompressorOutputStream.builder().setPath(outPath).get()) {
            out.writeUtf8(data);
        }
        // deprecated
        try (XZCompressorInputStream out = new XZCompressorInputStream(Files.newInputStream(outPath), false)) {
            assertEquals(data, IOUtils.toString(out, StandardCharsets.UTF_8));
        }
        // deprecated
        try (XZCompressorInputStream out = new XZCompressorInputStream(Files.newInputStream(outPath), false, -1)) {
            assertEquals(data, IOUtils.toString(out, StandardCharsets.UTF_8));
        }
    }
}
