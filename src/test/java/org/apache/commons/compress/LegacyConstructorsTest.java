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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
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

    static Stream<Arguments> testCpioConstructors() {
        final InputStream inputStream = mock(InputStream.class);
        return Stream.of(
                Arguments.of(new CpioArchiveInputStream(inputStream, 1024), inputStream, "US-ASCII", 1024),
                Arguments.of(new CpioArchiveInputStream(inputStream, 1024, "UTF-8"), inputStream, "UTF-8", 1024),
                Arguments.of(new CpioArchiveInputStream(inputStream, "UTF-8"), inputStream, "UTF-8", 512));
    }

    static Stream<Arguments> testTarConstructors() {
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

    static Stream<Arguments> testZipConstructors() {
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
            // Arj wraps the input stream in a DataInputStream
            assertEquals(inputStream, getNestedInputStream(getNestedInputStream(archiveInputStream)));
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
}
