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

import org.apache.commons.compress2.archivers.ArchiveEntryParameters;
import org.apache.commons.compress2.archivers.spi.SimpleArchiveEntry;

/**
 * Represents an archive entry in the "ar" format.
 * 
 * Each AR archive starts with "!&lt;arch&gt;" followed by a LF. After these 8 bytes
 * the archive entries are listed. The format of an entry header is as it follows:
 * 
 * <pre>
 * START BYTE   END BYTE    NAME                    FORMAT      LENGTH
 * 0            15          File name               ASCII       16
 * 16           27          Modification timestamp  Decimal     12
 * 28           33          Owner ID                Decimal     6
 * 34           39          Group ID                Decimal     6
 * 40           47          File mode               Octal       8
 * 48           57          File size (bytes)       Decimal     10
 * 58           59          File magic              \140\012    2
 * </pre>
 * 
 * This specifies that an ar archive entry header contains 60 bytes.
 * 
 * Due to the limitation of the file name length to 16 bytes GNU and
 * BSD has their own variants of this format. Currently Commons
 * Compress can read but not write the GNU variant.  It fully supports
 * the BSD variant.
 * 
 * @see <a href="http://www.freebsd.org/cgi/man.cgi?query=ar&sektion=5">ar man page</a>
 *
 * @Immutable
 */
public class ArArchiveEntry extends SimpleArchiveEntry {

    /** The header for each entry */
    public static final String HEADER = "!<arch>\n";

    /** The trailer for each entry */
    public static final String TRAILER = "`\012";

    /**
     * The default mode (a world readable, owner writable regular file).
     */
    public static final long DEFAULT_MODE = 0100644l;

    /**
     * Creates an ArArchiveEntry from a parameter object.
     * @param params the parameters describing the archive entry.
     */
    public ArArchiveEntry(ArchiveEntryParameters params) {
        super(params);
    }

}
