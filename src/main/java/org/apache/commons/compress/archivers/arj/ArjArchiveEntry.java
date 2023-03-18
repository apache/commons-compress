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
package org.apache.commons.compress.archivers.arj;

import java.io.File;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipUtil;

/**
 * An entry in an ARJ archive.
 *
 * @NotThreadSafe
 * @since 1.6
 */
public class ArjArchiveEntry implements ArchiveEntry {
    /**
     * The known values for HostOs.
     */
    public static class HostOs {
        public static final int DOS = 0;
        public static final int PRIMOS = 1;
        public static final int UNIX = 2;
        public static final int AMIGA = 3;
        public static final int MAC_OS = 4;
        public static final int OS_2 = 5;
        public static final int APPLE_GS = 6;
        public static final int ATARI_ST = 7;
        public static final int NEXT = 8;
        public static final int VAX_VMS = 9;
        public static final int WIN95 = 10;
        public static final int WIN32 = 11;
    }

    private final LocalFileHeader localFileHeader;

    public ArjArchiveEntry() {
        localFileHeader = new LocalFileHeader();
    }

    ArjArchiveEntry(final LocalFileHeader localFileHeader) {
        this.localFileHeader = localFileHeader;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ArjArchiveEntry other = (ArjArchiveEntry) obj;
        return localFileHeader.equals(other.localFileHeader);
    }

    /**
     * The operating system the archive has been created on.
     * @see HostOs
     * @return the host OS code
     */
    public int getHostOs() {
        return localFileHeader.hostOS;
    }

    /**
     * The last modified date of the entry.
     *
     * <p>Note the interpretation of time is different depending on
     * the HostOS that has created the archive.  While an OS that is
     * {@link #isHostOsUnix considered to be Unix} stores time in a
     * time zone independent manner, other platforms only use the local
     * time.  I.e. if an archive has been created at midnight UTC on a
     * machine in time zone UTC this method will return midnight
     * regardless of time zone if the archive has been created on a
     * non-Unix system and a time taking the current time zone into
     * account if the archive has been created on Unix.</p>
     *
     * @return the last modified date
     */
    @Override
    public Date getLastModifiedDate() {
        final long ts = isHostOsUnix() ? localFileHeader.dateTimeModified * 1000L
            : ZipUtil.dosToJavaTime(0xFFFFFFFFL & localFileHeader.dateTimeModified);
        return new Date(ts);
    }

    int getMethod() {
        return localFileHeader.method;
    }

    /**
     * File mode of this entry.
     *
     * <p>The format depends on the host os that created the entry.</p>
     *
     * @return the file mode
     */
    public int getMode() {
        return localFileHeader.fileAccessMode;
    }

    /**
     * Get this entry's name.
     *
     * <p>This method returns the raw name as it is stored inside of the archive.</p>
     *
     * @return This entry's name.
     */
    @Override
    public String getName() {
        if ((localFileHeader.arjFlags & LocalFileHeader.Flags.PATHSYM) != 0) {
            return localFileHeader.name.replace("/",
                    File.separator);
        }
        return localFileHeader.name;
    }

    /**
     * Get this entry's file size.
     *
     * @return This entry's file size.
     */
    @Override
    public long getSize() {
        return localFileHeader.originalSize;
    }

    /**
     * File mode of this entry as Unix stat value.
     *
     * <p>Will only be non-zero of the host os was UNIX.
     *
     * @return the Unix mode
     */
    public int getUnixMode() {
        return isHostOsUnix() ? getMode() : 0;
    }

    @Override
    public int hashCode() {
        final String name = getName();
        return name == null ? 0 : name.hashCode();
    }

    /** True if the entry refers to a directory.
     *
     * @return True if the entry refers to a directory
     */
    @Override
    public boolean isDirectory() {
        return localFileHeader.fileType == LocalFileHeader.FileTypes.DIRECTORY;
    }

    /**
     * Is the operating system the archive has been created on one
     * that is considered a UNIX OS by arj?
     * @return whether the operating system the archive has been
     * created on is considered a UNIX OS by arj
     */
    public boolean isHostOsUnix() {
        return getHostOs() == HostOs.UNIX || getHostOs() == HostOs.NEXT;
    }

}
