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
package org.apache.commons.compress2.archivers;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

public class ArchiveEntryParametersTest {

    @Test
    public void defaultValues() {
        ArchiveEntryParameters p = new ArchiveEntryParameters();
        assertEquals(null, p.getName());
        assertEquals(-1, p.getSize());
        assertEquals(false, p.isDirectory());
        assertEquals(null, p.getLastModified());
        assertEquals(null, p.getOwnerInformation());
    }

    @Test
    public void shouldAddTrailingSlashForDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withName("foo").asDirectory(true);
        assertEquals("foo/", p.getName());
        p.withName("foo/");
        assertEquals("foo/", p.getName());
        p.withName("");
        assertEquals("/", p.getName());
    }

    @Test
    public void shouldStripTrailingSlashForNonDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withName("foo").asDirectory(false);
        assertEquals("foo", p.getName());
        p.withName("foo/");
        assertEquals("foo", p.getName());
        p.withName("");
        assertEquals("", p.getName());
    }

    @Test
    public void sizeShouldBe0ForDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .asDirectory(true);
        assertEquals(0, p.getSize());
        p.withSize(42);
        assertEquals(0, p.getSize());
    }

    @Test
    public void copyActuallyCopies() {
        final Instant d = Instant.now();
        final OwnerInformation o = new OwnerInformation(17, 4);
        ArchiveEntryParameters p = ArchiveEntryParameters.copyOf(new ArchiveEntry() {
                public String getName() {return "baz";}
                public long getSize() {return 42;}
                public boolean isDirectory() {return false;}
                public Instant getLastModified() {return d;}
                public OwnerInformation getOwnerInformation() {return o;}
            });
        assertEquals("baz", p.getName());
        assertEquals(42, p.getSize());
        assertEquals(false, p.isDirectory());
        assertEquals(d, p.getLastModified());
        assertEquals(o, p.getOwnerInformation());
    }

    @Test
    public void fromExistingFileHasExpectedValues() throws IOException {
        final Instant d = Instant.now();
        File f = File.createTempFile("pre", "suf");
        f.deleteOnExit();
        f.setLastModified(d.toEpochMilli());
        ArchiveEntryParameters p = ArchiveEntryParameters.fromFile(f);
        assert p.getName().endsWith("suf");
        assert p.getName().startsWith("pre");
        assertEquals(0, p.getSize());
        assertEquals(false, p.isDirectory());
        assertWithinTwoSecondsOf(d, p.getLastModified());
        assertEquals(null, p.getOwnerInformation());
    }

    @Test
    public void fromExistingDirectoryHasExpectedValues() throws IOException {
        final Instant d = Instant.now();
        File f = File.createTempFile("pre", "suf");
        assert f.delete();
        f.mkdirs();
        f.deleteOnExit();
        f.setLastModified(d.toEpochMilli());
        ArchiveEntryParameters p = ArchiveEntryParameters.fromFile(f);
        assert p.getName().endsWith("suf/");
        assert p.getName().startsWith("pre");
        assertEquals(0, p.getSize());
        assertEquals(true, p.isDirectory());
        assertWithinTwoSecondsOf(d, p.getLastModified());
        assertEquals(null, p.getOwnerInformation());
    }

    @Test
    public void fromNonExistingFileHasNoSize() throws IOException {
        File f = File.createTempFile("pre", "suf");
        assert f.delete();
        ArchiveEntryParameters p = ArchiveEntryParameters.fromFile(f);
        assertEquals(-1, p.getSize());
    }

    private static void assertWithinTwoSecondsOf(Instant expected, Instant actual) {
        assert Math.abs(Duration.between(expected, actual).getSeconds()) < 2;
    }
}
