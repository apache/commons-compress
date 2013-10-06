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
import java.util.regex.Matcher;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipUtil;

/**
 * An entry in an ARJ archive.
 * 
 * @NotThreadSafe
 */
public class ArjArchiveEntry implements ArchiveEntry {
    private final LocalFileHeader localFileHeader;
    
    public ArjArchiveEntry() {
        localFileHeader = new LocalFileHeader();
    }
    
    ArjArchiveEntry(final LocalFileHeader localFileHeader) {
        this.localFileHeader = localFileHeader;
    }

    public String getName() {
        if ((localFileHeader.arjFlags & LocalFileHeader.Flags.PATHSYM) != 0) {
            return localFileHeader.name.replaceAll("/",
                    Matcher.quoteReplacement(File.separator));
        } else {
            return localFileHeader.name;
        }
    }

    public long getSize() {
        return localFileHeader.originalSize;
    }

    public boolean isDirectory() {
        return localFileHeader.fileType == LocalFileHeader.FileTypes.DIRECTORY;
    }

    public Date getLastModifiedDate() {
        long ts = isHostOsUnix() ? (localFileHeader.dateTimeModified * 1000l)
            : ZipUtil.dosToJavaTime(0xFFFFFFFFL & localFileHeader.dateTimeModified);
        return new Date(ts);
    }

    /**
     * File mode of this entry.
     *
     * <p>The format depends on the host os that created the entry.</p>
     */
    public int getMode() {
        return localFileHeader.fileAccessMode;
    }

    /**
     * File mode of this entry as Unix stat value.
     *
     * <p>Will only be non-zero of the host os was UNIX.
     */
    public int getUnixMode() {
        return isHostOsUnix() ? getMode() : 0;
    }

    /**
     * The operating system the archive has been created on.
     * @see HostOs
     */
    public int getHostOs() {
        return localFileHeader.hostOS;
    }

    /**
     * Is the operating system the archive has been created on one
     * that is considered a UNIX OS by arj?
     */
    public boolean isHostOsUnix() {
        return getHostOs() == HostOs.UNIX || getHostOs() == HostOs.NEXT;
    }

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
    
}
