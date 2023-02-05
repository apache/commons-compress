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
package org.apache.commons.compress.archivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.MockEvilInputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.junit.jupiter.api.Test;

public class ArchiveStreamFactoryTest extends AbstractTestCase {

    static class TestData {
        final String testFile;
        final String expectedEncoding;
        final ArchiveStreamFactory fac;
        final String fieldName;
        final String type;
        final boolean hasOutputStream;

        TestData(final String testFile, final String type, final boolean hasOut, final String expectedEncoding, final ArchiveStreamFactory fac, final String fieldName) {
            this.testFile = testFile;
            this.expectedEncoding = expectedEncoding;
            this.fac = fac;
            this.fieldName = fieldName;
            this.type = type;
            this.hasOutputStream = hasOut;
        }

        @Override
        public String toString() {
            return "TestData [testFile=" + testFile + ", expectedEncoding=" + expectedEncoding + ", fac=" + fac
                    + ", fieldName=" + fieldName + ", type=" + type + ", hasOutputStream=" + hasOutputStream + "]";
        }
    }

    private static final String UNKNOWN = "??";

    // The different factory types
    private static final ArchiveStreamFactory FACTORY = ArchiveStreamFactory.DEFAULT;

    private static final ArchiveStreamFactory FACTORY_UTF8 = new ArchiveStreamFactory("UTF-8");

    private static final ArchiveStreamFactory FACTORY_ASCII = new ArchiveStreamFactory("ASCII");

    private static final ArchiveStreamFactory FACTORY_SET_UTF8 = getFactory("UTF-8");

    private static final ArchiveStreamFactory FACTORY_SET_ASCII = getFactory("ASCII");

    // Default encoding if none is provided (not even null)
    // The test currently assumes that the output default is the same as the input default
    private static final String ARJ_DEFAULT;

    private static final String DUMP_DEFAULT;

    private static final String ZIP_DEFAULT = getField(new ZipArchiveInputStream(null),"encoding");

    private static final String CPIO_DEFAULT = getField(new CpioArchiveInputStream(null),"encoding");

