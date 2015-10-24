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

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.Test;

public class ArchiveStreamFactoryTest {

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-171
     */
    @Test
    public void shortTextFilesAreNoTARs() throws Exception {
        try {
            new ArchiveStreamFactory()
                .createArchiveInputStream(new ByteArrayInputStream("This certainly is not a tar archive, really, no kidding".getBytes()));
            fail("created an input stream for a non-archive");
        } catch (ArchiveException ae) {
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-191
     */
    @Test
    public void aiffFilesAreNoTARs() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/testAIFF.aif");
    	try {
            InputStream is = new BufferedInputStream(fis);
            try {
                new ArchiveStreamFactory().createArchiveInputStream(is);
                fail("created an input stream for a non-archive");
            } catch (ArchiveException ae) {
                assertTrue(ae.getMessage().startsWith("No Archiver found"));
            } finally {
                is.close();
            }
    	} finally {
            fis.close();
    	}
    }

    @Test
    public void testCOMPRESS209() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/testCompress209.doc");
    	try {
            InputStream bis = new BufferedInputStream(fis);
            try {
                new ArchiveStreamFactory().createArchiveInputStream(bis);
                fail("created an input stream for a non-archive");
            } catch (ArchiveException ae) {
                assertTrue(ae.getMessage().startsWith("No Archiver found"));
            } finally {
                bis.close();
            }
    	} finally {
            fis.close();
    	}
    }

    @Test(expected = StreamingNotSupportedException.class)
    public void cantRead7zFromStream() throws Exception {
        new ArchiveStreamFactory()
            .createArchiveInputStream(ArchiveStreamFactory.SEVEN_Z,
                                      new ByteArrayInputStream(new byte[0]));
    }

