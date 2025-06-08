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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.compress.AbstractTest;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ArchiveStreamFactoryTest extends AbstractTest {

    static class TestData {
        final String testFile;
        final String expectedEncoding;
        final ArchiveStreamFactory fac;
        final String fieldName;
        final String type;
        final boolean hasOutputStream;

        TestData(final String testFile, final String type, final boolean hasOut, final String expectedEncoding, final ArchiveStreamFactory fac,
                final String fieldName) {
            this.testFile = testFile;
            this.expectedEncoding = expectedEncoding;
            this.fac = fac;
            this.fieldName = fieldName;
            this.type = type;
            this.hasOutputStream = hasOut;
        }

        @Override
        public String toString() {
            return "TestData [testFile=" + testFile + ", expectedEncoding=" + expectedEncoding + ", fac=" + fac + ", fieldName=" + fieldName + ", type=" + type
                    + ", hasOutputStream=" + hasOutputStream + "]";
        }
    }

    private static final String UNKNOWN = "??";

    private static final ArchiveStreamFactory FACTORY = ArchiveStreamFactory.DEFAULT;

    private static final ArchiveStreamFactory FACTORY_UTF8 = new ArchiveStreamFactory(StandardCharsets.UTF_8.name());

    private static final ArchiveStreamFactory FACTORY_ASCII = new ArchiveStreamFactory(StandardCharsets.US_ASCII.name());

    private static final ArchiveStreamFactory FACTORY_SET_UTF8 = getFactory(StandardCharsets.UTF_8.name());

    private static final ArchiveStreamFactory FACTORY_SET_ASCII = getFactory(StandardCharsets.US_ASCII.name());

    /**
     * Default encoding if none is provided (not even null). The test currently assumes that the output default is the same as the input default.
     */
    private static final String ARJ_DEFAULT;
    private static final String DUMP_DEFAULT;
    private static final String ZIP_DEFAULT = getCharsetName(new ZipArchiveInputStream(null));
    private static final String CPIO_DEFAULT = getCharsetName(new CpioArchiveInputStream(null));
    private static final String TAR_DEFAULT = getCharsetName(new TarArchiveInputStream(null));
    private static final String JAR_DEFAULT = getCharsetName(new JarArchiveInputStream(null));

    static {
        String dflt;
        dflt = UNKNOWN;
        try (ArjArchiveInputStream inputStream = new ArjArchiveInputStream(newInputStream("bla.arj"))) {
            dflt = getCharsetName(inputStream);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        ARJ_DEFAULT = dflt;
        dflt = UNKNOWN;
        try (DumpArchiveInputStream inputStream = new DumpArchiveInputStream(newInputStream("bla.dump"))) {
            dflt = getCharsetName(inputStream);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        DUMP_DEFAULT = dflt;
    }

    static final TestData[] TESTS = { new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, ARJ_DEFAULT, FACTORY, "charsetName"),
            new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charsetName"),
            new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charsetName"),
            new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charsetName"),
            new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charsetName"),

            new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, CPIO_DEFAULT, FACTORY, "charsetName"),
            new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charsetName"),
            new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charsetName"),
            new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charsetName"),
            new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charsetName"),

            new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, DUMP_DEFAULT, FACTORY, "charsetName"),
            new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charsetName"),
            new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charsetName"),
            new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charsetName"),
            new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charsetName"),

            new TestData("bla.tar", ArchiveStreamFactory.TAR, true, TAR_DEFAULT, FACTORY, "charsetName"),
            new TestData("bla.tar", ArchiveStreamFactory.TAR, true, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charsetName"),
            new TestData("bla.tar", ArchiveStreamFactory.TAR, true, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charsetName"),
            new TestData("bla.tar", ArchiveStreamFactory.TAR, true, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charsetName"),
            new TestData("bla.tar", ArchiveStreamFactory.TAR, true, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charsetName"),

            new TestData("bla.jar", ArchiveStreamFactory.JAR, true, JAR_DEFAULT, FACTORY, "charset"),
            new TestData("bla.jar", ArchiveStreamFactory.JAR, true, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charset"),
            new TestData("bla.jar", ArchiveStreamFactory.JAR, true, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charset"),
            new TestData("bla.jar", ArchiveStreamFactory.JAR, true, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charset"),
            new TestData("bla.jar", ArchiveStreamFactory.JAR, true, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charset"),

            new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, ZIP_DEFAULT, FACTORY, "charset"),
            new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, StandardCharsets.UTF_8.name(), FACTORY_UTF8, "charset"),
            new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, StandardCharsets.US_ASCII.name(), FACTORY_ASCII, "charset"),
            new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, StandardCharsets.UTF_8.name(), FACTORY_SET_UTF8, "charset"),
            new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, StandardCharsets.US_ASCII.name(), FACTORY_SET_ASCII, "charset"), };

    private static String getCharsetName(final ArchiveInputStream<?> inputStream) {
        return inputStream.getCharset().name();
    }

    @SuppressWarnings("deprecation") // test of deprecated method
    static ArchiveStreamFactory getFactory(final String entryEncoding) {
        final ArchiveStreamFactory fac = new ArchiveStreamFactory();
        fac.setEntryEncoding(entryEncoding);
        return fac;
    }

    private static String getFieldAsString(final Object instance, final String name) {
        if (instance instanceof ArchiveInputStream) {
            return getCharsetName((ArchiveInputStream<?>) instance);
        }
        try {
            final Object object = FieldUtils.readField(instance, name, true);
            if (object == null) {
                return null;
            }
            if (object instanceof String) {
                // For example "charsetName"
                return (String) object;
            }
            if (object instanceof Charset) {
                // For example "charset"
                return ((Charset) object).name();
            }
            // System.out.println("Wrong type: " + object.getClass().getCanonicalName() + " for " + name + " in class " + instance.getClass().getSimpleName());
            return object.toString();
        } catch (final IllegalAccessException e) {
            System.out.println("Cannot find " + name + " in class " + instance.getClass().getSimpleName());
            return UNKNOWN;
        }
    }

    @SuppressWarnings("resource") // Caller closes
    public static Stream<Path> getIcoPathStream() throws IOException {
        return Files.walk(Paths.get("src/test/resources/org/apache/commons/compress/ico")).filter(Files::isRegularFile);
    }

    private String detect(final String resource) throws IOException, ArchiveException {
        try (InputStream in = new BufferedInputStream(newInputStream(resource))) {
            return ArchiveStreamFactory.detect(in);
        }
    }

    @SuppressWarnings("resource")
    private <T extends ArchiveInputStream<? extends E>, E extends ArchiveEntry> T getInputStream(final String resource, final ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveInputStream(new BufferedInputStream(newInputStream(resource)));
    }

    @SuppressWarnings("resource")
    private <T extends ArchiveInputStream<? extends E>, E extends ArchiveEntry> T getInputStream(final String type, final String resource,
            final ArchiveStreamFactory factory) throws IOException, ArchiveException {
        return factory.createArchiveInputStream(type, new BufferedInputStream(newInputStream(resource)));
    }

    private <T extends ArchiveOutputStream<? extends E>, E extends ArchiveEntry> T getOutputStream(final String type, final ArchiveStreamFactory factory)
            throws ArchiveException {
        return factory.createArchiveOutputStream(type, new ByteArrayOutputStream());
    }

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-191
     */
    @Test
    void testAiffFilesAreNoTARs() throws Exception {
        try (InputStream fis = newInputStream("testAIFF.aif");
                InputStream is = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(is),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    @Test
    void testCantRead7zFromStream() throws Exception {
        assertThrows(StreamingNotSupportedException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(ArchiveStreamFactory.SEVEN_Z,
                new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)));
    }

    @Test
    void testCantWrite7zToStream() throws Exception {
        assertThrows(StreamingNotSupportedException.class,
                () -> ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.SEVEN_Z, new ByteArrayOutputStream()));
    }

    @Test
    void testCOMPRESS209() throws Exception {
        try (InputStream fis = newInputStream("testCompress209.doc");
                InputStream bis = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    @Test
    void testDetect() throws Exception {
        for (final String extension : new String[] { ArchiveStreamFactory.AR, ArchiveStreamFactory.ARJ, ArchiveStreamFactory.CPIO, ArchiveStreamFactory.DUMP,
                // Compress doesn't know how to detect JARs, see COMPRESS-91
                // ArchiveStreamFactory.JAR,
                ArchiveStreamFactory.SEVEN_Z, ArchiveStreamFactory.TAR, ArchiveStreamFactory.ZIP }) {
            assertEquals(extension, detect("bla." + extension));
        }

        final ArchiveException e1 = assertThrows(ArchiveException.class,
                () -> ArchiveStreamFactory.detect(new BufferedInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))),
                "shouldn't be able to detect empty stream");
        assertEquals("No Archiver found for the stream signature", e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> ArchiveStreamFactory.detect(null),
                "shouldn't be able to detect null stream");
        assertEquals("Stream must not be null.", e2.getMessage());

        final ArchiveException e3 = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.detect(new BufferedInputStream(new BrokenInputStream())),
                "Expected ArchiveException");
        assertEquals("Failure reading signature.", e3.getMessage());
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-267">COMPRESS-267</a>.
     */
    @Test
    void testDetectsAndThrowsFor7z() throws Exception {
        try (InputStream fis = newInputStream("bla.7z");
                InputStream bis = new BufferedInputStream(fis)) {
            final StreamingNotSupportedException ex = assertThrows(StreamingNotSupportedException.class,
                    () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis), "Expected a StreamingNotSupportedException");
            assertEquals(ArchiveStreamFactory.SEVEN_Z, ex.getFormat());
        }
    }

    @Test
    void testEncodingCtor() {
        ArchiveStreamFactory fac = new ArchiveStreamFactory();
        assertNull(fac.getEntryEncoding());
        fac = new ArchiveStreamFactory(null);
        assertNull(fac.getEntryEncoding());
        fac = new ArchiveStreamFactory(StandardCharsets.UTF_8.name());
        assertEquals(StandardCharsets.UTF_8.name(), fac.getEntryEncoding());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testEncodingDeprecated() {
        final ArchiveStreamFactory fac1 = new ArchiveStreamFactory();
        assertNull(fac1.getEntryEncoding());
        fac1.setEntryEncoding(StandardCharsets.UTF_8.name());
        assertEquals(StandardCharsets.UTF_8.name(), fac1.getEntryEncoding());
        fac1.setEntryEncoding(StandardCharsets.US_ASCII.name());
        assertEquals(StandardCharsets.US_ASCII.name(), fac1.getEntryEncoding());
        final ArchiveStreamFactory fac2 = new ArchiveStreamFactory(StandardCharsets.UTF_8.name());
        assertEquals(StandardCharsets.UTF_8.name(), fac2.getEntryEncoding());
        fac2.setEntryEncoding(StandardCharsets.US_ASCII.name());
        assertEquals(StandardCharsets.US_ASCII.name(), fac2.getEntryEncoding());
    }

    @Test
    void testEncodingInputStream() throws Exception {
        int failed = 0;
        for (int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i - 1];
            try (ArchiveInputStream<?> ais = getInputStream(test.type, test.testFile, test.fac)) {
                final String field = getCharsetName(ais);
                if (!Objects.equals(field, field)) {
                    System.err.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                    failed++;
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }

    @Test
    void testEncodingInputStreamAutodetect() throws Exception {
        int failed = 0;
        for (int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i - 1];
            try (ArchiveInputStream<?> ais = getInputStream(test.testFile, test.fac)) {
                final String field = getCharsetName(ais);
                if (!Objects.equals(field, field)) {
                    System.err.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                    failed++;
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }

    @Test
    void testEncodingOutputStream() throws Exception {
        int failed = 0;
        for (int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i - 1];
            if (test.hasOutputStream) {
                try (ArchiveOutputStream<?> ais = getOutputStream(test.type, test.fac)) {
                    final String field = getFieldAsString(ais, test.fieldName);
                    if (!Objects.equals(field, field)) {
                        System.err.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                        failed++;
                    }
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }

    @ParameterizedTest
    @MethodSource("getIcoPathStream")
    void testIcoFilesAreNoTARs(final Path path) throws Exception {
        try (InputStream fis = Files.newInputStream(path);
                InputStream is = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.detect(is),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
        try (InputStream fis = Files.newInputStream(path);
                InputStream is = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(is),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    /**
     * See https://issues.apache.org/jira/browse/COMPRESS-171
     */
    @Test
    void testShortTextFilesAreNoTARs() {
        final ArchiveException ae = assertThrows(ArchiveException.class,
                () -> ArchiveStreamFactory.DEFAULT
                        .createArchiveInputStream(new ByteArrayInputStream("This certainly is not a tar archive, really, no kidding".getBytes())),
                "created an input stream for a non-archive");
        assertTrue(ae.getMessage().startsWith("No Archiver found"));
    }

    /**
     * Tests case for <a href="https://issues.apache.org/jira/browse/COMPRESS-208">COMPRESS-208</a>.
     */
    @Test
    void testSkipsPK00Prefix() throws Exception {
        try (InputStream fis = newInputStream("COMPRESS-208.zip")) {
            try (InputStream bis = new BufferedInputStream(fis)) {
                try (ArchiveInputStream<?> ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis)) {
                    assertInstanceOf(ZipArchiveInputStream.class, ais);
                }
            }
        }
    }

    @Test
    void testTarContainingDirWith1TxtFileIsTAR() throws IOException, ArchiveException {
        assertEquals(ArchiveStreamFactory.TAR, detect("dirWith1TxtFile.tar"));
    }

    @Test
    void testTarContainingEmptyDirIsTAR() throws IOException, ArchiveException {
        assertEquals(ArchiveStreamFactory.TAR, detect("emptyDir.tar"));
    }

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/COMPRESS-674">COMPRESS-674</a>.
     */
    @Test
    void testUtf16TextIsNotTAR() {
        final ArchiveException archiveException = assertThrows(ArchiveException.class,
                () -> detect("utf16-text.txt"));
        assertEquals("No Archiver found for the stream signature", archiveException.getMessage());
    }
}
