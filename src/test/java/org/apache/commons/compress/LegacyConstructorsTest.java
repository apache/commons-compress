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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredField;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZFileOptions;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that deprecated constructors are equivalent to the new builder pattern.
 */
@Tag("deprecated")
@SuppressWarnings("deprecation") // testing deprecated code
class LegacyConstructorsTest extends AbstractTest {

    private static InputStream getNestedInputStream(final InputStream is) throws ReflectiveOperationException {
        return (InputStream) readField(is, "in", true);
    }

    static Stream<Arguments> testCpioConstructors() throws IOException {
        final InputStream inputStream = mock(InputStream.class);
        return Stream.of(
                Arguments.of(new CpioArchiveInputStream(inputStream, 1024), inputStream, "US-ASCII", 1024),
                Arguments.of(new CpioArchiveInputStream(inputStream, 1024, "UTF-8"), inputStream, "UTF-8", 1024),
                Arguments.of(new CpioArchiveInputStream(inputStream, "UTF-8"), inputStream, "UTF-8", 512));
    }

    static Stream<Arguments> testTarConstructors() throws IOException {
        final InputStream inputStream = mock(InputStream.class);
        final String defaultEncoding = Charset.defaultCharset().name();
        final String otherEncoding = "UTF-8".equals(defaultEncoding) ? "US-ASCII" : "UTF-8";
        return Stream.of(
                Arguments.of(new TarArchiveInputStream(inputStream, true), inputStream, 10240, 512, defaultEncoding, true),
                Arguments.of(new TarArchiveInputStream(inputStream, 20480), inputStream, 20480, 512, defaultEncoding, false),
                Arguments.of(new TarArchiveInputStream(inputStream, 20480, 1024), inputStream, 20480, 1024, defaultEncoding, false),
                Arguments.of(new TarArchiveInputStream(inputStream, 20480, 1024, otherEncoding), inputStream, 20480, 1024, otherEncoding, false),
                Arguments.of(new TarArchiveInputStream(inputStream, 20480, 1024, otherEncoding, true), inputStream, 20480, 1024, otherEncoding, true),
                Arguments.of(new TarArchiveInputStream(inputStream, 20480, otherEncoding), inputStream, 20480, 512, otherEncoding, false),
                Arguments.of(new TarArchiveInputStream(inputStream, otherEncoding), inputStream, 10240, 512, otherEncoding, false));
    }

    static Stream<Arguments> testZipConstructors() throws IOException {
        final InputStream inputStream = mock(InputStream.class);
        return Stream.of(
                Arguments.of(new ZipArchiveInputStream(inputStream, "US-ASCII"), inputStream, "US-ASCII", true, false, false),
                Arguments.of(new ZipArchiveInputStream(inputStream, "US-ASCII", false), inputStream, "US-ASCII", false, false, false),
                Arguments.of(new ZipArchiveInputStream(inputStream, "US-ASCII", false, true), inputStream, "US-ASCII", false, true, false),
                Arguments.of(new ZipArchiveInputStream(inputStream, "US-ASCII", false, true, true), inputStream, "US-ASCII", false, true, true));
    }

    @Test
    void testArjConstructor() throws Exception {
        try (InputStream inputStream = Files.newInputStream(getPath("bla.arj"));
                ArjArchiveInputStream archiveInputStream = new ArjArchiveInputStream(inputStream, "US-ASCII")) {
            assertEquals(inputStream, getNestedInputStream(archiveInputStream));
            assertEquals(US_ASCII, archiveInputStream.getCharset());
        }
    }

    @ParameterizedTest
    @MethodSource
    void testCpioConstructors(final CpioArchiveInputStream archiveStream, final InputStream expectedInput, final String expectedEncoding,
            final int expectedBlockSize) throws Exception {
        assertEquals(expectedInput, getNestedInputStream(archiveStream));
        assertEquals(Charset.forName(expectedEncoding), archiveStream.getCharset());
        assertEquals(expectedBlockSize, readDeclaredField(archiveStream, "blockSize", true));
    }

    @Test
    void testDumpConstructor() throws Exception {
        final String otherEncoding = "UTF-8".equals(Charset.defaultCharset().name()) ? "US-ASCII" : "UTF-8";
        try (InputStream inputStream = Files.newInputStream(getPath("bla.dump"));
                DumpArchiveInputStream archiveStream = new DumpArchiveInputStream(inputStream, otherEncoding)) {
            assertEquals(inputStream, getNestedInputStream(archiveStream));
            assertEquals(Charset.forName(otherEncoding), archiveStream.getCharset());
        }
    }

    @Test
    void testJarConstructor() throws Exception {
        final InputStream inputStream = mock(InputStream.class);
        try (JarArchiveInputStream archiveInputStream = new JarArchiveInputStream(inputStream, "US-ASCII")) {
            assertEquals(US_ASCII, archiveInputStream.getCharset());
        }
    }

