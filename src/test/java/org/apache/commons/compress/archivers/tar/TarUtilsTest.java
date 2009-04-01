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
        StringBuffer sb1 = new StringBuffer("abcdefghijklmnopqrstuvwxyz");
        int off = TarUtils.getNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 20);
        StringBuffer sb2 = TarUtils.parseName(buff, 1, 10);
        assertEquals(sb2.toString(),sb1.substring(0,10));
        sb2 = TarUtils.parseName(buff, 1, 19);
        assertEquals(sb2.toString(),sb1.substring(0,19));
        buff = new byte[30];
        off = TarUtils.getNameBytes(sb1, buff, 1, buff.length-1);
        assertEquals(off, 30);
        sb2 = TarUtils.parseName(buff, 1, buff.length-1);
        assertEquals(sb1.toString(), sb2.toString());
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
        value = TarUtils.parseOctal(buffer,0, 11);
//        assertEquals(0, value); // Or perhaps an Exception?
    }
    
    private void checkRoundTripOctal(final long value) {
        byte [] buffer = new byte[12];
        long parseValue;
        TarUtils.getLongOctalBytes(value, buffer, 0, buffer.length);
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
        TarUtils.getLongOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals('3', buffer[buffer.length-2]); // end of number
        TarUtils.getOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(0  , buffer[buffer.length-1]);
        assertEquals(' ', buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
        TarUtils.getCheckSumOctalBytes(123, buffer, 0, buffer.length);
        assertEquals(' ', buffer[buffer.length-1]);
        assertEquals(0  , buffer[buffer.length-2]);
        assertEquals('3', buffer[buffer.length-3]); // end of number
    }
    
    public void testNegative() {
        byte [] buffer = new byte[10];
        TarUtils.formatUnsignedOctalString(-1, buffer, 0, buffer.length);
        // Currently negative numbers generate all zero buffer. This may need to change.
        assertEquals("0000000000", new String(buffer));
        
    }
}
