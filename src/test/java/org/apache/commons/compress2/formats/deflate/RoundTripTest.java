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
package org.apache.commons.compress2.formats.deflate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.compress2.compressors.CompressedInput;
import org.apache.commons.compress2.compressors.CompressedOutput;
import org.apache.commons.compress2.formats.AbstractFileSystemTest;
import org.apache.commons.compress2.util.IOUtils;

import static org.apache.commons.compress2.TestSupport.getFile;

public class RoundTripTest extends AbstractFileSystemTest {

    @Test
    public void testRoundtripUsingConstructors() throws Exception {
        final File output = new File(dir, "constructors.def");
        final File file1 = getFile("test1.xml");
        try (WritableByteChannel out = new FileOutputStream(output).getChannel();
             DeflateCompressedOutput os = new DeflateCompressedOutput(out);
             WritableByteChannel c = os.startCompressing();
             ReadableByteChannel in = new FileInputStream(file1).getChannel()) {
            IOUtils.copy(in, c);
        }

        final File input = output;
        final File target = new File(dir, "test1.xml");
        try (ReadableByteChannel is = new FileInputStream(input).getChannel();
             DeflateCompressedInput in = new DeflateCompressedInput(is);
             ReadableByteChannel r = in.next();
             WritableByteChannel out = new FileOutputStream(target).getChannel()) {
            IOUtils.copy(r, out);
        }
    }

    @Test
    public void testRoundtripUsingFormatInstanceAndChannels() throws Exception {
        DeflateCompressionFormat format = new DeflateCompressionFormat();
        final File output = new File(dir, "format-channels.def");
        final File file1 = getFile("test1.xml");
        try (WritableByteChannel out = new FileOutputStream(output).getChannel();
             DeflateCompressedOutput os = format.writeTo(out);
             WritableByteChannel c = os.startCompressing();
             ReadableByteChannel in = new FileInputStream(file1).getChannel()) {
            IOUtils.copy(in, c);
        }

        final File input = output;
        final File target = new File(dir, "test1.xml");
        try (ReadableByteChannel is = new FileInputStream(input).getChannel();
             DeflateCompressedInput in = format.readFrom(is);
             ReadableByteChannel r = in.next();
             WritableByteChannel out = new FileOutputStream(target).getChannel()) {
            IOUtils.copy(r, out);
        }
    }

    @Test
    public void testRoundtripUsingFormatInstanceAndPaths() throws Exception {
        DeflateCompressionFormat format = new DeflateCompressionFormat();
        final File output = new File(dir, "format-files.def");
        final File file1 = getFile("test1.xml");
        try (CompressedOutput os = format.writeTo(output.toPath());
             WritableByteChannel c = os.startCompressing();
             ReadableByteChannel in = new FileInputStream(file1).getChannel()) {
            IOUtils.copy(in, c);
        }

        final File input = output;
        final File target = new File(dir, "test1.xml");
        try (CompressedInput in = format.readFrom(input.toPath());
             ReadableByteChannel r = in.next();
             WritableByteChannel out = new FileOutputStream(target).getChannel()) {
            IOUtils.copy(r, out);
        }
    }
}
