/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArchiveUtilsTest extends AbstractTestCase {

    private static final int bytesToTest = 50;
    private static final byte[] byteTest = new byte[bytesToTest];
    static {
        for(int i=0; i < byteTest.length ;) {
            byteTest[i]=(byte) i;
            byteTest[i+1]=(byte) -i;
            i += 2;
        }
    }

    @Test
    public void testCompareBA(){
        final byte[] buffer1 = {1,2,3};
        final byte[] buffer2 = {1,2,3,0};
        final byte[] buffer3 = {1,2,3};
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
    public void testCompareAscii(){
        final byte[] buffer1 = {'a','b','c'};
        final byte[] buffer2 = {'d','e','f',0};
        assertTrue(ArchiveUtils.matchAsciiBuffer("abc", buffer1));
        assertFalse(ArchiveUtils.matchAsciiBuffer("abc\0", buffer1));
        assertTrue(ArchiveUtils.matchAsciiBuffer("def\0", buffer2));
        assertFalse(ArchiveUtils.matchAsciiBuffer("def", buffer2));
    }

    @Test
    public void testAsciiConversions() {
        asciiToByteAndBackOK("");
        asciiToByteAndBackOK("abcd");
        asciiToByteAndBackFail("\u8025");
    }

    @Test
    public void sanitizeShortensString() {
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
        assertEquals(expected, ArchiveUtils.sanitize(input));
    }

    @Test
    public void sanitizeLeavesShortStringsAlone() {
        final String input = "012345678901234567890123456789012345678901234567890123456789";
        assertEquals(input, ArchiveUtils.sanitize(input));
    }

    @Test
    public void sanitizeRemovesUnprintableCharacters() {
        final String input = "\b12345678901234567890123456789012345678901234567890123456789";
        final String expected = "?12345678901234567890123456789012345678901234567890123456789";
        assertEquals(expected, ArchiveUtils.sanitize(input));
    }

    @Test
    public void testIsEqualWithNullWithPositive() {

        byte[] byteArray = new byte[8];
        byteArray[1] = (byte) (-77);

        assertFalse(ArchiveUtils.isEqualWithNull(byteArray, 0, (byte)0, byteArray, (byte)0, (byte)80));

    }

    @Test
    public void testToAsciiBytes() {

        byte[] byteArray = ArchiveUtils.toAsciiBytes("SOCKET");

        assertArrayEquals(new byte[] {(byte)83, (byte)79, (byte)67, (byte)75, (byte)69, (byte)84}, byteArray);

        assertFalse(ArchiveUtils.isEqualWithNull(byteArray, 0, 46, byteArray, 63, 0));

    }

    @Test
    public void testToStringWithNonNull() {

        SevenZArchiveEntry sevenZArchiveEntry = new SevenZArchiveEntry();
        String string = ArchiveUtils.toString(sevenZArchiveEntry);

        assertEquals("-       0 null", string);

    }

    @Test
    public void testIsEqual() {

        assertTrue(ArchiveUtils.isEqual((byte[]) null, 0, 0, (byte[]) null, 0, 0));

    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testToAsciiStringThrowsStringIndexOutOfBoundsException() {

        byte[] byteArray = new byte[3];

        ArchiveUtils.toAsciiString(byteArray, 940, 2730);

    }

    private void asciiToByteAndBackOK(final String inputString) {
        assertEquals(inputString, ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString)));
    }

    private void asciiToByteAndBackFail(final String inputString) {
        assertFalse(inputString.equals(ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString))));
    }
}
