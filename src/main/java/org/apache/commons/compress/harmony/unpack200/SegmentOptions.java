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
package org.apache.commons.compress.harmony.unpack200;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Stores the combinations of bit flags that can be used in the segment header options. Whilst this could be defined in {@link Segment}, it's cleaner to pull it
 * out into a separate class, not least because methods can then be used to determine the semantic meaning of the flags. In languages with a pre-processor,
 * these may be defined by macros that do bitflag manipulation instead.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class SegmentOptions {

    private static final int DEFLATE_HINT = 1 << 5;

    private static final int HAVE_ALL_CODE_FLAGS = 1 << 2;

    private static final int HAVE_CLASS_FLAGS_HI = 1 << 9;

    // private static final int UNUSED_3 = 2^3;

    private static final int HAVE_CODE_FLAGS_HI = 1 << 10;

    private static final int HAVE_CP_NUMBERS = 1 << 1;

    private static final int HAVE_FIELD_FLAGS_HI = 1 << 10;

    private static final int HAVE_FILE_HEADERS = 1 << 4;

    private static final int HAVE_FILE_MODTIME = 1 << 6;

    private static final int HAVE_FILE_OPTIONS = 1 << 7;

    private static final int HAVE_FILE_SIZE_HI = 1 << 8;

    private static final int HAVE_METHOD_FLAGS_HI = 1 << 11;

    private static final int HAVE_SPECIAL_FORMATS = 1 << 0;

    /**
     * The bit flags that are defined as unused by the specification; specifically, every bit above bit 13 and bit 3.
     */
    private static final int UNUSED = -1 << 13 | 1 << 3;

    private final int options;

    /**
     * Creates a new segment options with the given integer value.
     *
     * @param options the integer value to use as the flags
     * @throws Pack200Exception if an unused bit (bit 3 or bit 13+) is non-zero
     */
    public SegmentOptions(final int options) throws Pack200Exception {
        if ((options & UNUSED) != 0) {
            throw new Pack200Exception("Some unused flags are non-zero");
        }
        this.options = options;
    }

    /**
     * Tests whether all code flags are present.
     *
     * @return true if all code flags are present.
     */
    public boolean hasAllCodeFlags() {
        return (options & HAVE_ALL_CODE_FLAGS) != 0;
    }

    /**
     * Tests whether archive file counts are present.
     *
     * @return true if archive file counts are present.
     */
    public boolean hasArchiveFileCounts() {
        return (options & HAVE_FILE_HEADERS) != 0;
    }

    /**
     * Tests whether class flags hi are present.
     *
     * @return true if class flags hi are present.
     */
    public boolean hasClassFlagsHi() {
        return (options & HAVE_CLASS_FLAGS_HI) != 0;
    }

    /**
     * Tests whether code flags hi are present.
     *
     * @return true if code flags hi are present.
     */
    public boolean hasCodeFlagsHi() {
        return (options & HAVE_CODE_FLAGS_HI) != 0;
    }

    /**
     * Tests whether CP number counts are present.
     *
     * @return true if CP number counts are present.
     */
    public boolean hasCPNumberCounts() {
        return (options & HAVE_CP_NUMBERS) != 0;
    }

    /**
     * Tests whether field flags hi are present.
     *
     * @return true if field flags hi are present.
     */
    public boolean hasFieldFlagsHi() {
        return (options & HAVE_FIELD_FLAGS_HI) != 0;
    }

    /**
     * Tests whether file modtime is present.
     *
     * @return true if file modtime is present.
     */
    public boolean hasFileModtime() {
        return (options & HAVE_FILE_MODTIME) != 0;
    }

    /**
     * Tests whether file options are present.
     *
     * @return true if file options are present.
     */
    public boolean hasFileOptions() {
        return (options & HAVE_FILE_OPTIONS) != 0;
    }

    /**
     * Tests whether file size hi is present.
     *
     * @return true if file size hi is present.
     */
    public boolean hasFileSizeHi() {
        return (options & HAVE_FILE_SIZE_HI) != 0;
    }

    /**
     * Tests whether method flags hi are present.
     *
     * @return true if method flags hi are present.
     */
    public boolean hasMethodFlagsHi() {
        return (options & HAVE_METHOD_FLAGS_HI) != 0;
    }

    /**
     * Tests whether special formats are present.
     *
     * @return true if special formats are present.
     */
    public boolean hasSpecialFormats() {
        return (options & HAVE_SPECIAL_FORMATS) != 0;
    }

    /**
     * Tests whether deflate should be used.
     *
     * @return true if deflate should be used.
     */
    public boolean shouldDeflate() {
        return (options & DEFLATE_HINT) != 0;
    }
}
