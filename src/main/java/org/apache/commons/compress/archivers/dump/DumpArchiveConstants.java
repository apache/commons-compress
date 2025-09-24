/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.dump;

/**
 * Various constants associated with dump archives.
 *
 * @since 1.3
 */
public final class DumpArchiveConstants {

    /**
     * Enumerates compression types.
     */
    public enum COMPRESSION_TYPE {

        /**
         * Compression code is -1.
         */
        UNKNOWN(-1),

        /**
         * Compression code is 0.
         */
        ZLIB(0),

        /**
         * Compression code is 1.
         */
        BZLIB(1),

        /**
         * Compression code is 2.
         */
        LZO(2);

        /**
         * Finds the matching enumeration value for the given code.
         *
         * @param code a code.
         * @return a value, never null.
         */
        public static COMPRESSION_TYPE find(final int code) {
            for (final COMPRESSION_TYPE e : values()) {
                if (e.code == code) {
                    return e;
                }
            }
            return UNKNOWN;
        }

        final int code;

        COMPRESSION_TYPE(final int code) {
            this.code = code;
        }
    }

    /**
     * Enumerates the types of tape segment.
     */
    public enum SEGMENT_TYPE {

        /**
         * TAPE with code 1.
         */
        TAPE(1),

        /**
         * INODE with code 2.
         */
        INODE(2),

        /**
         * BITS with code 3.
         */
        BITS(3),

        /**
         * ADDR with code 4.
         */
        ADDR(4),

        /**
         * END with code 5.
         */
        END(5),

        /**
         * CLRI with code 6.
         */
        CLRI(6);

        /**
         * Finds the matching enumeration value for the given code.
         *
         * @param code a code.
         * @return a value, or null if not found.
         */
        public static SEGMENT_TYPE find(final int code) {
            for (final SEGMENT_TYPE e : values()) {
                if (e.code == code) {
                    return e;
                }
            }
            return null;
        }

        public final int code;

        SEGMENT_TYPE(final int code) {
            this.code = code;
        }
    }

    /**
     * TP_SIZE value {@value}.
     */
    public static final int TP_SIZE = 1024;

    /**
     * Minimum number of kilobytes per record: {@value}.
     */
    static final int MIN_NTREC = 1;

    /**
     * Default number of kilobytes per record: {@value}.
     */
    public static final int NTREC = 10;

    /**
     * Number of kilobytes per record for high density tapes: {@value}.
     */
    public static final int HIGH_DENSITY_NTREC = 32;

    /**
     * Maximum number of kilobytes per record: {@value}.
     * <p>
     *     This limit matches the one used by the Linux ext2/3/4 dump/restore utilities.
     *     For more details, see the
     *     <a href="https://dump.sourceforge.io/">Linux dump/restore utilities documentation</a>
     *     and the
     *     <a href="https://manpages.debian.org/unstable/dump/dump.8.en.html#b,">dump(8) man page</a>.
     * </p>
     */
    static final int MAX_NTREC = 1024;

    /**
     * OFS_MAGIC value {@value}.
     */
    public static final int OFS_MAGIC = 60011;

    /**
     * NFS_MAGIC value {@value}.
     */
    public static final int NFS_MAGIC = 60012;

    /**
     * FS_UFS2_MAGIC value {@value}.
     */
    public static final int FS_UFS2_MAGIC = 0x19540119;

    /**
     * CHECKSUM value {@value}.
     */
    public static final int CHECKSUM = 84446;

    /**
     * LBLSIZE value {@value}.
     */
    public static final int LBLSIZE = 16;

    /**
     * NAMELEN value {@value}.
     */
    public static final int NAMELEN = 64;

    /** Do not instantiate. */
    private DumpArchiveConstants() {
    }
}
