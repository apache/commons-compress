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
        return new Date(ZipUtil.dosToJavaTime(
                0xffffFFFFL & localFileHeader.dateTimeModified));
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
        return getHostOs() == HostOs.UNIX ? getMode() : 0;
    }

    /**
     * The operating system the archive has been created on.
     * @see HostOs
     */
    public int getHostOs() {
        return localFileHeader.hostOS;
    }

    /**
     * The known values for HostOs.
     */
    public static class HostOs {
        static final int DOS = 0;
        static final int PRIMOS = 1;
        static final int UNIX = 2;
        static final int AMIGA = 3;
        static final int MAC_OS = 4;
        static final int OS_2 = 5;
        static final int APPLE_GS = 6;
        static final int ATARI_ST = 7;
        static final int NEXT = 8;
        static final int VAX_VMS = 9;
        static final int WIN95 = 10;
        static final int WIN32 = 11;
    }
    
}
