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

package org.apache.commons.compress;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.junit.jupiter.api.Test;

class ArchiveUtilsTest extends AbstractTest {

    private static final int bytesToTest = 50;
    private static final byte[] byteTest = new byte[bytesToTest];

    static {
        for (int i = 0; i < byteTest.length;) {
            byteTest[i] = (byte) i;
            byteTest[i + 1] = (byte) -i;
            i += 2;
        }
    }

    private void asciiToByteAndBackFail(final String inputString) {
        assertNotEquals(inputString, ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString)));
    }

    private void asciiToByteAndBackOK(final String inputString) {
        assertEquals(inputString, ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString)));
    }

    @Test
    void testAsciiConversions() {
        asciiToByteAndBackOK("");
        asciiToByteAndBackOK("abcd");
        asciiToByteAndBackFail("\u8025");
    }

    @Test
    void testCompareAscii() {
        final byte[] buffer1 = { 'a', 'b', 'c' };
        final byte[] buffer2 = { 'd', 'e', 'f', 0 };
        assertTrue(ArchiveUtils.matchAsciiBuffer("abc", buffer1));
        assertFalse(ArchiveUtils.matchAsciiBuffer("abc\0", buffer1));
        assertTrue(ArchiveUtils.matchAsciiBuffer("def\0", buffer2));
        assertFalse(ArchiveUtils.matchAsciiBuffer("def", buffer2));
    }

    @Test
    void testCompareBA() {
        final byte[] buffer1 = { 1, 2, 3 };
        final byte[] buffer2 = { 1, 2, 3, 0 };
        final byte[] buffer3 = { 1, 2, 3 };
        assertTrue(ArchiveUtils.isEqual(buffer1, buffer2, true));
        assertFalse(ArchiveUtils.isEqual(buffer1, buffer2, false));
        assertFalse(ArchiveUtils.isEqual(buffer1, buffer2));
        assertTrue(ArchiveUtils.isEqual(buffer2, buffer1, true));
        assertFalse(ArchiveUtils.isEqual(buffer2, buffer1, false));
        assertFalse(ArchiveUtils.isEqual(buffer2, buffer1));
        assertTrue(ArchiveUtils.isEqual(buffer1, buffer3));
        assertTrue(ArchiveUtils.isEqual(buffer3, buffer1));
    }

    @Test
    void testIsEqual() {
        assertTrue(ArchiveUtils.isEqual(null, 0, 0, null, 0, 0));
    }

    @Test
    void testIsEqualWithNullWithPositive() {
        final byte[] byteArray = new byte[8];
        byteArray[1] = (byte) -77;
        assertFalse(ArchiveUtils.isEqualWithNull(byteArray, 0, (byte) 0, byteArray, (byte) 0, (byte) 80));
    }

    @Test
    void testSanitizeLeavesShortStringsAlone() {
        final String input = "012345678901234567890123456789012345678901234567890123456789";
        assertEquals(input, ArchiveUtils.sanitize(input));
    }

    @Test
    void testSanitizeRemovesUnprintableCharacters() {
        final String input = "\b12345678901234567890123456789012345678901234567890123456789";
        final String expected = "?12345678901234567890123456789012345678901234567890123456789";
        assertEquals(expected, ArchiveUtils.sanitize(input));
    }

    @Test
    void testSanitizeShortensString() {
        // @formatter:off
        final String input = "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789";
        final String expected = "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901234567890123456789012345678901234567890123456789"
            + "012345678901...";
        // @formatter:on
        assertEquals(expected, ArchiveUtils.sanitize(input));
    }

    @Test
    void testToAsciiBytes() {
        final byte[] byteArray = ArchiveUtils.toAsciiBytes("SOCKET");
        assertArrayEquals(new byte[] { (byte) 83, (byte) 79, (byte) 67, (byte) 75, (byte) 69, (byte) 84 }, byteArray);
        assertFalse(ArchiveUtils.isEqualWithNull(byteArray, 0, 46, byteArray, 63, 0));
    }

    @Test
    void testToAsciiStringThrowsStringIndexOutOfBoundsException() {
        final byte[] byteArray = new byte[3];
        assertThrows(StringIndexOutOfBoundsException.class, () -> ArchiveUtils.toAsciiString(byteArray, 940, 2730));
    }

    @Test
    void testToStringWithNonNull() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        final String string = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("-       0 null", string);
    }

    @Test
    public void testisArrayZeroWithOutOFBoundArray() {
        final byte[] buffer3 = { 0, 0, 0 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArchiveUtils.isArrayZero(buffer3, 4));
    }

    @Test
    public void testisArrayZeroWithZeroArrayMaxSize() {
        final byte[] buffer = { 0, 0, 0 };
        final int size = 3;
        assertTrue(ArchiveUtils.isArrayZero(buffer, size));
    }

    @Test
    public void testisArrayZeroWithZeroArrayMinSize() {
        final byte[] buffer = { 0, 0, 0 };
        final int size = 0;
        assertTrue(ArchiveUtils.isArrayZero(buffer, size));
    }

    @Test
    public void testisArrayZeroWithNotZeroArrayMinSize() {
        final byte[] buffer = { 1, 2, 3 };
        final int size = 0;
        assertTrue(ArchiveUtils.isArrayZero(buffer, size));
    }

    @Test
    public void testisArrayZeroWithNotZeroArrayMiddleSize() {
        final byte[] buffer = { 1, 2, 3 };
        final int size = 2;
        assertFalse(ArchiveUtils.isArrayZero(buffer, size));
    }

    @Test
    public void testisArrayZeroWithZeroArrayMiddleSize() {
        final byte[] buffer = { 0, 0, 0 };
        final int size = 2;
        assertTrue(ArchiveUtils.isArrayZero(buffer, size));
    }

    @Test
    void testToStringisDirectory() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setDirectory(true);
        sevenZArchiveEntry.setName("test");
        sevenZArchiveEntry.setSize(1234567L);
        final String result = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("d 1234567 test", result);
    }

    @Test
    void testToStringisNotDirectorySizelessThanSeven() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setDirectory(false);
        sevenZArchiveEntry.setName("test");
        sevenZArchiveEntry.setSize(123L);
        final String result = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("-     123 test", result);
    }

    @Test
    void testToStringisNotDirectorySizeisSeven() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setDirectory(false);
        sevenZArchiveEntry.setName("test");
        sevenZArchiveEntry.setSize(1234567L);
        final String result = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("- 1234567 test", result);
    }

    @Test
    void testToStringisNotDirectorySizeMoreThanSeven() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setDirectory(false);
        sevenZArchiveEntry.setName("test");
        sevenZArchiveEntry.setSize(12345678L);
        final String result = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("- 12345678 test", result);
    }

    @Test
    void testToStringisNotDirectoryZeroSize() {
        final SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        sevenZArchiveEntry.setDirectory(false);
        sevenZArchiveEntry.setName("test");
        sevenZArchiveEntry.setSize(0L);
        final String result = ArchiveUtils.toString(sevenZArchiveEntry);
        assertEquals("-       0 test", result);
    }
}
