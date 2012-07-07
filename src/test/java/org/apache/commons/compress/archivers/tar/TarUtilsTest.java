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

package org.apache.commons.compress.archivers.tar;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.CharsetNames;

public class TarUtilsTest extends TestCase {


    public void testName(){
        byte [] buff = new byte[20];
        String sb1 = "abcdefghijklmnopqrstuvwxyz";
        int off = TarUtils.formatNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 20);
        String sb2 = TarUtils.parseName(buff, 1, 10);
        assertEquals(sb2,sb1.substring(0,10));
        sb2 = TarUtils.parseName(buff, 1, 19);
        assertEquals(sb2,sb1.substring(0,19));
        buff = new byte[30];
        off = TarUtils.formatNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 30);
        sb2 = TarUtils.parseName(buff, 1, buff.length-1);
        assertEquals(sb1, sb2);
    }

    public void testParseOctal() throws Exception{
        long value; 
        byte [] buffer;
        final long MAX_OCTAL  = 077777777777L; // Allowed 11 digits
        final String maxOctal = "77777777777 "; // Maximum valid octal
        buffer = maxOctal.getBytes(CharsetNames.UTF_8);
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(MAX_OCTAL, value);
        buffer[buffer.length-1]=0;
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(MAX_OCTAL, value);
        buffer=new byte[]{0,0};
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(0, value);
        buffer=new byte[]{0,' '};
        value = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(0, value);
    }

    public void testParseOctalInvalid() throws Exception{
        byte [] buffer;
        buffer=new byte[0]; // empty byte array
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - should be at least 2 bytes long");
        } catch (IllegalArgumentException expected) {
        }
        buffer=new byte[]{0}; // 1-byte array
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - should be at least 2 bytes long");
        } catch (IllegalArgumentException expected) {
        }
        buffer=new byte[]{' ',0,0,0}; // not all NULs
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - not all NULs");
        } catch (IllegalArgumentException expected) {
        }
        buffer = "abcdef ".getBytes(CharsetNames.UTF_8); // Invalid input
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        buffer = "77777777777".getBytes(CharsetNames.UTF_8); // Invalid input - no trailer
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - no trailer");
        } catch (IllegalArgumentException expected) {
        }
        buffer = " 0 07 ".getBytes(CharsetNames.UTF_8); // Invalid - embedded space
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - embedded space");
        } catch (IllegalArgumentException expected) {
        }
        buffer = " 0\00007 ".getBytes(CharsetNames.UTF_8); // Invalid - embedded NUL
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - embedded NUL");
        } catch (IllegalArgumentException expected) {
        }
    }

    private void checkRoundTripOctal(final long value, final int bufsize) {
        byte [] buffer = new byte[bufsize];
        long parseValue;
        TarUtils.formatLongOctalBytes(value, buffer, 0, buffer.length);
        parseValue = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(value,parseValue);
    }

    private void checkRoundTripOctal(final long value) {
        checkRoundTripOctal(value, TarConstants.SIZELEN);
    }

    public void testRoundTripOctal() {
        checkRoundTripOctal(0);
        checkRoundTripOctal(1);
//        checkRoundTripOctal(-1); // TODO What should this do?
        checkRoundTripOctal(TarConstants.MAXSIZE);
//        checkRoundTripOctal(0100000000000L); // TODO What should this do?

        checkRoundTripOctal(0, TarConstants.UIDLEN);
        checkRoundTripOctal(1, TarConstants.UIDLEN);
        checkRoundTripOctal(TarConstants.MAXID, 8);
    }

    private void checkRoundTripOctalOrBinary(final long value, final int bufsize) {
        byte [] buffer = new byte[bufsize];
        long parseValue;
        TarUtils.formatLongOctalOrBinaryBytes(value, buffer, 0, buffer.length);
        parseValue = TarUtils.parseOctalOrBinary(buffer,0, buffer.length);
        assertEquals(value,parseValue);
    }

    public void testRoundTripOctalOrBinary8() {
        testRoundTripOctalOrBinary(8);
    }

    public void testRoundTripOctalOrBinary12() {
        testRoundTripOctalOrBinary(12);
        checkRoundTripOctalOrBinary(Long.MAX_VALUE, 12);
        checkRoundTripOctalOrBinary(Long.MIN_VALUE + 1, 12);
    }

    private void testRoundTripOctalOrBinary(int length) {
        checkRoundTripOctalOrBinary(0, length);
        checkRoundTripOctalOrBinary(1, length);
        checkRoundTripOctalOrBinary(TarConstants.MAXSIZE, length); // will need binary format
        checkRoundTripOctalOrBinary(-1, length); // will need binary format
        checkRoundTripOctalOrBinary(0xff00000000000001l, length);
    }

    // Check correct trailing bytes are generated
    public void testTrailers() {
        byte [] buffer = new byte[12];
        TarUtils.formatLongOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals('3', buffer[buffer.length-2]); // end of number
        TarUtils.formatOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(0  , buffer[buffer.length-1]);
        assertEquals(' ', buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
        TarUtils.formatCheckSumOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals(0  , buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
    }

    public void testNegative() throws Exception {
        byte [] buffer = new byte[22];
        TarUtils.formatUnsignedOctalString(-1, buffer, 0, buffer.length);
        assertEquals("1777777777777777777777", new String(buffer, CharsetNames.UTF_8));
    }

    public void testOverflow() throws Exception {
        byte [] buffer = new byte[8-1]; // a lot of the numbers have 8-byte buffers (nul term)
        TarUtils.formatUnsignedOctalString(07777777L, buffer, 0, buffer.length);
        assertEquals("7777777", new String(buffer, CharsetNames.UTF_8));
        try {
            TarUtils.formatUnsignedOctalString(017777777L, buffer, 0, buffer.length);
            fail("Should have cause IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testRoundTripNames(){
        checkName("");
        checkName("The quick brown fox\n");
        checkName("\177");
        // checkName("\0"); // does not work, because NUL is ignored
    }

    public void testRoundEncoding() throws Exception {
        // COMPRESS-114
        ZipEncoding enc = ZipEncodingHelper.getZipEncoding(CharsetNames.ISO_8859_1);
        String s = "0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W220\u00b1ZB\u00b1LALALA\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1CAN\u00b1\u00b1DC\u00b1\u00b1\u00b104\u00b1060302\u00b1MOE.model";
        byte buff[] = new byte[100];
        int len = TarUtils.formatNameBytes(s, buff, 0, buff.length, enc);
        assertEquals(s, TarUtils.parseName(buff, 0, len, enc));
    }

    private void checkName(String string) {
        byte buff[] = new byte[100];
        int len = TarUtils.formatNameBytes(string, buff, 0, buff.length);
        assertEquals(string, TarUtils.parseName(buff, 0, len));
    }

    public void testReadNegativeBinary8Byte() {
        byte[] b = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601l, TarUtils.parseOctalOrBinary(b, 0, 8));
    }

    public void testReadNegativeBinary12Byte() {
        byte[] b = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601l, TarUtils.parseOctalOrBinary(b, 0, 12));
    }


    public void testWriteNegativeBinary8Byte() {
        byte[] b = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xf1, (byte) 0xef,
        };
        assertEquals(-3601l, TarUtils.parseOctalOrBinary(b, 0, 8));
    }

    // https://issues.apache.org/jira/browse/COMPRESS-191
    public void testVerifyHeaderCheckSum() {
        byte[] valid = { // from bla.tar
                116, 101, 115, 116, 49, 46, 120, 109, 108, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 48, 48, 48, 48, 54, 52, 52, 0, 48, 48, 48, 48, 55, 54, 53,
                0, 48, 48, 48, 48, 55, 54, 53, 0, 48, 48, 48, 48, 48, 48, 48,
                49, 49, 52, 50, 0, 49, 48, 55, 49, 54, 53, 52, 53, 54, 50, 54,
                0, 48, 49, 50, 50, 54, 48, 0, 32, 48, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 117, 115, 116, 97, 114, 32, 32, 0,
                116, 99, 117, 114, 100, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 116, 99, 117,
                114, 100, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0 };
        assertTrue(TarUtils.verifyCheckSum(valid));

        byte[] compress117 = { // from COMPRESS-117
                116, 101, 115, 116, 49, 46, 120, 109, 108, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 48, 48, 48, 48, 54, 52, 52, 0, 48, 48, 48, 48, 55, 54, 53,
                0, 48, 48, 48, 48, 55, 54, 53, 0, 48, 48, 48, 48, 48, 48, 48,
                49, 49, 52, 50, 0, 49, 48, 55, 49, 54, 53, 52, 53, 54, 50, 54,
                0, 48, 49, 50, 50, 54, 48, 0, 32, 48, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        assertTrue(TarUtils.verifyCheckSum(compress117));

        byte[] invalid = { // from the testAIFF.aif file in Tika
                70, 79, 82, 77, 0, 0, 15, 46, 65, 73, 70, 70, 67, 79, 77, 77,
                0, 0, 0, 18, 0, 2, 0, 0, 3, -64, 0, 16, 64, 14, -84, 68, 0, 0,
                0, 0, 0, 0, 83, 83, 78, 68, 0, 0, 15, 8, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, -1, -1, 0, 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 1, -1, -1,
                0, 0, 0, 0, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0,
                0, 0, 0, 1, -1, -1, 0, 1, -1, -2, 0, 1, -1, -1, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0,
                2, -1, -2, 0, 2, -1, -1, 0, 0, 0, 1, -1, -1, 0, 1, -1, -1, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0,
                0, 1, -1, -1, 0, 0, 0, 1, -1, -1, 0, 0, 0, 1, -1, -2, 0, 2,
                -1, -1, 0, 0, 0, 0, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 1, -1, -1, 0, 2, -1, -2,
                0, 2, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, -1,
                -2, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                -1, -1, 0, 1, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -1, 0, 0,
                0, 0, 0, 0, -1, -1, 0, 1, -1, -1, 0, 1, -1, -1, 0, 2, -1, -2,
                0, 1, 0, 0, -1, -1, 0, 2, -1, -2, 0, 2, -1, -2, 0, 1, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0,
                0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 2, -1, -1, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -2, 0, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 2, -1, -2,
                0, 2, -1, -2, 0, 2, -1, -1, 0, 0, 0, 0, -1, -1, 0, 1, -1, -1,
                0, 1, -1, -1, 0, 1, -1, -1, 0, 1, -1, -1, 0, 1, 0, 0, 0, 0,
                -1, -1, 0, 2, -1, -2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, -1, -1, 0, 0, 0, 0, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 1 };
        assertFalse(TarUtils.verifyCheckSum(invalid));
    }

}