    @Test(expected = StreamingNotSupportedException.class)
    public void cantWrite7zToStream() throws Exception {
        new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.SEVEN_Z,
                                       new ByteArrayOutputStream());
    }

    /**
     * Test case for 
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-267"
     * >COMPRESS-267</a>.
     */
    @Test
    public void detectsAndThrowsFor7z() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/bla.7z");
    	try {
            InputStream bis = new BufferedInputStream(fis);
            try {
                new ArchiveStreamFactory().createArchiveInputStream(bis);
                fail("Expected a StreamingNotSupportedException");
            } catch (StreamingNotSupportedException ex) {
                assertEquals(ArchiveStreamFactory.SEVEN_Z, ex.getFormat());
            } finally {
                bis.close();
            }
    	} finally {
            fis.close();
    	}
    }

    /**
     * Test case for 
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    @Test
    public void skipsPK00Prefix() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/COMPRESS-208.zip");
    	try {
            InputStream bis = new BufferedInputStream(fis);
            try {
                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
                try {
                    assertTrue(ais instanceof ZipArchiveInputStream);
                } finally {
                    ais.close();
                }
            } finally {
                bis.close();
            }
    	} finally {
            fis.close();
    	}
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
        ArchiveStreamFactory fac = new ArchiveStreamFactory();
        assertNull(fac.getEntryEncoding());
        fac.setEntryEncoding("UTF-8");
        assertEquals("UTF-8", fac.getEntryEncoding());
        fac.setEntryEncoding("US_ASCII");
        assertEquals("US_ASCII", fac.getEntryEncoding());
        fac = new ArchiveStreamFactory("UTF-8");
        assertEquals("UTF-8", fac.getEntryEncoding());
        try {
            fac.setEntryEncoding("US_ASCII");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    static class TestData {
        final String testFile;
        final String expectedEncoding;
        final ArchiveStreamFactory fac;
        final String fieldName;
        final String type;
        final boolean hasOutputStream;
        TestData(String testFile, String type, boolean hasOut, String expectedEncoding, ArchiveStreamFactory fac, String fieldName) {
            this.testFile = testFile;
            this.expectedEncoding = expectedEncoding;
            this.fac = fac;
            this.fieldName = fieldName;
            this.type = type;
            this.hasOutputStream = hasOut;
        }
    }

    @SuppressWarnings("deprecation") // test of deprecated method
    static ArchiveStreamFactory getFactory(String entryEncoding) {
        ArchiveStreamFactory fac = new ArchiveStreamFactory();
        fac.setEntryEncoding(entryEncoding);
        return fac;
    }
    // The different factory types
    private static final ArchiveStreamFactory FACTORY = new ArchiveStreamFactory();
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
        dflt = "??";
        try {
            dflt = getField(new ArjArchiveInputStream(new FileInputStream(getFile("bla.arj"))), "charsetName");
        } catch (ArchiveException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ARJ_DEFAULT = dflt;
        dflt = "??";
        try {
            dflt = getField(new DumpArchiveInputStream(new FileInputStream(getFile("bla.dump"))), "encoding");
        } catch (ArchiveException e) {
            e.printStackTrace();
        } catch (Exception e) {
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

    @Test
    public void testEncodingInputStreamAutodetect() throws Exception {
        int failed = 0;
        for(int i = 1; i <= TESTS.length; i++) {
            TestData test = TESTS[i-1];
            ArchiveInputStream ais = getInputStreamFor(test.testFile, test.fac);
            final String field = getField(ais,test.fieldName);
            if (!eq(test.expectedEncoding,field)) {
                System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                failed++;
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed);
        }
    }

    @Test
    public void testEncodingInputStream() throws Exception {
        int failed = 0;
        for(int i = 1; i <= TESTS.length; i++) {
            TestData test = TESTS[i-1];
            ArchiveInputStream ais = getInputStreamFor(test.type, test.testFile, test.fac);
            final String field = getField(ais,test.fieldName);
            if (!eq(test.expectedEncoding,field)) {
                System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                failed++;
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed);
        }
    }

    @Test
    public void testEncodingOutputStream() throws Exception {
        int failed = 0;
        for(int i = 1; i <= TESTS.length; i++) {
            TestData test = TESTS[i-1];
            if (test.hasOutputStream) {
                ArchiveOutputStream ais = getOutputStreamFor(test.type, test.fac);
                final String field = getField(ais, test.fieldName);
                if (!eq(test.expectedEncoding, field)) {
                    System.out.println("Failed test " + i + ". expected: " + test.expectedEncoding + " actual: " + field + " type: " + test.type);
                    failed++;
                }
            }
        }
        if (failed > 0) {
            fail("Tests failed: " + failed);
        }
    }

    // equals allowing null
    private static boolean eq(String exp, String act) {
        if (exp == null) {
            return act == null;
        }
        return exp.equals(act);
    }

    private static String getField(Object instance, String name) {
        Class<?> cls = instance.getClass();
        Field fld;
        try {
            fld = cls.getDeclaredField(name);
        } catch (NoSuchFieldException nsfe) {
                try {
                    fld = cls.getSuperclass().getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    System.out.println("Cannot find " + name + " in class " + instance.getClass().getSimpleName());
                    return "??";
                }                
        }
        boolean isAccessible = fld.isAccessible();
        try {
            if (!isAccessible) {
                fld.setAccessible(true);
            }
            final Object object = fld.get(instance);
            if (object instanceof String || object == null) {
                return (String) object;
            } else {
                System.out.println("Wrong type: " + object.getClass().getCanonicalName() + " for " + name + " in class " + instance.getClass().getSimpleName());
                return "??";                
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "??";
        } finally {
            if (!isAccessible) {
                fld.setAccessible(isAccessible);
            }
        }
    }

    private ArchiveInputStream getInputStreamFor(String resource, ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveInputStream(
                   new BufferedInputStream(new FileInputStream(
                       getFile(resource))));
    }

    private ArchiveInputStream getInputStreamFor(String type, String resource, ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveInputStream(
                   type,
                   new BufferedInputStream(new FileInputStream(
                       getFile(resource))));
    }

    private ArchiveOutputStream getOutputStreamFor(String type, ArchiveStreamFactory factory)
            throws IOException, ArchiveException {
        return factory.createArchiveOutputStream(type, new ByteArrayOutputStream());
    }
}
