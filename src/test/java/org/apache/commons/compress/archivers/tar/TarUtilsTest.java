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
    
    private void fillBuff(byte []buffer, String input){
        for(int i=0; i<buffer.length;i++){
            buffer[i]=0;
        }
        System.arraycopy(input.getBytes(),0,buffer,0,Math.min(buffer.length,input.length()));        
    }

    public void testParseOctal(){
        byte [] buffer = new byte[20];
        fillBuff(buffer,"777777777777 ");
        long value; 
        value = TarUtils.parseOctal(buffer,0, 11);
        assertEquals(077777777777L, value);
        value = TarUtils.parseOctal(buffer,0, 12);
        assertEquals(0777777777777L, value);
        buffer[11]=' ';
        value = TarUtils.parseOctal(buffer,0, 11);
        assertEquals(077777777777L, value);
        buffer[11]=0;
        value = TarUtils.parseOctal(buffer,0, 11);
        assertEquals(077777777777L, value);
        fillBuff(buffer, "abcdef"); // Invalid input
        try {
            value = TarUtils.parseOctal(buffer,0, 11);
            fail("Expected IllegalArgumentException");
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
}
