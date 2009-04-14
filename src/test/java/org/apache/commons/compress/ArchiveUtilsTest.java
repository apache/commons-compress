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

import org.apache.commons.compress.utils.ArchiveUtils;

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
    public void testCompareBA(){
        byte[] buffer1 = {1,2,3};
        byte[] buffer2 = {1,2,3,0};
        byte[] buffer3 = {1,2,3};
        assertTrue(ArchiveUtils.isEqual(buffer1, buffer2, true));
        assertFalse(ArchiveUtils.isEqual(buffer1, buffer2, false));
        assertFalse(ArchiveUtils.isEqual(buffer1, buffer2));
        assertTrue(ArchiveUtils.isEqual(buffer2, buffer1, true));
        assertFalse(ArchiveUtils.isEqual(buffer2, buffer1, false));
        assertFalse(ArchiveUtils.isEqual(buffer2, buffer1));
        assertTrue(ArchiveUtils.isEqual(buffer1, buffer3));
        assertTrue(ArchiveUtils.isEqual(buffer3, buffer1));
    }
    
    public void testCompareAscii(){
        byte[] buffer1 = {'a','b','c'};
        byte[] buffer2 = {'d','e','f',0};
        assertTrue(ArchiveUtils.matchAsciiBuffer("abc", buffer1));
        assertFalse(ArchiveUtils.matchAsciiBuffer("abc\0", buffer1));
        assertTrue(ArchiveUtils.matchAsciiBuffer("def\0", buffer2));        
        assertFalse(ArchiveUtils.matchAsciiBuffer("def", buffer2));
    }
    
    public void testAsciiConversions() {
        asciiToByteAndBackOK("");
        asciiToByteAndBackOK("abcd");
        asciiToByteAndBackFail("\u8025");
    }

    private void asciiToByteAndBackOK(String inputString) {
        assertEquals(inputString, ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString)));
    }

    private void asciiToByteAndBackFail(String inputString) {
        assertFalse(inputString.equals(ArchiveUtils.toAsciiString(ArchiveUtils.toAsciiBytes(inputString))));
    }
}
