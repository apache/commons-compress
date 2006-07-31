/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress;

import junit.framework.TestCase;

/**
 * Test for Compress Utils
 * @author christian.grobmeier
 */
public class CompressUtilsTest extends TestCase {

	public void testCompareByteArrays() {
		byte[] source = { 0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0 };
		byte[] match = { 0x0, 0x0, 0x0, 0x0, 0x0 };
		
		assertTrue(CompressUtils.compareByteArrays(source, match));
		
		byte[] match2 = { 0x0, 0x0, 0x0, 0x0, 0x10 };
		assertFalse(CompressUtils.compareByteArrays(source, match2));
		
		byte[] source3 = { 0x50, 0x4b, 0x03, 0x04, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0 };
		byte[] match3 = { 0x50, 0x4b, 0x03, 0x04};
		assertTrue(CompressUtils.compareByteArrays(source3, match3));
		
		byte[] source4 = { 0x50, 0x4b, 0x03, 0x04, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0,0x0, 0x0, 0x0, 0x0, 0x0 };
		byte[] match4 = { 0x52, 0x4b, 0x03, 0x04};
		assertFalse(CompressUtils.compareByteArrays(source4, match4));
	}
}
