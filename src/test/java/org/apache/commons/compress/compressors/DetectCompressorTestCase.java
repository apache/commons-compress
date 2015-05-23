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
package org.apache.commons.compress.compressors;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.junit.Test;

@SuppressWarnings("deprecation") // deliberately tests setDecompressConcatenated
public final class DetectCompressorTestCase {

    final CompressorStreamFactory factory = new CompressorStreamFactory();
    private static final CompressorStreamFactory factoryTrue = new CompressorStreamFactory(true);
    private static final CompressorStreamFactory factoryFalse = new CompressorStreamFactory(false);

    // Must be static to allow use in the TestData entries
    private static final CompressorStreamFactory factorySetTrue;
    private static final CompressorStreamFactory factorySetFalse;

    static {
        factorySetTrue = new CompressorStreamFactory();
        factorySetTrue.setDecompressConcatenated(true);
        factorySetFalse = new CompressorStreamFactory();
        factorySetFalse.setDecompressConcatenated(false);        
    }

    static class TestData {
        final String fileName; // The multiple file name
        final char[] entryNames; // expected entries ...
        final CompressorStreamFactory factory; // ... when using this factory
        final boolean concat; // expected value for decompressConcatenated
        TestData(String name, char[] names, CompressorStreamFactory factory, boolean concat) {
            this.fileName = name;
            this.entryNames = names;
            this.factory = factory;
            this.concat = concat;
        }
    }

    private final TestData[] tests = {
        new TestData("multiple.bz2", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.bz2", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.bz2", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.bz2", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.bz2", new char[]{'a'}, factory, false),

        new TestData("multiple.gz", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.gz", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.gz", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.gz", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.gz", new char[]{'a'}, factory, false),

        new TestData("multiple.xz", new char[]{'a','b'}, factoryTrue, true),
        new TestData("multiple.xz", new char[]{'a','b'}, factorySetTrue, true),
        new TestData("multiple.xz", new char[]{'a'}, factoryFalse, false),
        new TestData("multiple.xz", new char[]{'a'}, factorySetFalse, false),
        new TestData("multiple.xz", new char[]{'a'}, factory, false),
    };
    
    @Test
    public void testDetection() throws Exception {
        CompressorInputStream bzip2 = getStreamFor("bla.txt.bz2"); 
        assertNotNull(bzip2);
        assertTrue(bzip2 instanceof BZip2CompressorInputStream);

        CompressorInputStream gzip = getStreamFor("bla.tgz");
        assertNotNull(gzip);
        assertTrue(gzip instanceof GzipCompressorInputStream);
        
        CompressorInputStream pack200 = getStreamFor("bla.pack");
        assertNotNull(pack200);
        assertTrue(pack200 instanceof Pack200CompressorInputStream);

        CompressorInputStream xz = getStreamFor("bla.tar.xz");
        assertNotNull(xz);
        assertTrue(xz instanceof XZCompressorInputStream);

        CompressorInputStream zlib = getStreamFor("bla.tar.deflatez");
        assertNotNull(zlib);
        assertTrue(zlib instanceof DeflateCompressorInputStream);

        try {
            factory.createCompressorInputStream(new ByteArrayInputStream(new byte[0]));
            fail("No exception thrown for an empty input stream");
        } catch (CompressorException e) {
            // expected
        }
    }

    @Test
    public void testOverride() {
        CompressorStreamFactory fac = new CompressorStreamFactory();
        assertFalse(fac.getDecompressConcatenated());
        fac.setDecompressConcatenated(true);
        assertTrue(fac.getDecompressConcatenated());

        fac = new CompressorStreamFactory(false);
        assertFalse(fac.getDecompressConcatenated());
        try {
            fac.setDecompressConcatenated(true);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            // expected
        }

        fac = new CompressorStreamFactory(true);
        assertTrue(fac.getDecompressConcatenated());
        try {
            fac.setDecompressConcatenated(true);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    @Test
    public void testMutiples() throws Exception {
        for(int i=0; i <tests.length; i++) {
            TestData test = tests[i];
            final CompressorStreamFactory fac = test.factory;
            assertNotNull("Test entry "+i, fac);
            assertEquals("Test entry "+i, test.concat, fac.getDecompressConcatenated());
            CompressorInputStream in = getStreamFor(test.fileName, fac);
            assertNotNull("Test entry "+i,in);
            for (char entry : test.entryNames) {
                assertEquals("Test entry" + i, entry, in.read());                
            }
            assertEquals(0, in.available());
            assertEquals(-1, in.read());
        }
    }

    private CompressorInputStream getStreamFor(String resource)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(
                   new BufferedInputStream(new FileInputStream(
                       getFile(resource))));
    }

    private CompressorInputStream getStreamFor(String resource, CompressorStreamFactory factory)
            throws CompressorException, IOException {
        return factory.createCompressorInputStream(
                   new BufferedInputStream(new FileInputStream(
                       getFile(resource))));
    }

}
