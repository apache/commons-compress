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
package org.apache.commons.compress.compressors.gzip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @see "https://issues.apache.org/jira/browse/COMPRESS-376"
 */
public class Compress376Test extends AbstractTestCase {

    private interface StreamDecorator {
        InputStream decorate(InputStream is) throws IOException;
    }

    @Test
    public void testUnbuffered() throws Exception {
        test(new StreamDecorator() {
                @Override
                public InputStream decorate(InputStream is) {
                    return is;
                }
            });
    }

    @Test
    public void testBuffered() throws Exception {
        test(new StreamDecorator() {
                @Override
                public InputStream decorate(InputStream is) throws IOException {
                    return new BufferedInputStream(is);
                }
            });
    }

    /*
     * Really only asserts there is no error thrown. The archive
     * contains two entries and garbage after the second one. We
     * should be able to read both entries without an exception.
     */
    public void test(StreamDecorator decorator) throws Exception {
        final File input = getFile("COMPRESS-376.tar.gz");
        try (InputStream fis = new FileInputStream(input);
             InputStream gis = new GzipCompressorInputStream(fis, true);
             InputStream is = decorator.decorate(gis);
             TarArchiveInputStream in = new TarArchiveInputStream(is)) {
            TarArchiveEntry entry = (TarArchiveEntry) in.getNextEntry();
            try (OutputStream out = new FileOutputStream(new File(dir, entry.getName()))) {
                IOUtils.copy(in, out);
            }
            entry = (TarArchiveEntry) in.getNextEntry();
            try (OutputStream out = new FileOutputStream(new File(dir, entry.getName()))) {
                IOUtils.copy(in, out);
            }
            try {
                in.getNextEntry();
                Assert.fail("should report garbage");
            } catch (IOException ex) {
                Assert.assertEquals("Garbage after a valid .gz stream", ex.getMessage());
            }
        }
    }

}
