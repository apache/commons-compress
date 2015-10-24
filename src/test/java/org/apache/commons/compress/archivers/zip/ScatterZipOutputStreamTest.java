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
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.apache.commons.compress.AbstractTestCase.tryHardToDelete;
import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ScatterZipOutputStreamTest {

    private File scatterFile = null;
    private File target = null;

    @After
    public void cleanup() {
        tryHardToDelete(scatterFile);
        tryHardToDelete(target);
    }
    
    @Test
    public void putArchiveEntry() throws Exception {
        scatterFile = File.createTempFile("scattertest", ".notzip");
        ScatterZipOutputStream scatterZipOutputStream = ScatterZipOutputStream.fileBased(scatterFile);
        final byte[] B_PAYLOAD = "RBBBBBBS".getBytes();
        final byte[] A_PAYLOAD = "XAAY".getBytes();

        ZipArchiveEntry zab = new ZipArchiveEntry("b.txt");
        zab.setMethod(ZipArchiveEntry.DEFLATED);
        final ByteArrayInputStream payload = new ByteArrayInputStream(B_PAYLOAD);
        scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zab, createPayloadSupplier(payload)));

        ZipArchiveEntry zae = new ZipArchiveEntry("a.txt");
        zae.setMethod(ZipArchiveEntry.DEFLATED);
        ByteArrayInputStream payload1 = new ByteArrayInputStream(A_PAYLOAD);
        scatterZipOutputStream.addArchiveEntry(createZipArchiveEntryRequest(zae, createPayloadSupplier(payload1)));

        target = File.createTempFile("scattertest", ".zip");
        ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(target);
        scatterZipOutputStream.writeTo( outputStream);
        outputStream.close();
        scatterZipOutputStream.close();

        ZipFile zf = new ZipFile(target);
        final ZipArchiveEntry b_entry = zf.getEntries("b.txt").iterator().next();
        assertEquals(8, b_entry.getSize());
        assertArrayEquals(B_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(b_entry)));

        final ZipArchiveEntry a_entry = zf.getEntries("a.txt").iterator().next();
        assertEquals(4, a_entry.getSize());
        assertArrayEquals(A_PAYLOAD, IOUtils.toByteArray(zf.getInputStream(a_entry)));
        zf.close();
    }

    private InputStreamSupplier createPayloadSupplier(final ByteArrayInputStream payload) {
        return new InputStreamSupplier() {
            public InputStream get() {
                return payload;
            }
        };
    }
}
