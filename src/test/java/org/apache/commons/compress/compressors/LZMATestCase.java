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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class LZMATestCase extends AbstractTestCase {

    @Test
    public void testLZMAUnarchive() throws Exception {
        final File input = getFile("bla.tar.lzma");
        final File output = new File(dir, "bla.tar");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in = new LZMACompressorInputStream(is);
            copy(in, output);
        } finally {
            is.close();
        }
    }

    @Test
    public void testLZMAUnarchiveWithAutodetection() throws Exception {
        final File input = getFile("bla.tar.lzma");
        final File output = new File(dir, "bla.tar");
        final InputStream is = new BufferedInputStream(new FileInputStream(input));
        try {
            final CompressorInputStream in = new CompressorStreamFactory()
                .createCompressorInputStream(is);
            copy(in, output);
        } finally {
            is.close();
        }
    }

    private void copy(InputStream in, File output) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(output);
            IOUtils.copy(in, out);
        } finally {
            if (out != null) {
                out.close();
            }
            in.close();
        }
    }
}