    private static final String TAR_DEFAULT = getField(new TarArchiveInputStream(null),"encoding");
    private static final String JAR_DEFAULT = getField(new JarArchiveInputStream(null),"encoding");
    static {
        String dflt;
        dflt = UNKNOWN;
        try {
            dflt = getField(new ArjArchiveInputStream(newInputStream("bla.arj")), "charsetName");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        ARJ_DEFAULT = dflt;
        dflt = UNKNOWN;
        try {
            dflt = getField(new DumpArchiveInputStream(newInputStream("bla.dump")), "encoding");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        DUMP_DEFAULT = dflt;
    }
    static final TestData[] TESTS = {
        new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, ARJ_DEFAULT, FACTORY, "charsetName"),
        new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, "UTF-8", FACTORY_UTF8, "charsetName"),
        new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, "ASCII", FACTORY_ASCII, "charsetName"),
        new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, "UTF-8", FACTORY_SET_UTF8, "charsetName"),
        new TestData("bla.arj", ArchiveStreamFactory.ARJ, false, "ASCII", FACTORY_SET_ASCII, "charsetName"),

        new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, CPIO_DEFAULT, FACTORY, "encoding"),
        new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, "UTF-8", FACTORY_UTF8, "encoding"),
        new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, "ASCII", FACTORY_ASCII, "encoding"),
        new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, "UTF-8", FACTORY_SET_UTF8, "encoding"),
        new TestData("bla.cpio", ArchiveStreamFactory.CPIO, true, "ASCII", FACTORY_SET_ASCII, "encoding"),

        new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, DUMP_DEFAULT, FACTORY, "encoding"),
        new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, "UTF-8", FACTORY_UTF8, "encoding"),
        new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, "ASCII", FACTORY_ASCII, "encoding"),
        new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, "UTF-8", FACTORY_SET_UTF8, "encoding"),
        new TestData("bla.dump", ArchiveStreamFactory.DUMP, false, "ASCII", FACTORY_SET_ASCII, "encoding"),

        new TestData("bla.tar", ArchiveStreamFactory.TAR, true, TAR_DEFAULT, FACTORY, "encoding"),
        new TestData("bla.tar", ArchiveStreamFactory.TAR, true, "UTF-8", FACTORY_UTF8, "encoding"),
        new TestData("bla.tar", ArchiveStreamFactory.TAR, true, "ASCII", FACTORY_ASCII, "encoding"),
        new TestData("bla.tar", ArchiveStreamFactory.TAR, true, "UTF-8", FACTORY_SET_UTF8, "encoding"),
        new TestData("bla.tar", ArchiveStreamFactory.TAR, true, "ASCII", FACTORY_SET_ASCII, "encoding"),

        new TestData("bla.jar", ArchiveStreamFactory.JAR, true, JAR_DEFAULT, FACTORY, "encoding"),
        new TestData("bla.jar", ArchiveStreamFactory.JAR, true, "UTF-8", FACTORY_UTF8, "encoding"),
        new TestData("bla.jar", ArchiveStreamFactory.JAR, true, "ASCII", FACTORY_ASCII, "encoding"),
        new TestData("bla.jar", ArchiveStreamFactory.JAR, true, "UTF-8", FACTORY_SET_UTF8, "encoding"),
        new TestData("bla.jar", ArchiveStreamFactory.JAR, true, "ASCII", FACTORY_SET_ASCII, "encoding"),

        new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, ZIP_DEFAULT, FACTORY, "encoding"),
        new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, "UTF-8", FACTORY_UTF8, "encoding"),
        new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, "ASCII", FACTORY_ASCII, "encoding"),
        new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, "UTF-8", FACTORY_SET_UTF8, "encoding"),
        new TestData("bla.zip", ArchiveStreamFactory.ZIP, true, "ASCII", FACTORY_SET_ASCII, "encoding"),
    };
    // equals allowing null
    private static boolean eq(final String exp, final String act) {
        if (exp == null) {
            return act == null;
        }
        return exp.equals(act);
    }
    @SuppressWarnings("deprecation") // test of deprecated method
    static ArchiveStreamFactory getFactory(final String entryEncoding) {
        final ArchiveStreamFactory fac = new ArchiveStreamFactory();
        fac.setEntryEncoding(entryEncoding);
        return fac;
    }

    private static String getField(final Object instance, final String name) {
        final Class<?> cls = instance.getClass();
        Field fld;
        try {
            fld = cls.getDeclaredField(name);
        } catch (final NoSuchFieldException nsfe) {
                try {
                    fld = cls.getSuperclass().getDeclaredField(name);
                } catch (final NoSuchFieldException e) {
                    System.out.println("Cannot find " + name + " in class " + instance.getClass().getSimpleName());
                    return UNKNOWN;
                }
        }
        final boolean isAccessible = fld.isAccessible();
        try {
            if (!isAccessible) {
                fld.setAccessible(true);
            }
            final Object object = fld.get(instance);
            if (object instanceof String || object == null) {
                return (String) object;
            }
            System.out.println("Wrong type: " + object.getClass().getCanonicalName() + " for " + name + " in class " + instance.getClass().getSimpleName());
            return UNKNOWN;
        } catch (final Exception e) {
            e.printStackTrace();
            return UNKNOWN;
        } finally {
            if (!isAccessible) {
                fld.setAccessible(isAccessible);
            }
        }
    }
    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-191
     */
    @Test
    public void aiffFilesAreNoTARs() throws Exception {
        try (final InputStream fis = newInputStream("testAIFF.aif");
             final InputStream is = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(is),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    @Test
    public void cantRead7zFromStream() throws Exception {
        assertThrows(StreamingNotSupportedException.class,
            () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(ArchiveStreamFactory.SEVEN_Z, new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY)));
    }
    @Test
    public void cantWrite7zToStream() throws Exception {
        assertThrows(StreamingNotSupportedException.class,
            () -> ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(ArchiveStreamFactory.SEVEN_Z, new ByteArrayOutputStream()));
    }

    private String detect(final String resource) throws IOException, ArchiveException {
        try (InputStream in = new BufferedInputStream(newInputStream(resource))) {
            return ArchiveStreamFactory.detect(in);
        }
    }
    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-267"
     * >COMPRESS-267</a>.
     */
    @Test
    public void detectsAndThrowsFor7z() throws Exception {
        try (final InputStream fis = newInputStream("bla.7z");
             final InputStream bis = new BufferedInputStream(fis)) {
            final StreamingNotSupportedException ex = assertThrows(StreamingNotSupportedException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis),
                    "Expected a StreamingNotSupportedException");
            assertEquals(ArchiveStreamFactory.SEVEN_Z, ex.getFormat());
        }
    }

    private ArchiveInputStream getInputStreamFor(final String resource, final ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveInputStream(new BufferedInputStream(newInputStream(resource)));
    }

    private ArchiveInputStream getInputStreamFor(final String type, final String resource, final ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveInputStream(type, new BufferedInputStream(newInputStream(resource)));
    }

    private ArchiveOutputStream getOutputStreamFor(final String type, final ArchiveStreamFactory factory)
            throws ArchiveException {
        return factory.createArchiveOutputStream(type, new ByteArrayOutputStream());
    }

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-171
     */
    @Test
    public void shortTextFilesAreNoTARs() {
        final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(
                new ByteArrayInputStream(("This certainly is not a tar archive, really, no kidding").getBytes())), "created an input stream for a non-archive");
        assertTrue(ae.getMessage().startsWith("No Archiver found"));
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    @Test
    public void skipsPK00Prefix() throws Exception {
        try (InputStream fis = newInputStream("COMPRESS-208.zip")) {
            try (InputStream bis = new BufferedInputStream(fis)) {
                try (ArchiveInputStream ais = ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis)) {
                    assertTrue(ais instanceof ZipArchiveInputStream);
                }
            }
        }
    }

    @Test
    public void testCOMPRESS209() throws Exception {
        try (final InputStream fis = newInputStream("testCompress209.doc");
             final InputStream bis = new BufferedInputStream(fis)) {
            final ArchiveException ae = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.DEFAULT.createArchiveInputStream(bis),
                    "created an input stream for a non-archive");
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    @Test
    public void testDetect() throws Exception {
        for (final String extension : new String[]{
                ArchiveStreamFactory.AR,
                ArchiveStreamFactory.ARJ,
                ArchiveStreamFactory.CPIO,
                ArchiveStreamFactory.DUMP,
                // Compress doesn't know how to detect JARs, see COMPRESS-91
 //               ArchiveStreamFactory.JAR,
                ArchiveStreamFactory.SEVEN_Z,
                ArchiveStreamFactory.TAR,
                ArchiveStreamFactory.ZIP
        }) {
            assertEquals(extension, detect("bla."+extension));
        }

        final ArchiveException e1 = assertThrows(ArchiveException.class,
                () -> ArchiveStreamFactory.detect(new BufferedInputStream(new ByteArrayInputStream(ByteUtils.EMPTY_BYTE_ARRAY))),
                "shouldn't be able to detect empty stream");
        assertEquals("No Archiver found for the stream signature", e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> ArchiveStreamFactory.detect(null),
                "shouldn't be able to detect null stream");
        assertEquals("Stream must not be null.", e2.getMessage());

        final ArchiveException e3 = assertThrows(ArchiveException.class, () -> ArchiveStreamFactory.detect(new BufferedInputStream(new MockEvilInputStream())),
                "Expected ArchiveException");
        assertEquals("IOException while reading signature.", e3.getMessage());
    }

    @Test
    public void testEncodingCtor() {
        ArchiveStreamFactory fac = new ArchiveStreamFactory();
        assertNull(fac.getEntryEncoding());
        fac = new ArchiveStreamFactory(null);
        assertNull(fac.getEntryEncoding());
        fac = new ArchiveStreamFactory("UTF-8");
        assertEquals("UTF-8", fac.getEntryEncoding());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testEncodingDeprecated() {
        final ArchiveStreamFactory fac1 = new ArchiveStreamFactory();
        assertNull(fac1.getEntryEncoding());
        fac1.setEntryEncoding("UTF-8");
        assertEquals("UTF-8", fac1.getEntryEncoding());
        fac1.setEntryEncoding("US_ASCII");
        assertEquals("US_ASCII", fac1.getEntryEncoding());
        final ArchiveStreamFactory fac2 = new ArchiveStreamFactory("UTF-8");
        assertEquals("UTF-8", fac2.getEntryEncoding());
        assertThrows(IllegalStateException.class, () -> fac2.setEntryEncoding("US_ASCII"), "Expected IllegalStateException");
    }

    @Test
    public void testEncodingInputStream() throws Exception {
        int failed = 0;
        for (int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i - 1];
            try (final ArchiveInputStream ais = getInputStreamFor(test.type, test.testFile, test.fac)) {
                final String field = getField(ais, test.fieldName);
                if (!eq(test.expectedEncoding, field)) {
                    System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field
                            + " type: " + test.type);
                    failed++;
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }

    @Test
    public void testEncodingInputStreamAutodetect() throws Exception {
        int failed = 0;
        for (int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i - 1];
            try (final ArchiveInputStream ais = getInputStreamFor(test.testFile, test.fac)) {
                final String field = getField(ais, test.fieldName);
                if (!eq(test.expectedEncoding, field)) {
                    System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field
                            + " type: " + test.type);
                    failed++;
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }

    @Test
    public void testEncodingOutputStream() throws Exception {
        int failed = 0;
        for(int i = 1; i <= TESTS.length; i++) {
            final TestData test = TESTS[i-1];
            if (test.hasOutputStream) {
                try (final ArchiveOutputStream ais = getOutputStreamFor(test.type, test.fac)) {
                    final String field = getField(ais, test.fieldName);
                    if (!eq(test.expectedEncoding, field)) {
                        System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: "
                                + field + " type: " + test.type);
                        failed++;
                    }
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed + " out of " + TESTS.length);
        }
    }
}
