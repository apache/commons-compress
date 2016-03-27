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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

public class ArchiveEntryParametersTest {

    @Test
    public void defaultValues() {
        ArchiveEntryParameters p = new ArchiveEntryParameters();
        assertNull(p.getName());
        assertEquals(-1, p.size());
        assertFalse(p.isDirectory());
        assertFalse(p.isSymbolicLink());
        assertFalse(p.isOther());
        assertTrue(p.isRegularFile());
        assertNull(p.lastModifiedTime());
        assertNull(p.lastAccessTime());
        assertNull(p.creationTime());
        assertNull(p.fileKey());
        assertFalse(p.getOwnerInformation().isPresent());
        assertFalse(p.getPermissions().isPresent());
        assertEquals(ArchiveEntry.FileType.REGULAR_FILE, p.getType());
    }

    @Test
    public void shouldAddTrailingSlashForDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withName("foo").withType(ArchiveEntry.FileType.DIR);
        assertEquals("foo/", p.getName());
        p.withName("foo/");
        assertEquals("foo/", p.getName());
        p.withName("");
        assertEquals("/", p.getName());
    }

    @Test
    public void shouldStripTrailingSlashForNonDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withName("foo").withType(ArchiveEntry.FileType.REGULAR_FILE);
        assertEquals("foo", p.getName());
        p.withName("foo/");
        assertEquals("foo", p.getName());
        p.withName("");
        assertEquals("", p.getName());
    }

    @Test
    public void sizeShouldBe0ForDirectories() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withType(ArchiveEntry.FileType.DIR);
        assertEquals(0, p.size());
        p.withSize(42);
        assertEquals(0, p.size());
    }

    @Test
    public void copyActuallyCopies() {
        final FileTime d1 = FileTime.from(Instant.now());
        final FileTime d2 = FileTime.from(Instant.now());
        final OwnerInformation o = new OwnerInformation(17, 4);
        ArchiveEntryParameters p = ArchiveEntryParameters.copyOf(new ArchiveEntry() {
                @Override
                public String getName() {return "baz";}
                @Override
                public long size() {return 42;}
                @Override
                public boolean isDirectory() {return false;}
                @Override
                public boolean isSymbolicLink() {return false;}
                @Override
                public boolean isOther() {return false;}
                @Override
                public boolean isRegularFile() {return true;}
                @Override
                public FileTime lastModifiedTime() {return d1;}
                @Override
                public FileTime lastAccessTime() {return null;}
                @Override
                public FileTime creationTime() {return d2;}
                @Override
                public Optional<OwnerInformation> getOwnerInformation() {return Optional.of(o);}
                @Override
                public Optional<Long> getMode() { return Optional.of(4711l); }
                @Override
                public Optional<Set<PosixFilePermission>> getPermissions() {
                    return Optional.of(EnumSet.of(PosixFilePermission.OWNER_READ));
                }
                @Override
                public Object fileKey() {
                    return "foo";
                }
            });
        assertEquals("baz", p.getName());
        assertEquals(42, p.size());
        assertFalse(p.isDirectory());
        assertFalse(p.isSymbolicLink());
        assertFalse(p.isOther());
        assertTrue(p.isRegularFile());
        assertEquals(d1, p.lastModifiedTime());
        assertEquals(d2, p.creationTime());
        assertNull(p.lastAccessTime());
        assertEquals("foo", p.fileKey());
        assertEquals(ArchiveEntry.FileType.REGULAR_FILE, p.getType());
        assertEquals(o, p.getOwnerInformation().get());
        assertEquals(4711l, p.getMode().get().longValue());
        assertTrue(p.getPermissions().get().contains(PosixFilePermission.OWNER_READ));
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
        assertEquals(0, p.size());
        assertEquals(ArchiveEntry.FileType.REGULAR_FILE, p.getType());
        assertWithinTwoSecondsOf(d, p.lastModifiedTime());
        assertFalse(p.getOwnerInformation().isPresent());
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
        assertEquals(0, p.size());
        assertEquals(ArchiveEntry.FileType.DIR, p.getType());
        assertWithinTwoSecondsOf(d, p.lastModifiedTime());
        assertFalse(p.getOwnerInformation().isPresent());
    }

    @Test
    public void fromNonExistingFileHasNoSize() throws IOException {
        File f = File.createTempFile("pre", "suf");
        assert f.delete();
        ArchiveEntryParameters p = ArchiveEntryParameters.fromFile(f);
        assertEquals(-1, p.size());
    }

    @Test
    public void getModeConstructsModeFromPermissions() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withPermissions(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        assertEquals("100700", Long.toString(p.getMode().get(), 8));
    }

    @Test
    public void getPermissionsConstructsPermissionsFromMode() {
        ArchiveEntryParameters p = new ArchiveEntryParameters()
            .withMode(0100753l);
        Set<PosixFilePermission> s = p.getPermissions().get();
        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
                                PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_WRITE,
                                PosixFilePermission.OTHERS_EXECUTE),
                     s);
    }

    private static void assertWithinTwoSecondsOf(Instant expected, FileTime actual) {
        assert Math.abs(Duration.between(expected, actual.toInstant()).getSeconds()) < 2;
    }
}
