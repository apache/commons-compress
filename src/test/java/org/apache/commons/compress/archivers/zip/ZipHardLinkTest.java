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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.examples.Expander;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

class ZipHardLinkTest {

    private static final byte[] CONTENT = "hard link contents".getBytes(UTF_8);
    private static final byte[] EMPTY = {};
    private static final ZipShort UNIX = new ZipShort(0x000d);

    @TempDir
    private Path temp;

    /**
     * Writes a fixture with empty reference bodies and a shared regular-file payload.
     */
    private Path archive(final ZipArchiveEntry... entries) throws IOException {
        final Path path = Files.createTempFile(temp, "links", ".zip");
        // Fixture references and directories carry no body; regular entries share CONTENT.
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(path)) {
            for (final ZipArchiveEntry entry : entries) {
                write(out, entry, entry.isUnixHardLink() || entry.getExtraField(UNIX) != null || entry.isDirectory() ? EMPTY : CONTENT);
            }
        }
        return path;
    }

    /**
     * Checks that an invalid reference fails reading and extraction before creating output.
     */
    private void assertInvalid(final Path path) throws IOException {
        try (ZipFile zip = ZipFile.builder().setPath(path).setResolveUnixHardLinks(true).get()) {
            // All resolved-read entry points must reject the same invalid reference.
            final ZipArchiveEntry entry = zip.getEntry("link");
            assertThrows(IOException.class, () -> zip.resolveUnixHardLink(entry));
            assertThrows(IOException.class, zip::resolveUnixHardLinks);
            assertThrows(IOException.class, () -> zip.getInputStream(entry));
            assertFalse(zip.canReadEntryData(entry));
            // Extraction must fail before writing, including when output is discarded.
            final Path output = temp.resolve("invalid-output");
            assertThrows(IOException.class, () -> new Expander().expand(zip, output));
            assertFalse(Files.exists(output));
            assertThrows(IOException.class, () -> new Expander().expand(zip, (Path) null));
        }
    }

    /**
     * Skips filesystem assertions when the temporary directory cannot support hard links.
     */
    private void assumeHardLinks() throws IOException {
        final Path target = Files.createTempFile(temp, "probe", ".txt");
        final Path link = temp.resolve("probe-link");
        // Probe the actual filesystem instead of assuming support from the operating system.
        try {
            Files.createLink(link, target);
            assertTrue(Files.isSameFile(target, link));
        } catch (final UnsupportedOperationException | IOException ex) {
            Assumptions.abort("File system cannot create hard links: " + ex);
        } finally {
            Files.deleteIfExists(link);
            Files.delete(target);
        }
    }

    /**
     * Creates a symlink control or skips the test when symlinks are unavailable.
     */
    private void createSymbolicLink(final Path path, final Path target) throws IOException {
        try {
            Files.createSymbolicLink(path, target);
        } catch (final UnsupportedOperationException | IOException ex) {
            Assumptions.abort("File system cannot create symbolic links: " + ex);
        }
    }

    /**
     * Builds a PKWARE UNIX payload with a zeroed fixed prefix and an unterminated pathname.
     */
    private byte[] extra(final String target) {
        final byte[] name = target.getBytes(UTF_8);
        final byte[] data = new byte[12 + name.length];
        System.arraycopy(name, 0, data, 12, name.length);
        return data;
    }

    /**
     * Creates a marked UNIX reference carrying its target in extra field {@code 0x000d}.
     */
    private ZipArchiveEntry link(final String name, final String target) {
        // The external attribute marker and PKWARE target payload are both required.
        final ZipArchiveEntry entry = regular(name);
        entry.setExternalAttributes(entry.getExternalAttributes() | 0x800L);
        final UnrecognizedExtraField field = new UnrecognizedExtraField();
        field.setHeaderId(UNIX);
        field.setLocalFileDataData(extra(target));
        entry.addExtraField(field);
        return entry;
    }

    /**
     * Creates a UNIX regular-file entry with ordinary read/write permissions.
     */
    private ZipArchiveEntry regular(final String name) {
        final ZipArchiveEntry entry = new ZipArchiveEntry(name);
        entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
        return entry;
    }

    /**
     * Reads an entry using the archive's configured hard link resolution policy.
     */
    private byte[] read(final ZipFile zip, final ZipArchiveEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return IOUtils.toByteArray(in);
        }
    }

    /**
     * Checks opt-in content resolution while stored metadata and raw copying remain unchanged.
     */
    @Test
    void testDefaultAndResolvedReads() throws Exception {
        final Path path = archive(link("link", "chain"), link("chain", "target"), regular("target"));
        // Default reads expose the stored empty body, but explicit resolution is available.
        try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zip.getEntry("link");
            assertTrue(entry.isUnixHardLink());
            assertEquals("chain", zip.getUnixHardLink(entry));
            assertSame(zip.getEntry("target"), zip.resolveUnixHardLink(entry));
            assertSame(zip.getEntry("target"), zip.resolveUnixHardLink(zip.getEntry("target")));
            assertNull(zip.getUnixHardLink(zip.getEntry("target")));
            assertNull(zip.getUnixHardLink(null));
            assertArrayEquals(EMPTY, read(zip, entry));
            assertTrue(zip.canReadEntryData(entry));
            assertThrows(IOException.class, () -> zip.resolveUnixHardLink(new ZipArchiveEntry(entry)));
        }
        // Resolved reads follow the chain and reject entries owned by another archive.
        try (ZipFile zip = ZipFile.builder().setPath(path).setResolveUnixHardLinks(true).get();
                ZipFile other = ZipFile.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zip.getEntry("link");
            assertThrows(IOException.class, () -> zip.resolveUnixHardLink(other.getEntry("link")));
            assertArrayEquals(CONTENT, read(zip, entry));
            // Following content must not rewrite the reference's stored metadata.
            assertEquals(0, entry.getSize());
            assertEquals(0, entry.getCompressedSize());
            assertEquals(0, entry.getCrc());
            assertTrue(zip.canReadEntryData(entry));
            try (InputStream raw = zip.getRawInputStream(entry)) {
                assertArrayEquals(EMPTY, IOUtils.toByteArray(raw));
            }
            // Raw copying must preserve the original marker and opaque target payload.
            final Path copy = temp.resolve("raw-copy.zip");
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(copy)) {
                zip.copyRawEntries(out, e -> true);
            }
            try (ZipFile copied = ZipFile.builder().setPath(copy).get()) {
                assertArrayEquals(EMPTY, read(copied, copied.getEntry("link")));
                assertEquals(entry.getExternalAttributes(), copied.getEntry("link").getExternalAttributes());
                assertArrayEquals(entry.getExtraField(UNIX).getLocalFileDataData(), copied.getEntry("link").getExtraField(UNIX).getLocalFileDataData());
            }
        }
    }

    /**
     * Rejects missing targets, self-links, cycles and duplicate target names.
     */
    @Test
    void testCyclesMissingAndAmbiguousTargets() throws Exception {
        assertInvalid(archive(link("link", "missing")));
        assertInvalid(archive(link("link", "link")));
        assertInvalid(archive(link("link", "other"), link("other", "link")));
        assertInvalid(archive(link("link", "target"), regular("target"), regular("target")));
    }

    /**
     * Rejects directories, symlinks, devices, FIFOs and sockets as hard link targets.
     */
    @ParameterizedTest
    @ValueSource(ints = {UnixStat.DIR_FLAG, UnixStat.LINK_FLAG, 0020000, 0060000, 0010000, 0140000})
    void testNonRegularTargets(final int mode) throws Exception {
        final ZipArchiveEntry target = regular("target");
        target.setUnixMode(mode | 0644);
        assertInvalid(archive(link("link", "target"), target));
    }

    /**
     * Rejects absent, truncated, NUL-containing and conflicting target metadata.
     */
    @Test
    void testMalformedExtraFields() throws Exception {
        // Exercise invalid payloads before checking missing and conflicting header fields.
        for (final byte[] bytes : new byte[][] {EMPTY, new byte[11], new byte[12], extra("target\0"), extra("\0target")}) {
            final ZipArchiveEntry entry = link("link", "target");
            ((UnrecognizedExtraField) entry.getExtraField(UNIX)).setLocalFileDataData(bytes);
            assertInvalid(archive(entry, regular("target")));
        }
        // A marker alone is insufficient, and two different target names are ambiguous.
        final ZipArchiveEntry missing = link("link", "target");
        missing.removeExtraField(UNIX);
        assertInvalid(archive(missing, regular("target")));
        final ZipArchiveEntry conflict = link("link", "target");
        ((UnrecognizedExtraField) conflict.getExtraField(UNIX)).setCentralDirectoryData(extra("other"));
        assertInvalid(archive(conflict, regular("target"), regular("other")));
    }

    /**
     * Rejects references whose header does not describe an unencrypted empty stored entry.
     */
    @Test
    void testMalformedReferenceHeaders() throws Exception {
        final Path path = archive(link("link", "target"), regular("target"));
        try (ZipFile zip = ZipFile.builder().setPath(path).setResolveUnixHardLinks(true).get()) {
            final ZipArchiveEntry entry = zip.getEntry("link");
            // Change one header property at a time to isolate each validation rule.
            entry.setSize(1);
            assertThrows(IOException.class, () -> zip.getUnixHardLink(entry));
            entry.setSize(0);
            entry.setCompressedSize(1);
            assertThrows(IOException.class, () -> zip.getUnixHardLink(entry));
            entry.setCompressedSize(0);
            entry.setCrc(1);
            assertThrows(IOException.class, () -> zip.getUnixHardLink(entry));
            entry.setCrc(0);
            entry.setMethod(ZipMethod.DEFLATED.getCode());
            assertThrows(IOException.class, () -> zip.getUnixHardLink(entry));
            entry.setMethod(ZipMethod.STORED.getCode());
            entry.getGeneralPurposeBit().useEncryption(true);
            assertThrows(IOException.class, () -> zip.getUnixHardLink(entry));
        }
    }

    /**
     * Rejects unreadable terminal payloads before extraction creates any files.
     */
    @Test
    void testUnsupportedTarget() throws Exception {
        final Path path = archive(link("link", "target"), regular("target"));
        try (ZipFile zip = ZipFile.builder().setPath(path).setResolveUnixHardLinks(true).get()) {
            zip.getEntry("target").setMethod(99);
            assertFalse(zip.canReadEntryData(zip.getEntry("link")));
            assertThrows(IOException.class, () -> zip.getInputStream(zip.getEntry("link")));
            assertThrows(IOException.class, () -> new Expander().expand(zip, temp.resolve("unsupported")));
            assertFalse(Files.exists(temp.resolve("unsupported")));
        }
    }

    /**
     * Checks target availability with central-only and local-only UNIX fields.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testLocalAndCentralTargets(final boolean ignoreLocal) throws Exception {
        // Central-directory targets remain available when local headers are ignored.
        final ZipArchiveEntry centralOnly = link("link", "target");
        final UnrecognizedExtraField field = (UnrecognizedExtraField) centralOnly.getExtraField(UNIX);
        field.setCentralDirectoryData(extra("target"));
        field.setLocalFileDataData(EMPTY);
        final Path path = archive(centralOnly, regular("target"));
        try (ZipFile zip = ZipFile.builder().setPath(path).setIgnoreLocalFileHeader(ignoreLocal).setResolveUnixHardLinks(true).get()) {
            assertArrayEquals(CONTENT, read(zip, zip.getEntry("link")));
        }
        // A local-only target requires local header parsing to resolve the reference.
        final ZipArchiveEntry localOnly = link("link", "target");
        ((UnrecognizedExtraField) localOnly.getExtraField(UNIX)).setCentralDirectoryData(new byte[12]);
        final Path localPath = archive(localOnly, regular("target"));
        try (ZipFile zip = ZipFile.builder().setPath(localPath).setIgnoreLocalFileHeader(ignoreLocal).setResolveUnixHardLinks(true).get()) {
            if (ignoreLocal) {
                assertThrows(IOException.class, () -> zip.getInputStream(zip.getEntry("link")));
            } else {
                assertArrayEquals(CONTENT, read(zip, zip.getEntry("link")));
            }
        }
    }

    /**
     * Decodes target names using the entry's UTF-8 flag or the configured legacy charset.
     */
    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "IBM437"})
    void testTargetEncoding(final String encoding) throws Exception {
        // Encode the opaque target bytes explicitly; the writer only encodes entry names.
        final String target = "d/\u00e9.txt";
        final ZipArchiveEntry entry = link("link", target);
        final byte[] encoded = target.getBytes(Charset.forName(encoding));
        final byte[] bytes = new byte[12 + encoded.length];
        System.arraycopy(encoded, 0, bytes, 12, encoded.length);
        ((UnrecognizedExtraField) entry.getExtraField(UNIX)).setLocalFileDataData(bytes);
        final Path path = temp.resolve("encoded.zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(path)) {
            out.setEncoding(encoding);
            write(out, entry, EMPTY);
            write(out, regular(target), CONTENT);
        }
        // UTF-8 entry flags must override a different configured fallback charset.
        try (ZipFile zip = ZipFile.builder().setPath(path).setCharset(Charset.forName("IBM437")).setResolveUnixHardLinks(true).get()) {
            assertEquals(target, zip.getUnixHardLink(zip.getEntry("link")));
            assertArrayEquals(CONTENT, read(zip, zip.getEntry("link")));
        }
    }

    /**
     * Rejects malformed UTF-8 targets instead of linking to a replacement-character filename.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testMalformedTargetEncoding(final boolean utf8Flag) throws Exception {
        // Cover invalid bytes, truncated sequences, overlong encodings and encoded surrogates.
        for (final byte[] target : new byte[][] {{(byte) 0xff}, {(byte) 0xc3}, {(byte) 0xc0, (byte) 0xaf},
                {(byte) 0xed, (byte) 0xa0, (byte) 0x80}}) {
            final ZipArchiveEntry entry = link("link", "?");
            final byte[] data = new byte[12 + target.length];
            System.arraycopy(target, 0, data, 12, target.length);
            ((UnrecognizedExtraField) entry.getExtraField(UNIX)).setLocalFileDataData(data);
            final Path path = Files.createTempFile(temp, "malformed-target", ".zip");
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(path)) {
                out.setUseLanguageEncodingFlag(utf8Flag);
                write(out, entry, EMPTY);
                write(out, regular("?"), CONTENT);
            }
            // Both the UTF-8 flag and the default UTF-8 charset must use strict decoding.
            try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
                assertEquals(utf8Flag, zip.getEntry("link").getGeneralPurposeBit().usesUTF8ForNames());
                assertThrows(IOException.class, () -> zip.getUnixHardLink(zip.getEntry("link")));
            }
            assertInvalid(path);
        }
        // A literal question mark is a valid member name and must still resolve normally.
        try (ZipFile zip = ZipFile.builder().setPath(archive(link("link", "?"), regular("?"))).setResolveUnixHardLinks(true).get()) {
            assertArrayEquals(CONTENT, read(zip, zip.getEntry("link")));
        }
    }

    /**
     * Rejects target bytes that the configured legacy charset cannot map to a character.
     */
    @ParameterizedTest
    @ValueSource(strings = {"US-ASCII", "windows-1252"})
    void testUnmappableTargetEncoding(final String charset) throws Exception {
        final ZipArchiveEntry entry = link("link", "?");
        final byte[] data = extra("?");
        data[12] = (byte) 0x81;
        ((UnrecognizedExtraField) entry.getExtraField(UNIX)).setLocalFileDataData(data);
        final Path path = temp.resolve("unmappable.zip");
        // Clear the language flag so target decoding uses the selected legacy charset.
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(path)) {
            out.setUseLanguageEncodingFlag(false);
            write(out, entry, EMPTY);
            write(out, regular("?"), CONTENT);
        }
        try (ZipFile zip = ZipFile.builder().setPath(path).setCharset(Charset.forName(charset)).setResolveUnixHardLinks(true).get()) {
            assertThrows(IOException.class, () -> zip.getUnixHardLink(zip.getEntry("link")));
            assertThrows(IOException.class, zip::resolveUnixHardLinks);
            assertFalse(zip.canReadEntryData(zip.getEntry("link")));
            assertThrows(IOException.class, () -> new Expander().expand(zip, (Path) null));
        }
    }

    /**
     * Separates hard link markers from UTF-8 flags and host-dependent Windows attributes.
     */
    @Test
    void testFlagAndHostControls() throws Exception {
        // Only the legacy FAT case may become a hard link through the compatibility option.
        final ZipArchiveEntry absent = link("unflagged", "target");
        absent.setUnixMode(UnixStat.FILE_FLAG | 0644);
        final ZipArchiveEntry legacy = link("legacy", "target");
        legacy.setPlatform(ZipArchiveEntry.PLATFORM_FAT);
        final ZipArchiveEntry ntfs = link("ntfs", "target");
        ntfs.setPlatform(10);
        final Path path = archive(absent, legacy, ntfs, regular("target"));
        for (final boolean compatibility : new boolean[] {false, true}) {
            try (ZipFile zip = ZipFile.builder().setPath(path).setAllowLegacyUnixHardLinks(compatibility).setResolveUnixHardLinks(true).get()) {
                for (final String name : Arrays.asList("unflagged", "legacy", "ntfs")) {
                    final ZipArchiveEntry entry = zip.getEntry(name);
                    assertFalse(entry.isUnixHardLink());
                    assertTrue(entry.getGeneralPurposeBit().usesUTF8ForNames());
                    assertArrayEquals(compatibility && name.equals("legacy") ? CONTENT : EMPTY, read(zip, entry));
                }
            }
        }
        // Streaming input cannot see the central-directory external attribute marker.
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(Files.newInputStream(archive(link("link", "target"), regular("target"))))) {
            final ZipArchiveEntry entry = in.getNextEntry();
            assertFalse(entry.isUnixHardLink());
            assertEquals(0, entry.getExternalAttributes());
            assertArrayEquals(EMPTY, IOUtils.toByteArray(in));
        }
    }

    /**
     * Extracts streamed ZIP output in both orders and requires the external hard link marker.
     */
    @Test
    void testStreamingOutputAndUnflaggedExtraction() throws Exception {
        assumeHardLinks();
        // Vary the marker and entry order independently on a non-seekable writer.
        for (final boolean flag : new boolean[] {false, true}) {
            for (final boolean reverse : new boolean[] {false, true}) {
                final ZipArchiveEntry entry = link("link", "target");
                if (!flag) {
                    entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
                }
                final Path path = Files.createTempFile(temp, "streamed", ".zip");
                try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(Files.newOutputStream(path))) {
                    if (reverse) {
                        write(out, entry, EMPTY);
                    }
                    write(out, regular("target"), CONTENT);
                    if (!reverse) {
                        write(out, entry, EMPTY);
                    }
                }
                // A seekable reader can restore links from the completed central directory.
                final Path output = temp.resolve("streamed-" + flag + reverse);
                try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
                    new Expander().expand(zip, output);
                }
                assertEquals(flag, Files.isSameFile(output.resolve("target"), output.resolve("link")));
                assertArrayEquals(flag ? CONTENT : EMPTY, Files.readAllBytes(output.resolve("link")));
            }
        }
    }

    /**
     * Resolves historical central-only fixtures while streaming reads retain stored bodies.
     */
    @ParameterizedTest
    @ValueSource(strings = {"hlink-chain.zip", "hlink-before-target.zip"})
    void testHistoricalReaders(final String name) throws Exception {
        // These fixtures resolve entirely from the central directory.
        for (final boolean ignoreLocal : new boolean[] {false, true}) {
            try (ZipFile zip = ZipFile.builder().setURI(AbstractTest.getURI("zip-hardlinks/" + name)).setIgnoreLocalFileHeader(ignoreLocal)
                    .setResolveUnixHardLinks(true).get()) {
                for (final ZipArchiveEntry entry : zip.entries()) {
                    assertEquals(10, read(zip, entry).length);
                }
            }
        }
        // Local headers contain neither the target field nor the external attribute marker.
        try (ZipArchiveInputStream in = new ZipArchiveInputStream(AbstractTest.getURI("zip-hardlinks/" + name).toURL().openStream())) {
            ZipArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                assertNull(entry.getExtraField(UNIX));
                assertFalse(entry.isUnixHardLink());
                assertEquals(entry.getSize(), IOUtils.toByteArray(in).length);
            }
        }
    }

    /**
     * Restores historical chains and forward references as paths sharing one inode.
     */
    @ParameterizedTest
    @ValueSource(strings = {"hlink-chain.zip", "hlink-before-target.zip"})
    void testHistoricalExtraction(final String name) throws Exception {
        assumeHardLinks();
        final Path output = temp.resolve("historical");
        try (ZipFile zip = ZipFile.builder().setURI(AbstractTest.getURI("zip-hardlinks/" + name)).get()) {
            new Expander().expand(zip, output);
            // Every fixture member must share both the payload and the filesystem identity.
            Path first = null;
            for (final ZipArchiveEntry entry : zip.entries()) {
                final Path path = output.resolve(entry.getName());
                if (first == null) {
                    first = path;
                }
                assertTrue(Files.isSameFile(first, path));
                assertEquals(10, Files.size(path));
            }
        }
    }

    /**
     * Requires explicit FAT compatibility to restore the original PKWARE regular-file links.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPkwareRegularFiles(final boolean compatibility) throws Exception {
        // Keep device and symlink behavior out of the regular-file hard link test.
        final Path subset = temp.resolve("pkware-regular.zip");
        try (ZipFile zip = ZipFile.builder().setURI(AbstractTest.getURI("zip-hardlinks/pkware-specials.zip")).get();
                ZipArchiveOutputStream out = new ZipArchiveOutputStream(subset)) {
            zip.copyRawEntries(out, e -> Arrays.asList("regular", "z-hardlink1", "z-hardlink2").contains(e.getName()));
        }
        // Check both resolved reads and inode identity under the selected compatibility policy.
        try (ZipFile zip = ZipFile.builder().setPath(subset).setResolveUnixHardLinks(true).setAllowLegacyUnixHardLinks(compatibility).get()) {
            assertEquals(0, zip.getEntry("z-hardlink1").getPlatform());
            assertArrayEquals(compatibility ? read(zip, zip.getEntry("regular")) : EMPTY, read(zip, zip.getEntry("z-hardlink1")));
            if (compatibility) {
                assumeHardLinks();
            }
            final Path output = temp.resolve("pkware-output");
            new Expander().expand(zip, output);
            for (final String name : Arrays.asList("z-hardlink1", "z-hardlink2")) {
                assertEquals(compatibility, Files.isSameFile(output.resolve("regular"), output.resolve(name)));
                assertEquals(compatibility ? 32 : 0, Files.size(output.resolve(name)));
            }
        }
    }

    /**
     * Rejects escaping or conflicting output paths and targets absent from the archive.
     */
    @Test
    void testRejectEscapingAndConflictingPaths() throws Exception {
        final Path output = temp.resolve("conflicts");
        // Preflight must catch lexical path conflicts before creating the output directory.
        for (final String name : Arrays.asList("../escape", temp.resolve("absolute").toString(), "target", "a/../target", "target/child")) {
            final Path path = archive(regular("target"), link(name, "target"));
            try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
                assertThrows(IOException.class, () -> new Expander().expand(zip, output), name);
                assertFalse(Files.exists(output));
            }
        }
        // A target must be a member of the archive, never an unrelated file already in the output directory.
        Files.createDirectories(output);
        Files.write(output.resolve("target"), CONTENT);
        try (ZipFile zip = ZipFile.builder().setPath(archive(link("link", "target"))).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(zip, output));
            assertFalse(Files.exists(output.resolve("link")));
        }
    }

    /**
     * Rejects filesystem case aliases between payloads, references or both.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testRejectCaseInsensitiveDestinationCollision(final int kind) throws Exception {
        // Cover each pairing of payload and reference destinations that can alias.
        final Path path;
        if (kind == 0) {
            path = archive(regular("target"), regular("TARGET"), link("link", "target"));
        } else if (kind == 1) {
            path = archive(regular("target"), link("TARGET", "target"));
        } else {
            path = archive(regular("target"), link("LINK", "target"), link("link", "target"));
        }
        // macOS-style paths compare case-sensitively even when filesystem lookups ignore case.
        try (FileSystem fileSystem = MemoryFileSystemBuilder.newMacOs().build();
                ZipFile zip = ZipFile.builder().setPath(path).get()) {
            final IOException exception = assertThrows(IOException.class, () -> new Expander().expand(zip, fileSystem.getPath("/output")));
            assertTrue(exception.getMessage().contains("Conflicting ZIP extraction destination"));
        }
    }

    /**
     * Rejects symlink destinations or ancestors and incompatible existing directories.
     */
    @Test
    void testRejectExistingSymlinksAndDirectories() throws Exception {
        // A symlink extraction root must not redirect writes into an outside directory.
        final Path path = archive(regular("target"), link("link", "target"));
        final Path outside = Files.createDirectory(temp.resolve("outside"));
        Files.write(outside.resolve("target"), CONTENT);
        final Path output = temp.resolve("symlink-root");
        createSymbolicLink(output, outside);
        try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(zip, output));
            assertFalse(Files.exists(outside.resolve("link")));
            // Pre-existing leaf objects must have the required destination type.
            final Path directory = Files.createDirectory(temp.resolve("directory"));
            Files.createDirectory(directory.resolve("link"));
            assertThrows(IOException.class, () -> new Expander().expand(zip, directory));
            assertFalse(Files.exists(directory.resolve("target")));
            final Path leaf = Files.createDirectory(temp.resolve("leaf"));
            createSymbolicLink(leaf.resolve("target"), outside.resolve("target"));
            assertThrows(IOException.class, () -> new Expander().expand(zip, leaf));
            assertFalse(Files.exists(leaf.resolve("link")));
        }
        // Checking only the root and leaf would miss a symlink in an intermediate directory.
        final Path nested = Files.createDirectory(temp.resolve("nested"));
        createSymbolicLink(nested.resolve("sub"), outside);
        try (ZipFile zip = ZipFile.builder().setPath(archive(regular("sub/target"), link("link", "sub/target"))).get()) {
            assertThrows(IOException.class, () -> new Expander().expand(zip, nested));
        }
        assertArrayEquals(CONTENT, Files.readAllBytes(outside.resolve("target")));
    }

    /**
     * Replaces existing destinations without truncating unrelated paths sharing their old inode.
     */
    @Test
    void testOverwriteDoesNotTruncateUnrelatedInodes() throws Exception {
        assumeHardLinks();
        // Both destination names initially alias an unrelated file outside the output root.
        final Path outside = Files.write(temp.resolve("outside"), new byte[] {1, 2, 3});
        final Path output = Files.createDirectory(temp.resolve("overwrite"));
        Files.createLink(output.resolve("target"), outside);
        Files.createLink(output.resolve("link"), outside);
        try (ZipFile zip = ZipFile.builder().setPath(archive(link("link", "target"), regular("target"))).get()) {
            new Expander().expand(zip, output);
        }
        // Extraction must establish a new inode group and preserve the old group's bytes.
        assertTrue(Files.isSameFile(output.resolve("link"), output.resolve("target")));
        assertFalse(Files.isSameFile(outside, output.resolve("target")));
        assertArrayEquals(CONTENT, Files.readAllBytes(output.resolve("link")));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(outside));
    }

    /**
     * Propagates unsupported hard link creation without silently copying target contents.
     */
    @Test
    void testLinkCreationFailureDoesNotCopy() throws Exception {
        final Path path = archive(link("link", "target"), regular("target"));
        // The JDK ZIP filesystem supports regular-file writes but cannot create hard links.
        final URI outputUri = URI.create("jar:" + temp.resolve("output.zip").toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(outputUri, Collections.singletonMap("create", "true"));
                ZipFile zip = ZipFile.builder().setPath(path).get()) {
            final Path output = fileSystem.getPath("/");
            final IOException exception = assertThrows(IOException.class, () -> new Expander().expand(zip, output));
            assertTrue(exception.getCause() instanceof UnsupportedOperationException);
            assertArrayEquals(CONTENT, Files.readAllBytes(output.resolve("target")));
            assertFalse(Files.exists(output.resolve("link")));
        }
    }

    /**
     * Preserves discarded-output behavior and keeps symlinks separate from hard link handling.
     */
    @Test
    void testNullDestinationAndSymlinkControl() throws Exception {
        final ZipArchiveEntry symlink = regular("symlink");
        symlink.setUnixMode(UnixStat.LINK_FLAG | 0777);
        final Path path = archive(link("link", "target"), regular("target"), symlink);
        // Discarding output still validates links, without interpreting symlinks as references.
        try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
            new Expander().expand(zip, (Path) null);
            assertTrue(zip.getEntry("symlink").isUnixSymlink());
            assertFalse(zip.getEntry("symlink").isUnixHardLink());
            assertNull(zip.getUnixHardLink(zip.getEntry("symlink")));
        }
        // Existing Expander symlink behavior remains an ordinary file containing its body.
        assumeHardLinks();
        final Path output = temp.resolve("control");
        new Expander().expand(path, output);
        assertTrue(Files.isRegularFile(output.resolve("symlink"), LinkOption.NOFOLLOW_LINKS));
        assertArrayEquals(CONTENT, Files.readAllBytes(output.resolve("symlink")));
    }

    /**
     * Resolves a long reference chain without recursive stack growth.
     */
    @Test
    void testLongChainDoesNotRecurse() throws Exception {
        final Path path = temp.resolve("long-chain.zip");
        // Store a long forward chain ending in one regular payload.
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(path)) {
            for (int i = 0; i < 2000; i++) {
                write(out, link("link" + i, i == 1999 ? "target" : "link" + (i + 1)), EMPTY);
            }
            write(out, regular("target"), CONTENT);
        }
        try (ZipFile zip = ZipFile.builder().setPath(path).setResolveUnixHardLinks(true).get()) {
            assertArrayEquals(CONTENT, read(zip, zip.getEntry("link0")));
        }
    }

    /**
     * Bounds extraction's target decoding work linearly for chains and shared suffixes.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExtractionResolvesChainsOnce(final boolean reverse) throws Exception {
        final int count = 128;
        final List<ZipArchiveEntry> entries = new ArrayList<>();
        // The branch joins an intermediate reference, requiring reuse of resolved suffixes.
        for (int i = 0; i < count; i++) {
            entries.add(link("link" + i, i == count - 1 ? "target" : "link" + (i + 1)));
        }
        entries.add(link("branch", "link64"));
        entries.add(regular("target"));
        if (reverse) {
            Collections.reverse(entries);
        }
        final Path path = archive(entries.toArray(ZipArchiveEntry.EMPTY_ARRAY));
        try (ZipFile source = ZipFile.builder().setPath(path).get()) {
            final ZipFile zip = spy(source);
            new Expander().expand(zip, (Path) null);
            // Count decoding calls rather than asserting machine-dependent elapsed times.
            final long decodes = mockingDetails(zip).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("getUnixHardLink")).count();
            assertTrue(decodes <= 4L * entries.size(), "Target decodes must scale linearly, observed " + decodes);
        }
    }

    /**
     * Keeps bulk resolution results local to one call and observes subsequent entry mutations.
     */
    @Test
    void testBulkResolutionSnapshot() throws Exception {
        final Path path = archive(link("link", "middle"), link("middle", "first"), link("branch", "middle"), regular("first"), regular("second"));
        try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
            final ZipArchiveEntry entry = zip.getEntry("link");
            final Map<ZipArchiveEntry, ZipArchiveEntry> first = zip.resolveUnixHardLinks();
            assertEquals(3, first.size());
            assertSame(zip.getEntry("first"), first.get(entry));
            assertNull(first.get(new ZipArchiveEntry(entry)));
            assertThrows(UnsupportedOperationException.class, () -> first.put(entry, zip.getEntry("second")));
            new Expander().expand(zip, (Path) null);

            // A previously resolved chain can become cyclic; no old result may hide the mutation.
            final UnrecognizedExtraField field = (UnrecognizedExtraField) zip.getEntry("middle").getExtraField(UNIX);
            field.setLocalFileDataData(extra("link"));
            field.setCentralDirectoryData(extra("link"));
            assertThrows(IOException.class, zip::resolveUnixHardLinks);
            assertThrows(IOException.class, () -> new Expander().expand(zip, (Path) null));

            // Repairing the chain produces a new snapshot without changing the previous map.
            field.setLocalFileDataData(extra("second"));
            field.setCentralDirectoryData(extra("second"));
            final Map<ZipArchiveEntry, ZipArchiveEntry> second = zip.resolveUnixHardLinks();
            assertEquals(3, second.size());
            for (final ZipArchiveEntry target : second.values()) {
                assertSame(zip.getEntry("second"), target);
            }
            assertSame(zip.getEntry("first"), first.get(entry));
            assertSame(zip.getEntry("second"), zip.resolveUnixHardLink(entry));
            new Expander().expand(zip, (Path) null);
        }
    }

    /**
     * Restores empty and nonempty groups in either order without merging identical files.
     */
    @Test
    void testExtractHardLinksInBothOrders() throws Exception {
        assumeHardLinks();
        // Reverse the full archive to cover targets both before and after their references.
        for (final boolean reverse : new boolean[] {false, true}) {
            final Path archive = temp.resolve("links-" + reverse + ".zip");
            // Include cross-directory links, a chain and independent files with equal contents.
            final List<ZipArchiveEntry> entries = Arrays.asList(regular("a/data"), link("b/link", "a/data"), link("chain", "b/link"),
                    regular("a/empty"), link("b/empty", "a/empty"), regular("same"), regular("independent-empty"));
            if (reverse) {
                Collections.reverse(entries);
            }
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(archive)) {
                for (final ZipArchiveEntry entry : entries) {
                    write(out, entry, entry.getName().equals("a/data") || entry.getName().equals("same") ? CONTENT : EMPTY);
                }
            }
            final Path output = temp.resolve("output-" + reverse);
            new Expander().expand(archive, output);
            // Content equality alone is insufficient: verify shared and distinct file identities.
            assertArrayEquals(CONTENT, Files.readAllBytes(output.resolve("b/link")));
            assertTrue(Files.isSameFile(output.resolve("a/data"), output.resolve("b/link")));
            assertTrue(Files.isSameFile(output.resolve("a/data"), output.resolve("chain")));
            assertTrue(Files.isSameFile(output.resolve("a/empty"), output.resolve("b/empty")));
            assertFalse(Files.isSameFile(output.resolve("a/data"), output.resolve("same")));
            assertFalse(Files.isSameFile(output.resolve("a/empty"), output.resolve("independent-empty")));
        }
    }

    /**
     * Writes a stored fixture entry with explicit size and CRC metadata.
     */
    private void write(final ZipArchiveOutputStream out, final ZipArchiveEntry entry, final byte[] body) throws IOException {
        // Stored entries need complete metadata before either seekable or streaming output.
        final CRC32 crc = new CRC32();
        crc.update(body);
        entry.setMethod(ZipArchiveOutputStream.STORED);
        entry.setSize(body.length);
        entry.setCrc(crc.getValue());
        out.putArchiveEntry(entry);
        out.write(body);
        out.closeArchiveEntry();
    }
}
