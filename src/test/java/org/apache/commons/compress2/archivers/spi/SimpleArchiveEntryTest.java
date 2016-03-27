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
package org.apache.commons.compress2.archivers.spi;

import static org.junit.Assert.assertEquals;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.junit.Test;

public class SimpleArchiveEntryTest {

    @Test
    public void nullLastModifiedIsTranslatedToEpoch() {
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters());
        assertEquals(0l, e.lastModifiedTime().toMillis());
    }

    @Test
    public void lastModifiedIsUsedWhenPresent() {
        FileTime d = FileTime.from(Instant.now());
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withLastModifiedTime(d));
        assertEquals(d, e.lastModifiedTime());
    }

    @Test
    public void nullLastAccessUsesLastModified() {
        FileTime d = FileTime.from(Instant.now());
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withLastModifiedTime(d));
        assertEquals(d, e.lastAccessTime());
    }

    @Test
    public void lastAccessIsUsedWhenPresent() {
        FileTime d1 = FileTime.from(Instant.now());
        FileTime d2 = FileTime.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withLastModifiedTime(d1)
                                                      .withLastAccessTime(d2));
        assertEquals(d2, e.lastAccessTime());
    }

    @Test
    public void nullCreationUsesLastModified() {
        FileTime d = FileTime.from(Instant.now());
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withLastModifiedTime(d));
        assertEquals(d, e.creationTime());
    }

    @Test
    public void creationIsUsedWhenPresent() {
        FileTime d1 = FileTime.from(Instant.now());
        FileTime d2 = FileTime.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withLastModifiedTime(d1)
                                                      .withCreationTime(d2));
        assertEquals(d2, e.creationTime());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void permissionSetIsImmutable() {
        SimpleArchiveEntry e = new SimpleArchiveEntry(new ArchiveEntryParameters()
                                                      .withPermissions(EnumSet.of(PosixFilePermission.OWNER_READ)));
        e.getPermissions().get().clear();
    }

}
