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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class XZTestCase extends AbstractTestCase {

    @Test
    public void testXZCreation()  throws Exception {
        long max = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        System.out.println("XZTestCase: HeapMax="+max+" bytes "+(double)max/(1024*1024)+" MB");
        final File input = getFile("test1.xml");
        final File output = new File(dir, "test1.xml.xz");
        final OutputStream out = new FileOutputStream(output);
        try {
            final CompressorOutputStream cos = new CompressorStreamFactory()
                .createCompressorOutputStream("xz", out);
            try {
                IOUtils.copy(new FileInputStream(input), cos);
            } finally {
                cos.close();
            }
        } finally {
            out.close();
        }
    }

    @Test
    public void testXZUnarchive() throws Exception {
        final File input = getFile("bla.tar.xz");
        final File output = new File(dir, "bla.tar");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in = new CompressorStreamFactory()
                .createCompressorInputStream("xz", is);
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
        } finally {
            is.close();
        }
    }

    @Test
    public void testConcatenatedStreamsReadFirstOnly() throws Exception {
        final File input = getFile("multiple.xz");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in = new CompressorStreamFactory()
                .createCompressorInputStream("xz", is);
            try {
                assertEquals('a', in.read());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testConcatenatedStreamsReadFully() throws Exception {
        final File input = getFile("multiple.xz");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in =
                new XZCompressorInputStream(is, true);
            try {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }
}
