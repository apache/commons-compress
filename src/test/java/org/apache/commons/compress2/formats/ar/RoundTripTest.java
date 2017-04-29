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
package org.apache.commons.compress2.formats.ar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.ArchiveInput;
import org.apache.commons.compress2.archivers.ArchiveOutput;
import org.apache.commons.compress2.formats.AbstractFileSystemTest;
import org.apache.commons.compress2.util.IOUtils;

import static org.apache.commons.compress2.TestSupport.getFile;

public class RoundTripTest extends AbstractFileSystemTest {

    @Test
    public void testRoundtripUsingConstructors() throws Exception {
        final File output = new File(dir, "constructors.ar");
        {
            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");

            final WritableByteChannel out = new FileOutputStream(output).getChannel();
            final ArArchiveOutput os = new ArArchiveOutput(out);
            IOUtils.copy(new FileInputStream(file1).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file1.toPath()))));
            os.closeEntry();

            IOUtils.copy(new FileInputStream(file2).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file2.toPath()))));
            os.closeEntry();
            os.close();
            out.close();
        }

        // UnArArchive Operation
        final File input = output;
        final ReadableByteChannel is = new FileInputStream(input).getChannel();
        final ArArchiveInput in = new ArArchiveInput(is);
        ArArchiveEntry entry = in.next();
        Assert.assertEquals("test1.xml", entry.getName());

        File target = new File(dir, entry.getName());
        final WritableByteChannel out = new FileOutputStream(target).getChannel();

        IOUtils.copy(in.getChannel(), out);
        out.close();

        entry = in.next();
        Assert.assertEquals("test2.xml", entry.getName());

        in.close();
        is.close();
    }

    @Test
    public void testRoundtripUsingFormatInstanceAndChannels() throws Exception {
        ArArchiveFormat format = new ArArchiveFormat();
        final File output = new File(dir, "format-channels.ar");
        {
            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");

            final WritableByteChannel out = new FileOutputStream(output).getChannel();
            final ArchiveOutput<ArArchiveEntry> os = format.writeTo(out, null);
            IOUtils.copy(new FileInputStream(file1).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file1.toPath()))));
            os.closeEntry();

            IOUtils.copy(new FileInputStream(file2).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file2.toPath()))));
            os.closeEntry();
            os.close();
            out.close();
        }

        // UnArArchive Operation
        final File input = output;
        final ReadableByteChannel is = new FileInputStream(input).getChannel();
        final ArchiveInput<ArArchiveEntry> in = format.readFrom(is, null);
        ArArchiveEntry entry = in.next();
        Assert.assertEquals("test1.xml", entry.getName());

        File target = new File(dir, entry.getName());
        final WritableByteChannel out = new FileOutputStream(target).getChannel();

        IOUtils.copy(in.getChannel(), out);
        out.close();

        entry = in.next();
        Assert.assertEquals("test2.xml", entry.getName());

        in.close();
        is.close();
    }

    @Test
    public void testRoundtripUsingFormatInstanceAndPaths() throws Exception {
        ArArchiveFormat format = new ArArchiveFormat();
        final File output = new File(dir, "format-files.ar");
        {
            final File file1 = getFile("test1.xml");
            final File file2 = getFile("test2.xml");

            final ArchiveOutput<ArArchiveEntry> os = format.writeTo(output.toPath(), null);
            IOUtils.copy(new FileInputStream(file1).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file1.toPath()))));
            os.closeEntry();

            IOUtils.copy(new FileInputStream(file2).getChannel(),
                         os.putEntry(os.createEntry(ArchiveEntryParameters.fromPath(file2.toPath()))));
            os.closeEntry();
            os.close();
        }

        // UnArArchive Operation
        final File input = output;
        final ArchiveInput<ArArchiveEntry> in = format.readFrom(input.toPath(), null);
        ArArchiveEntry entry = in.next();
        Assert.assertEquals("test1.xml", entry.getName());

        File target = new File(dir, entry.getName());
        final WritableByteChannel out = new FileOutputStream(target).getChannel();

        IOUtils.copy(in.getChannel(), out);
        out.close();

        entry = in.next();
        Assert.assertEquals("test2.xml", entry.getName());

        in.close();
    }

}
