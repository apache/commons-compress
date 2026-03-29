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
package org.apache.commons.compress.archivers.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.airlift.compress.zstd.ZstdOutputStream;

/**
 * Regression and feature tests for {@link ZipArchiveOutputStream#setCompressionPayloadWriterFactory}, buffering of payload-compressed entries on non-seekable
 * outputs (local file header with CRC/sizes instead of a data descriptor), and {@link ZipArchiveOutputStream#finish()}/{@link ZipArchiveOutputStream#close()}
 * behavior.
 */
class ZipArchiveOutputStreamPayloadWriterTest {

    @TempDir
    Path tempDir;

    private static final int ZSTD_LEVEL = 3;

    /** Offsets of fields in the local file header (fixed prefix up to file name length). */
    private static final int LFH_GPB_OFFSET = 6;

    private static final int LFH_CRC_OFFSET = 14;
    private static final int LFH_COMPRESSED_SIZE_OFFSET = 18;
    private static final int LFH_UNCOMPRESSED_SIZE_OFFSET = 22;

    private static final int GPB_DATA_DESCRIPTOR_FLAG = 1 << 3;

    /**
     * Airlift {@link ZstdOutputStream} as entry payload compressor; frames must remain decodable by {@link ZstdCompressorInputStream}.
     */
    private static final ZipCompressionPayloadWriterFactory AIRLIFT_ZSTD_PAYLOAD_WRITER_FACTORY =
            (compressedPayloadSink, entry) -> {
                final ZstdOutputStream zOut = new ZstdOutputStream(nonClosingPayloadSink(compressedPayloadSink));
                return new ZipCompressionPayloadWriter() {
                    @Override
                    public void write(final byte[] b, final int off, final int len) throws IOException {
                        if (len > 0) {
                            zOut.write(b, off, len);
                        }
                    }

                    @Override
                    public void finish() throws IOException {
                        zOut.close();
                    }
                };
            };