    static Stream<Arguments> testSevenZFileContructors() throws IOException {
        final Path path = getPath("bla.7z");
        final String defaultName = "unknown archive";
        final String otherName = path.toAbsolutePath().toString();
        final String customName = "customName";
        final int defaultMemoryLimit = SevenZFileOptions.DEFAULT.getMaxMemoryLimitInKb();
        final boolean defaultUseDefaultNameForUnnamedEntries = SevenZFileOptions.DEFAULT.getUseDefaultNameForUnnamedEntries();
        final boolean defaultTryToRecoverBrokenArchives = SevenZFileOptions.DEFAULT.getTryToRecoverBrokenArchives();
        final SevenZFileOptions otherOptions =
                SevenZFileOptions.builder().withMaxMemoryLimitInKb(42).withTryToRecoverBrokenArchives(true).withUseDefaultNameForUnnamedEntries(true).build();
        final char[] otherPassword = "password".toCharArray();
        final byte[] otherPasswordBytes = "password".getBytes(StandardCharsets.UTF_16LE);
        return Stream.of(
                // From File
                Arguments.of(new SevenZFile(path.toFile()), otherName, defaultMemoryLimit, defaultUseDefaultNameForUnnamedEntries,
                        defaultTryToRecoverBrokenArchives, null),
                Arguments.of(new SevenZFile(path.toFile(), otherPasswordBytes), otherName, defaultMemoryLimit, defaultUseDefaultNameForUnnamedEntries,
                        defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(path.toFile(), otherPassword), otherName, defaultMemoryLimit, defaultUseDefaultNameForUnnamedEntries,
                        defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(path.toFile(), otherPassword, otherOptions), otherName, 42, true, true, otherPasswordBytes),
                Arguments.of(new SevenZFile(path.toFile(), otherOptions), otherName, 42, true, true, null),
                // From SeekableByteChannel
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ)), defaultName, defaultMemoryLimit,
                        defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, null),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), otherPasswordBytes), defaultName, defaultMemoryLimit,
                        defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), otherPassword), defaultName, defaultMemoryLimit,
                        defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), otherPassword, otherOptions), defaultName, 42, true, true,
                        otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), otherOptions), defaultName, 42, true, true, null),
                // From SeekableByteChannel with custom name
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), customName), customName, defaultMemoryLimit,
                        defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, null),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), customName, otherPasswordBytes), customName,
                        defaultMemoryLimit, defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), customName, otherPassword), customName, defaultMemoryLimit,
                        defaultUseDefaultNameForUnnamedEntries, defaultTryToRecoverBrokenArchives, otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), customName, otherPassword, otherOptions), customName, 42,
                        true, true, otherPasswordBytes),
                Arguments.of(new SevenZFile(Files.newByteChannel(path, StandardOpenOption.READ), customName, otherOptions), customName, 42, true,
                        true, null));
    }

    @ParameterizedTest
    @MethodSource
    void testSevenZFileContructors(final SevenZFile archiveFile, final String expectedName, final int expectedMemoryLimit,
            final boolean expectedUseDefaultNameForUnnamedEntries, final boolean expectedTryToRecoverBrokenArchives,
            final byte[] expectedPassword) throws Exception {
        assertEquals(expectedName, readDeclaredField(archiveFile, "fileName", true));
        assertEquals(expectedMemoryLimit, readDeclaredField(archiveFile, "maxMemoryLimitKiB", true));
        assertEquals(expectedUseDefaultNameForUnnamedEntries, readDeclaredField(archiveFile, "useDefaultNameForUnnamedEntries", true));
        assertEquals(expectedTryToRecoverBrokenArchives, readDeclaredField(archiveFile, "tryToRecoverBrokenArchives", true));
        assertArrayEquals(expectedPassword, (byte[]) readDeclaredField(archiveFile, "password", true));
    }

    @ParameterizedTest
    @MethodSource
    void testTarConstructors(final TarArchiveInputStream archiveStream, final InputStream expectedInput, final int expectedBlockSize,
            final int expectedRecordSize, final String expectedEncoding, final boolean expectedLenient) throws Exception {
        assertEquals(expectedInput, getNestedInputStream(archiveStream));
        assertEquals(expectedBlockSize, readDeclaredField(archiveStream, "blockSize", true));
        final byte[] recordBuffer = (byte[]) readField(archiveStream, "recordBuffer", true);
        assertEquals(expectedRecordSize, recordBuffer.length);
        assertEquals(Charset.forName(expectedEncoding), archiveStream.getCharset());
        assertEquals(expectedLenient, readField(archiveStream, "lenient", true));
    }

    static Stream<Arguments> testTarFileConstructors() throws IOException {
        final Path path = getPath("bla.tar");
        final File file = getFile("bla.tar");
        final SeekableByteChannel channel = mock(SeekableByteChannel.class);
        final String defaultEncoding = Charset.defaultCharset().name();
        final String otherEncoding = "UTF-8".equals(defaultEncoding) ? "US-ASCII" : "UTF-8";
        return Stream.of(
                Arguments.of(new TarFile(IOUtils.EMPTY_BYTE_ARRAY), defaultEncoding, false),
                Arguments.of(new TarFile(IOUtils.EMPTY_BYTE_ARRAY, true), defaultEncoding, true),
                Arguments.of(new TarFile(IOUtils.EMPTY_BYTE_ARRAY, otherEncoding), otherEncoding, false),
                Arguments.of(new TarFile(file), defaultEncoding, false),
                Arguments.of(new TarFile(file, true), defaultEncoding, true),
                Arguments.of(new TarFile(file, otherEncoding), otherEncoding, false),
                Arguments.of(new TarFile(path), defaultEncoding, false),
                Arguments.of(new TarFile(path, true), defaultEncoding, true),
                Arguments.of(new TarFile(path, otherEncoding), otherEncoding, false),
                Arguments.of(new TarFile(channel), defaultEncoding, false),
                Arguments.of(new TarFile(channel, 1024, 1024, otherEncoding, true), otherEncoding, true));
    }

    @ParameterizedTest
    @MethodSource
    void testTarFileConstructors(final TarFile tarFile, final String expectedEncoding, final boolean expectedLenient) throws Exception {
        final ZipEncoding encoding = (ZipEncoding) readDeclaredField(tarFile, "zipEncoding", true);
        final Charset charset = (Charset) readDeclaredField(encoding, "charset", true);
        assertEquals(Charset.forName(expectedEncoding), charset);
        assertEquals(expectedLenient, readDeclaredField(tarFile, "lenient", true));
    }

    @ParameterizedTest
    @MethodSource
    void testZipConstructors(final ZipArchiveInputStream archiveStream, final InputStream expectedInput, final String expectedEncoding,
            final boolean expectedUseUnicodeExtraFields, final boolean expectedSupportStoredEntryDataDescriptor, final boolean expectedSkipSplitSignature)
            throws Exception {
        // Zip wraps the input stream in a PushbackInputStream
        assertEquals(expectedInput, getNestedInputStream(getNestedInputStream(archiveStream)));
        assertEquals(Charset.forName(expectedEncoding), archiveStream.getCharset());
        assertEquals(expectedUseUnicodeExtraFields, readDeclaredField(archiveStream, "useUnicodeExtraFields", true));
        assertEquals(expectedSupportStoredEntryDataDescriptor, readDeclaredField(archiveStream, "supportStoredEntryDataDescriptor", true));
        assertEquals(expectedSkipSplitSignature, readDeclaredField(archiveStream, "skipSplitSignature", true));
    }

    static Stream<Arguments> testZipFileConstructors() throws IOException {
        final Path path = getPath("bla.zip");
        final String defaultEncoding = StandardCharsets.UTF_8.name();
        final String otherEncoding = "UTF-8".equals(defaultEncoding) ? "US-ASCII" : "UTF-8";
        return Stream.of(
                Arguments.of(new ZipFile(path.toFile()), defaultEncoding, true),
                Arguments.of(new ZipFile(path.toFile(), otherEncoding), otherEncoding, true),
                Arguments.of(new ZipFile(path.toFile(), otherEncoding, false), otherEncoding, false),
                Arguments.of(new ZipFile(path.toFile(), otherEncoding, false, true), otherEncoding, false),
                Arguments.of(new ZipFile(path), defaultEncoding, true),
                Arguments.of(new ZipFile(path, otherEncoding), otherEncoding, true),
                Arguments.of(new ZipFile(path, otherEncoding, false), otherEncoding, false),
                Arguments.of(new ZipFile(path, otherEncoding, false, true), otherEncoding, false),
                Arguments.of(new ZipFile(Files.newByteChannel(path, StandardOpenOption.READ)), defaultEncoding, true),
                Arguments.of(new ZipFile(Files.newByteChannel(path, StandardOpenOption.READ), otherEncoding), otherEncoding, true),
                Arguments.of(new ZipFile(Files.newByteChannel(path, StandardOpenOption.READ), null, otherEncoding, false),
                        otherEncoding, false),
                Arguments.of(new ZipFile(Files.newByteChannel(path, StandardOpenOption.READ), null, otherEncoding, false, true),
                        otherEncoding, false),
                Arguments.of(new ZipFile(path.toAbsolutePath().toString()), defaultEncoding, true),
                Arguments.of(new ZipFile(path.toAbsolutePath().toString(), otherEncoding), otherEncoding, true));
    }

    @ParameterizedTest
    @MethodSource
    void testZipFileConstructors(final ZipFile zipFile, final String expectedEncoding, final boolean expectedUseUnicodeExtraFields) throws Exception {
        assertEquals(Charset.forName(expectedEncoding), readDeclaredField(zipFile, "encoding", true));
        assertEquals(expectedUseUnicodeExtraFields, readDeclaredField(zipFile, "useUnicodeExtraFields", true));
    }
}
