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
        buffer = maxOctal.getBytes("UTF-8");
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
        buffer=new byte[]{0,0,' '}; // not all NULs
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - not all NULs");
        } catch (IllegalArgumentException expected) {
        }
        buffer=new byte[]{' ',0,0,0}; // not all NULs
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - not all NULs");
        } catch (IllegalArgumentException expected) {
        }
        buffer = "abcdef ".getBytes("UTF-8"); // Invalid input
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        buffer = "77777777777".getBytes("UTF-8"); // Invalid input - no trailer
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - no trailer");
        } catch (IllegalArgumentException expected) {
        }
        buffer = " 0 07 ".getBytes("UTF-8"); // Invalid - embedded space
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - embedded space");
        } catch (IllegalArgumentException expected) {
        }
        buffer = " 0\00007 ".getBytes("UTF-8"); // Invalid - embedded NUL
        try {
            TarUtils.parseOctal(buffer,0, buffer.length);
            fail("Expected IllegalArgumentException - embedded NUL");
        } catch (IllegalArgumentException expected) {
        }
    }

    private void checkRoundTripOctal(final long value) {
        byte [] buffer = new byte[12];
        long parseValue;
        TarUtils.formatLongOctalBytes(value, buffer, 0, buffer.length);
        parseValue = TarUtils.parseOctal(buffer,0, buffer.length);
        assertEquals(value,parseValue);
    }
    
    public void testRoundTripOctal() {
        checkRoundTripOctal(0);
        checkRoundTripOctal(1);
//        checkRoundTripOctal(-1); // TODO What should this do?
        checkRoundTripOctal(077777777777L);
//        checkRoundTripOctal(0100000000000L); // TODO What should this do?
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
    
    public void testNegative() {
        byte [] buffer = new byte[22];
        TarUtils.formatUnsignedOctalString(-1, buffer, 0, buffer.length);
        assertEquals("1777777777777777777777", new String(buffer));
    }

    public void testOverflow() {
        byte [] buffer = new byte[8-1]; // a lot of the numbers have 8-byte buffers (nul term)
        TarUtils.formatUnsignedOctalString(07777777L, buffer, 0, buffer.length);
        assertEquals("7777777", new String(buffer));        
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
        // COMPRESS-114
        checkName("0302-0601-3\u00b1\u00b1\u00b1F06\u00b1W220\u00b1ZB\u00b1LALALA\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1\u00b1CAN\u00b1\u00b1DC\u00b1\u00b1\u00b104\u00b1060302\u00b1MOE.model");
    }

    private void checkName(String string) {
        byte buff[] = new byte[100];
        int len = TarUtils.formatNameBytes(string, buff, 0, buff.length);
        assertEquals(string, TarUtils.parseName(buff, 0, len));
    }
}