    private static OutputStream nonClosingPayloadSink(final OutputStream delegate) {
        return new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void close() {
                // Never close the ZIP stream: only airlift ZstdOutputStream.finish/close ends the frame.
            }
        };
    }

    /**
     * On seekable {@link ZipArchiveOutputStream}, CRC and sizes are written into the local file header when the entry is closed; they must match the central
     * directory. The data-descriptor flag must not be set in the LFH.
     */
    private static void assertLocalHeaderCrcAndSizesMatchCentralDirectory(final byte[] zip, final ZipArchiveEntry entry) {
        final int localOffset = Math.toIntExact(entry.getLocalHeaderOffset());
        assertTrue(localOffset >= 0 && localOffset + LFH_UNCOMPRESSED_SIZE_OFFSET + 4 <= zip.length);
        assertEquals(ZipLong.LFH_SIG.getValue(), ZipLong.getValue(zip, localOffset));
        final int gpb = ZipShort.getValue(zip, localOffset + LFH_GPB_OFFSET);
        assertEquals(0, gpb & GPB_DATA_DESCRIPTOR_FLAG, "LFH must not use data descriptor when writing to a seekable destination");
        assertFalse(entry.getGeneralPurposeBit().usesDataDescriptor(), "central directory: no data descriptor on seekable output");
        assertEquals(entry.getCrc(), ZipLong.getValue(zip, localOffset + LFH_CRC_OFFSET), "LFH CRC must match CD");
        assertEquals(entry.getCompressedSize(), ZipLong.getValue(zip, localOffset + LFH_COMPRESSED_SIZE_OFFSET), "LFH compressed size must match CD");
        assertEquals(entry.getSize(), ZipLong.getValue(zip, localOffset + LFH_UNCOMPRESSED_SIZE_OFFSET), "LFH uncompressed size must match CD");
    }

    private static ZipCompressionPayloadWriterFactory factoryAssertingEntry(
            final ZipCompressionPayloadWriterFactory delegate,
            final int expectedMethod,
            final String expectedName) {
        return (sink, entry) -> {
            assertEquals(expectedMethod, entry.getMethod(), "ZIP method passed to payload writer factory");
            assertEquals(expectedName, entry.getName(), "entry name passed to payload writer factory");
            return delegate.create(sink, entry);
        };
    }

    private static void compressZstd(final InputStream input, final OutputStream output) throws IOException {
        @SuppressWarnings("resource")
        final ZstdCompressorOutputStream zOut = new ZstdCompressorOutputStream(output, ZSTD_LEVEL, true);
        IOUtils.copyLarge(input, zOut);
        zOut.flush();
    }

    /** DEFLATED without closing the entry: {@link ZipArchiveOutputStream#finish()} must still fail (unchanged contract). */
    @Test
    void testDeflatedFinishWithOpenEntryThrows() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw);
        try {
            final ZipArchiveEntry ze = new ZipArchiveEntry("open.txt");
            ze.setMethod(ZipArchiveOutputStream.DEFLATED);
            zos.putArchiveEntry(ze);
            zos.write('x');
            final ArchiveException ex = assertThrows(ArchiveException.class, zos::finish);
            assertTrue(ex.getMessage().contains("unclosed"));
        } finally {
            zos.destroy();
        }
    }

    /** DEFLATED: implicit {@link ZipArchiveOutputStream#close()} still requires an explicit {@link ZipArchiveOutputStream#closeArchiveEntry()}. */
    @Test
    void testDeflatedTryWithResourcesWithOpenEntryThrows() {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        assertThrows(ArchiveException.class, () -> {
            try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
                final ZipArchiveEntry ze = new ZipArchiveEntry("open.txt");
                ze.setMethod(ZipArchiveOutputStream.DEFLATED);
                zos.putArchiveEntry(ze);
                zos.write("data".getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    /**
     * Legacy ZSTD path: no payload factory, pre-compressed bytes on a non-seekable stream — payload is buffered so local header carries CRC and sizes (no data
     * descriptor).
     */
    @ParameterizedTest
    @EnumSource(names = { "ZSTD", "ZSTD_DEPRECATED" })
    void testZstdPassthroughOnByteArrayOutputStreamStillWorks(final ZipMethod zipMethod) throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final String name = "legacy.txt";
        final byte[] plain = "legacy passthrough zstd".getBytes(StandardCharsets.UTF_8);
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            final ZipArchiveEntry ze = new ZipArchiveEntry(name);
            ze.setMethod(zipMethod.getCode());
            ze.setSize(plain.length);
            zos.putArchiveEntry(ze);
            compressZstd(new ByteArrayInputStream(plain), zos);
            zos.closeArchiveEntry();
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry(name);
            assertEquals(zipMethod.getCode(), e.getMethod());
            assertFalse(e.getGeneralPurposeBit().usesDataDescriptor());
            assertEquals(plain.length, e.getSize());
            try (InputStream in = zf.getInputStream(e)) {
                assertTrue(in instanceof ZstdCompressorInputStream);
                assertEquals(new String(plain, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Two ZSTD payload entries without explicit {@link ZipArchiveOutputStream#closeArchiveEntry()} for the first: the second {@link
     * ZipArchiveOutputStream#putArchiveEntry} closes the previous entry (existing zip behaviour, independent of {@link ZipCompressionPayloadWriterFactory}). The
     * last entry is closed by stream {@link ZipArchiveOutputStream#close()}.
     */
    @ParameterizedTest
    @EnumSource(names = { "ZSTD", "ZSTD_DEPRECATED" })
    void testZstdPayloadWriterTwoEntries(final ZipMethod zipMethod) throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final byte[] a = "entry-a".getBytes(StandardCharsets.UTF_8);
        final byte[] b = "entry-b-longer".getBytes(StandardCharsets.UTF_8);
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(zipMethod.getCode(), ZipCompressionPayloadWriters.zstd(ZSTD_LEVEL));
            final ZipArchiveEntry e1 = new ZipArchiveEntry("a.txt");
            e1.setMethod(zipMethod.getCode());
            zos.putArchiveEntry(e1);
            zos.write(a);

            final ZipArchiveEntry e2 = new ZipArchiveEntry("b.txt");
            e2.setMethod(zipMethod.getCode());
            zos.putArchiveEntry(e2);
            zos.write(b);
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry ra = zf.getEntry("a.txt");
            final ZipArchiveEntry rb = zf.getEntry("b.txt");
            assertEquals(a.length, ra.getSize());
            assertEquals(b.length, rb.getSize());
            try (InputStream in = zf.getInputStream(ra)) {
                assertEquals("entry-a", new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
            try (InputStream in = zf.getInputStream(rb)) {
                assertEquals("entry-b-longer", new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }

    /** Removing the factory allows the next entry to use external ZSTD compression again. */
    @ParameterizedTest
    @ValueSource(ints = { 93 /* ZipMethod.ZSTD */, 20 /* ZipMethod.ZSTD_DEPRECATED */ })
    void testClearPayloadWriterFactoryRestoresExternalCompression(final int zipMethodCode) throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final String payloadName = "wrapped.txt";
        final byte[] plain = "external zstd after clear".getBytes(StandardCharsets.UTF_8);

        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(zipMethodCode, ZipCompressionPayloadWriters.zstd(ZSTD_LEVEL));
            final ZipArchiveEntry e1 = new ZipArchiveEntry("inner.txt");
            e1.setMethod(zipMethodCode);
            zos.putArchiveEntry(e1);
            zos.write("inner".getBytes(StandardCharsets.UTF_8));
            zos.closeArchiveEntry();

            zos.setCompressionPayloadWriterFactory(zipMethodCode, null);

            final ZipArchiveEntry e2 = new ZipArchiveEntry(payloadName);
            e2.setMethod(zipMethodCode);
            e2.setSize(plain.length);
            zos.putArchiveEntry(e2);
            compressZstd(new ByteArrayInputStream(plain), zos);
            zos.closeArchiveEntry();
        }

        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry(payloadName);
            assertEquals(zipMethodCode, e.getMethod());
            assertEquals(plain.length, e.getSize());
            try (InputStream in = zf.getInputStream(e)) {
                assertEquals(new String(plain, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * {@link ZipCompressionPayloadWriter} backed by Airlift {@link ZstdOutputStream}; default {@link ZipFile} decoding uses zstd-jni — verifies bitstream
     * compatibility for ZIP entries.
     */
    @ParameterizedTest
    @EnumSource(names = { "ZSTD", "ZSTD_DEPRECATED" })
    void testZstdPayloadWriterAirliftCompressorRoundtrip(final ZipMethod zipMethod) throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final String entryName = "airlift.txt";
        final byte[] plain = "Airlift aircompressor ZstdOutputStream in ZIP.".getBytes(StandardCharsets.UTF_8);

        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(rawOut)) {
            zos.setCompressionPayloadWriterFactory(zipMethod.getCode(), AIRLIFT_ZSTD_PAYLOAD_WRITER_FACTORY);
            final ZipArchiveEntry ze = new ZipArchiveEntry(entryName);
            ze.setMethod(zipMethod.getCode());
            zos.putArchiveEntry(ze);
            zos.write(plain);
        }

        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(rawOut.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry(entryName);
            assertEquals(zipMethod.getCode(), e.getMethod());
            assertEquals(plain.length, e.getSize());
            try (InputStream in = zf.getInputStream(e)) {
                assertTrue(in instanceof ZstdCompressorInputStream, "ZipFile decodes ZSTD with zstd-jni");
                assertEquals(new String(plain, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }

    /** Empty entry body with payload writer: {@link ZipArchiveOutputStream#close()} must still produce a valid archive. */
    @Test
    void testZstdPayloadWriterEmptyEntry() throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(ZipMethod.ZSTD.getCode(), ZipCompressionPayloadWriters.zstd(ZSTD_LEVEL));
            final ZipArchiveEntry ze = new ZipArchiveEntry("empty.txt");
            ze.setMethod(ZipMethod.ZSTD.getCode());
            zos.putArchiveEntry(ze);
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry("empty.txt");
            assertEquals(0L, e.getSize());
            try (InputStream in = zf.getInputStream(e)) {
                assertEquals(0, IOUtils.toByteArray(in).length);
            }
        }
    }

    /**
     * BZip2 via payload writer on non-seekable output: compressed payload is buffered so the local header and central directory carry CRC and sizes (no data
     * descriptor).
     */
    @Test
    void testBzip2PayloadWriterNonSeekableRoundtrip() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final byte[] plain = "bzip2 payload writer unknown size".getBytes(StandardCharsets.UTF_8);
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(ZipMethod.BZIP2.getCode(), ZipCompressionPayloadWriters.bzip2());
            final ZipArchiveEntry ze = new ZipArchiveEntry("b2.txt");
            ze.setMethod(ZipMethod.BZIP2.getCode());
            zos.putArchiveEntry(ze);
            zos.write(plain);
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry("b2.txt");
            assertEquals(ZipMethod.BZIP2.getCode(), e.getMethod());
            assertEquals(plain.length, e.getSize());
            assertFalse(e.getGeneralPurposeBit().usesDataDescriptor());
            assertTrue(e.getCompressedSize() > 0);
            try (InputStream in = zf.getInputStream(e)) {
                assertTrue(in instanceof BZip2CompressorInputStream);
                assertEquals(new String(plain, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Seekable ZIP ({@link Path}): local file header CRC and sizes match the central directory for DEFLATED, BZip2 payload writer, and Zstandard payload writer
     * (no Zip64, no data descriptor in LFH).
     */
    @Test
    void testSeekablePathLocalHeaderMatchesCentralDirectory() throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final Path zip = tempDir.resolve("seekable-lfh-cd.zip");
        try (ZipArchiveOutputStream zos = ZipArchiveOutputStream.builder(zip)
                .setCompressionPayloadWriterFactory(ZipMethod.BZIP2.getCode(), ZipCompressionPayloadWriters.bzip2())
                .setCompressionPayloadWriterFactory(ZipMethod.ZSTD.getCode(), ZipCompressionPayloadWriters.zstd(ZSTD_LEVEL))
                .build()) {
            assertTrue(zos.isSeekable());

            final ZipArchiveEntry eDeflate = new ZipArchiveEntry("deflated.txt");
            eDeflate.setMethod(ZipArchiveOutputStream.DEFLATED);
            zos.putArchiveEntry(eDeflate);
            zos.write("seekable deflate payload".getBytes(StandardCharsets.UTF_8));
            zos.closeArchiveEntry();

            final ZipArchiveEntry eBz = new ZipArchiveEntry("bzip2.txt");
            eBz.setMethod(ZipMethod.BZIP2.getCode());
            zos.putArchiveEntry(eBz);
            zos.write("seekable bzip2 payload".getBytes(StandardCharsets.UTF_8));
            zos.closeArchiveEntry();

            final ZipArchiveEntry eZst = new ZipArchiveEntry("zstd.txt");
            eZst.setMethod(ZipMethod.ZSTD.getCode());
            zos.putArchiveEntry(eZst);
            zos.write("seekable zstd payload".getBytes(StandardCharsets.UTF_8));
            zos.closeArchiveEntry();
        }
        final byte[] zipBytes = Files.readAllBytes(zip);
        try (ZipFile zf = ZipFile.builder().setPath(zip).get()) {
            for (final String name : new String[] { "deflated.txt", "bzip2.txt", "zstd.txt" }) {
                final ZipArchiveEntry e = zf.getEntry(name);
                assertNotNull(e);
                assertLocalHeaderCrcAndSizesMatchCentralDirectory(zipBytes, e);
            }
        }
    }

    /**
     * Same LFH/CD consistency as {@link #testSeekablePathLocalHeaderMatchesCentralDirectory()} for {@link ZipArchiveOutputStream#ZipArchiveOutputStream(
     * java.nio.channels.SeekableByteChannel)}.
     */
    @Test
    void testSeekableChannelLocalHeaderMatchesCentralDirectoryPayloadWriter() throws IOException {
        final SeekableInMemoryByteChannel ch = new SeekableInMemoryByteChannel();
        final ZipArchiveOutputStream zos = ZipArchiveOutputStream.builder(ch)
                .setCompressionPayloadWriterFactory(ZipMethod.BZIP2.getCode(), ZipCompressionPayloadWriters.bzip2())
                .build();
        try {
            assertTrue(zos.isSeekable());
            final ZipArchiveEntry ze = new ZipArchiveEntry("only-bzip2.txt");
            ze.setMethod(ZipMethod.BZIP2.getCode());
            zos.putArchiveEntry(ze);
            zos.write("in-memory seekable channel".getBytes(StandardCharsets.UTF_8));
            zos.closeArchiveEntry();
            zos.finish();
            final byte[] zipBytes = Arrays.copyOf(ch.array(), Math.toIntExact(ch.size()));
            try (SeekableByteChannel zch = new SeekableInMemoryByteChannel(zipBytes); ZipFile zf = ZipFile.builder().setSeekableByteChannel(zch).get()) {
                final ZipArchiveEntry e = zf.getEntry("only-bzip2.txt");
                assertNotNull(e);
                assertEquals(ZipMethod.BZIP2.getCode(), e.getMethod());
                assertLocalHeaderCrcAndSizesMatchCentralDirectory(zipBytes, e);
                try (InputStream in = zf.getInputStream(e)) {
                    assertTrue(in instanceof BZip2CompressorInputStream);
                }
            }
        } finally {
            zos.close();
        }
    }

    /**
     * One archive, two entries — two different {@link ZipCompressionPayloadWriterFactory} registrations: {@link ZipCompressionPayloadWriters#zstd(int)} and
     * {@link ZipCompressionPayloadWriters#bzip2()}. Each wrapper asserts {@link ZipArchiveOutputStream} passes the correct {@link ZipArchiveEntry} to {@code
     * create}. Round-trip via {@link ZipFile} and sequential {@link ZipArchiveInputStream#getNextZipEntry()} (non-seekable ZIP: payload buffered, local headers
     * hold CRC and sizes).
     */
    @Test
    void testRoundtripTwoDifferentPayloadWriterFactories() throws IOException {
        Assumptions.assumeTrue(ZstdUtils.isZstdCompressionAvailable());
        final String nameZstd = "a/zstd.txt";
        final String nameBzip2 = "b/bzip2.txt";
        final byte[] plainZstd = "first entry zstd payload".getBytes(StandardCharsets.UTF_8);
        final byte[] plainBzip2 = "second entry bzip2 payload".getBytes(StandardCharsets.UTF_8);

        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(ZipMethod.ZSTD.getCode(),
                    factoryAssertingEntry(ZipCompressionPayloadWriters.zstd(ZSTD_LEVEL), ZipMethod.ZSTD.getCode(), nameZstd));
            zos.setCompressionPayloadWriterFactory(ZipMethod.BZIP2.getCode(),
                    factoryAssertingEntry(ZipCompressionPayloadWriters.bzip2(), ZipMethod.BZIP2.getCode(), nameBzip2));

            final ZipArchiveEntry e1 = new ZipArchiveEntry(nameZstd);
            e1.setMethod(ZipMethod.ZSTD.getCode());
            zos.putArchiveEntry(e1);
            zos.write(plainZstd);
            zos.closeArchiveEntry();

            final ZipArchiveEntry e2 = new ZipArchiveEntry(nameBzip2);
            e2.setMethod(ZipMethod.BZIP2.getCode());
            zos.putArchiveEntry(e2);
            zos.write(plainBzip2);
            zos.closeArchiveEntry();
        }

        final byte[] zipBytes = raw.toByteArray();

        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(zipBytes);
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry zst = zf.getEntry(nameZstd);
            assertNotNull(zst);
            assertEquals(ZipMethod.ZSTD.getCode(), zst.getMethod());
            assertEquals(plainZstd.length, zst.getSize());
            try (InputStream in = zf.getInputStream(zst)) {
                assertTrue(in instanceof ZstdCompressorInputStream);
                assertEquals(new String(plainZstd, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
            final ZipArchiveEntry bz = zf.getEntry(nameBzip2);
            assertNotNull(bz);
            assertEquals(ZipMethod.BZIP2.getCode(), bz.getMethod());
            assertEquals(plainBzip2.length, bz.getSize());
            try (InputStream in = zf.getInputStream(bz)) {
                assertTrue(in instanceof BZip2CompressorInputStream);
                assertEquals(new String(plainBzip2, StandardCharsets.UTF_8), new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        }

        try (ZipArchiveInputStream zin = new ZipArchiveInputStream(new ByteArrayInputStream(zipBytes))) {
            final ZipArchiveEntry eFirst = zin.getNextZipEntry();
            assertNotNull(eFirst);
            assertEquals(nameZstd, eFirst.getName());
            assertEquals(ZipMethod.ZSTD.getCode(), eFirst.getMethod());
            assertTrue(zin.canReadEntryData(eFirst), "Zstd entry must be readable");
            final byte[] readZstd = IOUtils.toByteArray(zin);
            assertEquals(plainZstd.length, readZstd.length);
            assertEquals(new String(plainZstd, StandardCharsets.UTF_8), new String(readZstd, StandardCharsets.UTF_8));

            final ZipArchiveEntry eSecond = zin.getNextZipEntry();
            assertNotNull(eSecond);
            assertEquals(nameBzip2, eSecond.getName());
            assertEquals(ZipMethod.BZIP2.getCode(), eSecond.getMethod());
            assertTrue(zin.canReadEntryData(eSecond), "BZIP2 entry must be readable");
            final byte[] readBzip2 = IOUtils.toByteArray(zin);
            assertEquals(plainBzip2.length, readBzip2.length);
            assertEquals(new String(plainBzip2, StandardCharsets.UTF_8), new String(readBzip2, StandardCharsets.UTF_8));

            assertNull(zin.getNextZipEntry());
        }
    }

    /**
     * Non-enum method code with a registered factory: buffered payload, local header and CD carry CRC and sizes from plaintext; {@link ZipFile} cannot decode the
     * method-specific payload.
     */
    @Test
    void testCustomMethodCodePayloadWriterWritesLocalMetadataAndBody() throws IOException {
        final int customMethod = 254;
        final ZipCompressionPayloadWriterFactory identity = (sink, ze) -> new ZipCompressionPayloadWriter() {
            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                sink.write(b, off, len);
            }

            @Override
            public void finish() {
                // identity "compression" — no trailer
            }
        };
        final byte[] plain = "future-proof custom zip method code".getBytes(StandardCharsets.UTF_8);
        final CRC32 expectCrc = new CRC32();
        expectCrc.update(plain);
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(raw)) {
            zos.setCompressionPayloadWriterFactory(customMethod, identity);
            final ZipArchiveEntry ze = new ZipArchiveEntry("custom.txt");
            ze.setMethod(customMethod);
            zos.putArchiveEntry(ze);
            zos.write(plain);
        }
        try (SeekableByteChannel ch = new SeekableInMemoryByteChannel(raw.toByteArray());
                ZipFile zf = ZipFile.builder().setSeekableByteChannel(ch).get()) {
            final ZipArchiveEntry e = zf.getEntry("custom.txt");
            assertEquals(customMethod, e.getMethod());
            assertEquals(plain.length, e.getSize());
            assertFalse(e.getGeneralPurposeBit().usesDataDescriptor());
            assertEquals(expectCrc.getValue(), e.getCrc());
            assertEquals(plain.length, e.getCompressedSize());
            assertThrows(UnsupportedZipFeatureException.class, () -> zf.getInputStream(e));
        }
    }
}
