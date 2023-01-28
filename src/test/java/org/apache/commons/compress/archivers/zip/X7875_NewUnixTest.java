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
package org.apache.commons.compress.archivers.zip;

import static org.apache.commons.compress.AbstractTestCase.getFile;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipException;

import org.apache.commons.compress.utils.ByteUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class X7875_NewUnixTest {

    private final static ZipShort X7875 = new ZipShort(0x7875);

    private static byte[] trimTest(final byte[] b) { return X7875_NewUnix.trimLeadingZeroesForceMinLength(b); }

    private X7875_NewUnix xf;


    @BeforeEach
    public void before() {
        xf = new X7875_NewUnix();
    }

    private void parseReparse(
            final long uid,
            final long gid,
            final byte[] expected,
            final long expectedUID,
            final long expectedGID
    ) throws ZipException {

        // Initial local parse (init with garbage to avoid defaults causing test to pass).
        xf.setUID(54321);
        xf.setGID(12345);
        xf.parseFromLocalFileData(expected, 0, expected.length);
        assertEquals(expectedUID, xf.getUID());
        assertEquals(expectedGID, xf.getGID());

        xf.setUID(uid);
        xf.setGID(gid);
        if (expected.length < 5) {
            // We never emit zero-length entries.
            assertEquals(5, xf.getLocalFileDataLength().getValue());
        } else {
            assertEquals(expected.length, xf.getLocalFileDataLength().getValue());
        }
        byte[] result = xf.getLocalFileDataData();
        if (expected.length < 5) {
            // We never emit zero-length entries.
            assertArrayEquals(new byte[]{1, 1, 0, 1, 0}, result);
        } else {
            assertArrayEquals(expected, result);
        }



        // And now we re-parse:
        xf.parseFromLocalFileData(result, 0, result.length);

        // Did uid/gid change from re-parse?  They shouldn't!
        assertEquals(expectedUID, xf.getUID());
        assertEquals(expectedGID, xf.getGID());

        assertEquals(0, xf.getCentralDirectoryLength().getValue());
        result = xf.getCentralDirectoryData();
        assertArrayEquals(ByteUtils.EMPTY_BYTE_ARRAY, result);

        // And now we re-parse:
        xf.parseFromCentralDirectoryData(result, 0, result.length);

        // Did uid/gid change from 2nd re-parse?  They shouldn't!
        assertEquals(expectedUID, xf.getUID());
        assertEquals(expectedGID, xf.getGID());
    }

    @Test
    public void testGetHeaderId() {
        assertEquals(X7875, xf.getHeaderId());
    }

    @Test
    public void testMisc() throws Exception {
        assertNotEquals(xf, new Object());
        assertTrue(xf.toString().startsWith("0x7875 Zip Extra Field"));
        final Object o = xf.clone();
        assertEquals(o.hashCode(), xf.hashCode());
        assertEquals(xf, o);
        xf.setUID(12345);
        assertNotEquals(xf, o);
    }

    @Test
    public void testParseReparse() throws ZipException {

        // Version=1, Len=0, Len=0.
        final byte[] ZERO_LEN = {1, 0, 0};

        // Version=1, Len=1, zero, Len=1, zero.
        final byte[] ZERO_UID_GID = {1, 1, 0, 1, 0};

        // Version=1, Len=1, one, Len=1, one
        final byte[] ONE_UID_GID = {1, 1, 1, 1, 1};

        // Version=1, Len=2, one thousand, Len=2, one thousand
        final byte[] ONE_THOUSAND_UID_GID = {1, 2, -24, 3, 2, -24, 3};

        // (2^32 - 2).   I guess they avoid (2^32 - 1) since it's identical to -1 in
        // two's complement, and -1 often has a special meaning.
        final byte[] UNIX_MAX_UID_GID = {1, 4, -2, -1, -1, -1, 4, -2, -1, -1, -1};

        // Version=1, Len=5, 2^32, Len=5, 2^32 + 1
        // Esoteric test:  can we handle 40 bit numbers?
        final byte[] LENGTH_5 = {1, 5, 0, 0, 0, 0, 1, 5, 1, 0, 0, 0, 1};

        // Version=1, Len=8, 2^63 - 2, Len=8, 2^63 - 1
        // Esoteric test:  can we handle 64 bit numbers?
        final byte[] LENGTH_8 = {1, 8, -2, -1, -1, -1, -1, -1, -1, 127, 8, -1, -1, -1, -1, -1, -1, -1, 127};

        final long TWO_TO_32 = 0x100000000L;
        final long MAX = TWO_TO_32 - 2;

        parseReparse(0, 0, ZERO_LEN, 0, 0);
        parseReparse(0, 0, ZERO_UID_GID, 0, 0);
        parseReparse(1, 1, ONE_UID_GID, 1, 1);
        parseReparse(1000, 1000, ONE_THOUSAND_UID_GID, 1000, 1000);
        parseReparse(MAX, MAX, UNIX_MAX_UID_GID, MAX, MAX);
        parseReparse(-2, -2, UNIX_MAX_UID_GID, MAX, MAX);
        parseReparse(TWO_TO_32, TWO_TO_32 + 1, LENGTH_5, TWO_TO_32, TWO_TO_32 + 1);
        parseReparse(Long.MAX_VALUE - 1, Long.MAX_VALUE, LENGTH_8, Long.MAX_VALUE - 1, Long.MAX_VALUE);

        // We never emit this, but we should be able to parse it:
        final byte[] SPURIOUS_ZEROES_1 = {1, 4, -1, 0, 0, 0, 4, -128, 0, 0, 0};
        final byte[] EXPECTED_1 = {1, 1, -1, 1, -128};
        xf.parseFromLocalFileData(SPURIOUS_ZEROES_1, 0, SPURIOUS_ZEROES_1.length);

        assertEquals(255, xf.getUID());
        assertEquals(128, xf.getGID());
        assertArrayEquals(EXPECTED_1, xf.getLocalFileDataData());

        final byte[] SPURIOUS_ZEROES_2 = {1, 4, -1, -1, 0, 0, 4, 1, 2, 0, 0};
        final byte[] EXPECTED_2 = {1, 2, -1, -1, 2, 1, 2};
        xf.parseFromLocalFileData(SPURIOUS_ZEROES_2, 0, SPURIOUS_ZEROES_2.length);

        assertEquals(65535, xf.getUID());
        assertEquals(513, xf.getGID());
        assertArrayEquals(EXPECTED_2, xf.getLocalFileDataData());
    }

    @Test
    public void testSampleFile() throws Exception {
        final File archive = getFile("COMPRESS-211_uid_gid_zip_test.zip");

        try (ZipFile zf = new ZipFile(archive)) {
            final Enumeration<ZipArchiveEntry> en = zf.getEntries();

            // We expect EVERY entry of this ZIP file (dir & file) to
            // contain extra field 0x7875.
            while (en.hasMoreElements()) {

                final ZipArchiveEntry zae = en.nextElement();
                final String name = zae.getName();
                final X7875_NewUnix xf = (X7875_NewUnix) zae.getExtraField(X7875);

                // The directory entry in the test ZIP file is uid/gid 1000.
                long expected = 1000;
                if (name.contains("uid555_gid555")) {
                    expected = 555;
                } else if (name.contains("uid5555_gid5555")) {
                    expected = 5555;
                } else if (name.contains("uid55555_gid55555")) {
                    expected = 55555;
                } else if (name.contains("uid555555_gid555555")) {
                    expected = 555555;
                } else if (name.contains("min_unix")) {
                    expected = 0;
                } else if (name.contains("max_unix")) {
                    // 2^32-2 was the biggest UID/GID I could create on my Linux!
                    // (December 2012, Linux kernel 3.4)
                    expected = 0x100000000L - 2;
                }
                assertEquals(expected, xf.getUID());
                assertEquals(expected, xf.getGID());
            }
        }
    }


    @Test
    public void testTrimLeadingZeroesForceMinLength4() {
        final byte[] NULL = null;
        final byte[] EMPTY = ByteUtils.EMPTY_BYTE_ARRAY;
        final byte[] ONE_ZERO = {0};
        final byte[] TWO_ZEROES = {0, 0};
        final byte[] FOUR_ZEROES = {0, 0, 0, 0};
        final byte[] SEQUENCE = {1, 2, 3};
        final byte[] SEQUENCE_LEADING_ZERO = {0, 1, 2, 3};
        final byte[] SEQUENCE_LEADING_ZEROES = {0, 0, 0, 0, 0, 0, 0, 1, 2, 3};
        final byte[] TRAILING_ZERO = {1, 2, 3, 0};
        final byte[] PADDING_ZERO = {0, 1, 2, 3, 0};
        final byte[] SEQUENCE6 = {1, 2, 3, 4, 5, 6};
        final byte[] SEQUENCE6_LEADING_ZERO = {0, 1, 2, 3, 4, 5, 6};

        assertSame(NULL, trimTest(NULL));
        assertArrayEquals(ONE_ZERO, trimTest(EMPTY));
        assertArrayEquals(ONE_ZERO, trimTest(ONE_ZERO));
        assertArrayEquals(ONE_ZERO, trimTest(TWO_ZEROES));
        assertArrayEquals(ONE_ZERO, trimTest(FOUR_ZEROES));
        assertArrayEquals(SEQUENCE, trimTest(SEQUENCE));
        assertArrayEquals(SEQUENCE, trimTest(SEQUENCE_LEADING_ZERO));
        assertArrayEquals(SEQUENCE, trimTest(SEQUENCE_LEADING_ZEROES));
        assertArrayEquals(TRAILING_ZERO, trimTest(TRAILING_ZERO));
        assertArrayEquals(TRAILING_ZERO, trimTest(PADDING_ZERO));
        assertArrayEquals(SEQUENCE6, trimTest(SEQUENCE6));
        assertArrayEquals(SEQUENCE6, trimTest(SEQUENCE6_LEADING_ZERO));
    }
}
