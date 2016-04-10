/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;

import static org.junit.Assert.*;

public class ScatterSampleTest {

    @Test
    public void testSample() throws Exception {
        final File result = File.createTempFile("testSample", "fe");

        createFile(result);
        checkFile(result);
    }

    private void createFile(final File result) throws IOException, ExecutionException, InterruptedException {
        final ScatterSample scatterSample = new ScatterSample();
        final ZipArchiveEntry archiveEntry = new ZipArchiveEntry("test1.xml");
        archiveEntry.setMethod(ZipEntry.DEFLATED);
        final InputStreamSupplier supp = new InputStreamSupplier() {
            @Override
            public InputStream get() {
                return new ByteArrayInputStream("Hello".getBytes());
            }
        };

        scatterSample.addEntry(archiveEntry, supp);
        final ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(result);
        scatterSample.writeTo(zipArchiveOutputStream);
        zipArchiveOutputStream.close();
    }

    private void checkFile(final File result) throws IOException {
        final ZipFile zf = new ZipFile(result);
        final ZipArchiveEntry archiveEntry1 = zf.getEntries().nextElement();
        assertEquals( "test1.xml", archiveEntry1.getName());
        final InputStream inputStream = zf.getInputStream(archiveEntry1);
        final byte[] b = new byte[6];
        final int i = IOUtils.readFully(inputStream, b);
        assertEquals(5, i);
        assertEquals('H', b[0]);
        assertEquals('o', b[4]);
        zf.close();
        result.delete();
    }
}