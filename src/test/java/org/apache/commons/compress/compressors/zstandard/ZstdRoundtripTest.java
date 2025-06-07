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

package org.apache.commons.compress.compressors.zstandard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOFunction;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import com.github.luben.zstd.ZstdOutputStream;

/**
 * Tests {@link ZstdCompressorOutputStream}.
 */
public class ZstdRoundtripTest extends AbstractTest {

    private interface OutputStreamCreator extends IOFunction<FileOutputStream, ZstdCompressorOutputStream> {
        // empty
    }

    private void roundtrip(final OutputStreamCreator oc) throws IOException {
        final Path input = getPath("bla.tar");
        final File output = newTempFile(input.getFileName() + ".zstd");
        try (FileOutputStream os = new FileOutputStream(output);
                ZstdCompressorOutputStream zos = oc.apply(os)) {
            zos.write(input);
            zos.close();
            assertTrue(zos.isClosed());
        }
        try (ZstdCompressorInputStream zis = new ZstdCompressorInputStream(Files.newInputStream(output.toPath()))) {
            final byte[] expected = Files.readAllBytes(input);
            final byte[] actual = IOUtils.toByteArray(zis);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void testDirectRoundtrip() throws Exception {
        roundtrip(ZstdCompressorOutputStream::new);
    }

    @Test
    void testFactoryRoundtrip() throws Exception {
        final Path input = getPath("bla.tar");
        final File output = newTempFile(input.getFileName() + ".zstd");
        try (OutputStream os = Files.newOutputStream(output.toPath());
                CompressorOutputStream<?> zos = new CompressorStreamFactory().createCompressorOutputStream("zstd", os)) {
            zos.write(input);
        }
        try (InputStream inputStream = Files.newInputStream(output.toPath());
                CompressorInputStream zis = new CompressorStreamFactory().createCompressorInputStream("zstd", inputStream)) {
            final byte[] expected = Files.readAllBytes(input);
            final byte[] actual = IOUtils.toByteArray(zis);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    void testRoundtripSetChainLogNonDefaultMax() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setChainLog(ZstdConstants.ZSTD_CHAINLOG_MAX).get());
    }

    @Test
    void testRoundtripSetChainLogNonDefaultMin() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setChainLog(ZstdConstants.ZSTD_CHAINLOG_MIN).get());
    }

    @Test
    void testRoundtripSetChecksumNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setChecksum(true).get());
    }

    @Test
    void testRoundtripSetCloseFrameOnFlushNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setCloseFrameOnFlush(true).get());
    }

    @Test
    void testRoundtripSetHashLogNonDefaultMax() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setHashLog(ZstdConstants.ZSTD_HASHLOG_MAX).get());
    }

    @Test
    void testRoundtripSetHashLogNonDefaultMin() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setHashLog(ZstdConstants.ZSTD_HASHLOG_MIN).get());
    }

    @Test
    void testRoundtripSetJobSizeNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setJobSize(1).get());
    }

    @Test
    void testRoundtripSetLevelNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setLevel(1).get());
    }

    @Test
    void testRoundtripSetMinMatchNonDefaultMax() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setMinMatch(ZstdConstants.ZSTD_MINMATCH_MAX).get());
    }

    @Test
    void testRoundtripSetMinMatchNonDefaultMin() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setMinMatch(ZstdConstants.ZSTD_MINMATCH_MIN).get());
    }

    @Test
    void testRoundtripSetOverlapLogNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setOverlapLog(1).get());
    }

    @Test
    void testRoundtripSetSearchLogNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setSearchLog(1).get());
    }

    @Test
    void testRoundtripSetStrategyNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setStrategy(1).get());
    }

    @Test
    @Disabled("com.github.luben.zstd.ZstdIOException: Frame requires too much memory for decoding")
    void testRoundtripSetWindowLogNonDefaultMax() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setWindowLog(ZstdConstants.ZSTD_WINDOWLOG_MAX).get());
    }

    @Test
    void testRoundtripSetWindowLogNonDefaultMin() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setWindowLog(ZstdConstants.ZSTD_WINDOWLOG_MIN).get());
    }

    @Test
    void testRoundtripSetWorkersNonDefault() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setWorkers(1).get());
    }

    @Test
    void testRoundtripSetZstdDict() throws Exception {
        // Avoid JVM segmentation fault in zstd-jni 1.5.7-2
        // TODO Remove ternary expression in the ctor if/when https://github.com/luben/zstd-jni/pull/356 is fixed.
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setDict(null).get());
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setDict(ArrayUtils.EMPTY_BYTE_ARRAY).get());
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setDict(new byte[512]).get());
    }

    @CartesianTest
    // @formatter:off
    void testRoundtripWithAll(
            @Values(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }) final int level, // see zstd.h
            @Values(booleans = { false, true }) final boolean checksum,
            @Values(booleans = { false, true }) final boolean closeFrameOnFlush,
            @Values(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }) final int strategy, // see zstd.h
            @Values(ints = { 0, 6, 9 }) final int overlapLog  // see zstd.h
        ) throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder()
                .setChainLog(0)
                .setChecksum(checksum)
                .setCloseFrameOnFlush(closeFrameOnFlush)
                .setDict(null)
                .setHashLog(0)
                .setJobSize(0)
                .setLevel(level)
                .setMinMatch(0)
                .setOutputStream(os)
                .setOverlapLog(overlapLog)
                .setSearchLog(0)
                .setStrategy(strategy)
                .setWindowLog(0)
                .setWorkers(0)
                .get());
    }
    // @formatter:on

    @Test
    void testRoundtripWithChecksum() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 3, false, true));
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setLevel(3).setCloseFrameOnFlush(true).setChecksum(true).get());
    }

    @Test
    void testRoundtripWithCloseFrameOnFlush() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 3, true));
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setLevel(3).setCloseFrameOnFlush(true).get());
    }

    @Test
    void testRoundtripWithCustomLevel() throws Exception {
        roundtrip(os -> new ZstdCompressorOutputStream(os, 1));
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(os).setLevel(1).get());
    }

    @Test
    void testRoundtripWithZstdOutputStream() throws Exception {
        roundtrip(os -> ZstdCompressorOutputStream.builder().setOutputStream(new ZstdOutputStream(os)).get());
        roundtrip(os -> new ZstdCompressorOutputStream(new ZstdOutputStream(os)));
    }
}
