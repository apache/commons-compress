/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.compress.harmony.unpack200.CpBands;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFieldRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LocalVariableTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Tests for CodeAttribute
 */
public class CodeAttributeTest extends TestCase {

    public class MockCodeAttribute extends CodeAttribute {

        public MockCodeAttribute(int maxStack, int maxLocals,
                byte[] codePacked, Segment segment,
                OperandManager operandManager, List exceptionTable) {
            super(maxStack, maxLocals, codePacked, segment, operandManager,
                    exceptionTable);
        }

        @Override
        public int getLength() {
            return super.getLength();
        }
    }

    public class MockCpBands extends CpBands {

        public MockCpBands(Segment segment) {
            super(segment);
        }

        @Override
        public CPFieldRef cpFieldValue(int index) {
            return null;
        }

        @Override
        public CPString cpStringValue(int index) {
            return new CPString(new CPUTF8("Hello"), -1);
        }

        @Override
        public CPMethodRef cpMethodValue(int index) {
            return null;
        }

    }

    public class MockOperandManager extends OperandManager {

        public MockOperandManager() {
            super(new int[] {}, // bcCaseCount
                    new int[] {}, // bcCaseValues
                    new int[] {}, // bcByte
                    new int[] {}, // bcShort
                    new int[] {}, // bcLocal
                    new int[] {}, // bcLabel
                    new int[] {}, // bcIntRef
                    new int[] {}, // bcFloatRef
                    new int[] {}, // bcLongRef
                    new int[] {}, // bcDoubleRef
                    new int[] { 0, 1, 2, 3, 4 }, // bcStringRef
                    new int[] {}, // bcClassRef
                    new int[] {}, // bcFieldRef
                    new int[] {}, // bcMethodRef
                    new int[] {}, // bcIMethodRef
                    new int[] { 0, 0, 0, 0, 0, 0 }, // bcThisField
                    new int[] {}, // bcSuperField
                    new int[] { 0 }, // bcThisMethod
                    new int[] {}, // bcSuperMethod
                    new int[] {} // bcInitRef
                    , null);
        }
    }

    public class MockSegment extends Segment {

        @Override
        public SegmentConstantPool getConstantPool() {
            return new MockSegmentConstantPool(cpBands);
        }
    }

    public class MockSegmentConstantPool extends SegmentConstantPool {

        public MockSegmentConstantPool(CpBands bands) {
            super(bands);
        }

        @Override
        protected int matchSpecificPoolEntryIndex(String[] nameArray,
                String compareString, int desiredIndex) {
            return 1;
        }
    }

    Segment segment = new MockSegment();
    CpBands cpBands = new MockCpBands(segment);

    public byte[] mixedByteArray = { -47, // aload_0_getstatic_this 0, 1
            -46, // aload_0_putstatic_this 4, 5
            1, // aconst_null 8
            -45, // aload_0_getfield_this 9, 10
            // Should always end with a multibyte
            // instruction
            -44, // aload_0_putfield_this (int) 13, 14
    };

    public byte[] singleByteArray = { 42, // aload_0 0
            1, // aconst_null 1
            18, // ldc 2
            -49, // return 4
    };

    public void testLength() {
        OperandManager operandManager = new MockOperandManager();
        operandManager.setSegment(segment);
        operandManager.setCurrentClass("java/lang/Foo");

        MockCodeAttribute attribute = new MockCodeAttribute(3, // maxStack
                2, // maxLocals
                mixedByteArray, // codePacked
                segment, // segment
                operandManager, // operandManager
                new ArrayList());
        assertEquals(29, attribute.getLength());

        attribute.attributes.add(new LocalVariableTableAttribute(0, null, null,
                null, null, null));
        assertEquals(37, attribute.getLength());
    }

    public void testMixedByteCodes() {
        OperandManager operandManager = new MockOperandManager();
        operandManager.setSegment(segment);
        operandManager.setCurrentClass("java/lang/Foo");

        CodeAttribute attribute = new CodeAttribute(3, // maxStack
                2, // maxLocals
                mixedByteArray, // codePacked
                segment, // segment
                operandManager, // operandManager
                new ArrayList());
        assertEquals(2, attribute.maxLocals);
        assertEquals(3, attribute.maxStack);
        assertEquals("aload_0_putfield_this", ((ByteCode) attribute.byteCodes
                .get(4)).toString());

        int expectedLabels[] = new int[] { 0, 1, 4, 5, 8, 9, 10, 13, 14 };
        for (int index = 0; index < expectedLabels.length; index++) {
            assertEquals(expectedLabels[index],
                    ((Integer) attribute.byteCodeOffsets.get(index)).intValue());
        }
    }

    public void testSingleByteCodes() {
        OperandManager operandManager = new MockOperandManager();
        operandManager.setSegment(segment);
        operandManager.setCurrentClass("java/lang/Foo");

        CodeAttribute attribute = new CodeAttribute(4, // maxStack
                3, // maxLocals
                singleByteArray, // codePacked
                segment, // segment
                operandManager, // operandManager
                new ArrayList());
        assertEquals(3, attribute.maxLocals);
        assertEquals(4, attribute.maxStack);
        assertEquals("invokespecial_this", ((ByteCode) attribute.byteCodes
                .get(3)).toString());

        int expectedLabels[] = new int[] { 0, 1, 2, 4 };
        for (int index = 0; index < expectedLabels.length; index++) {
            assertEquals(expectedLabels[index],
                    ((Integer) attribute.byteCodeOffsets.get(index)).intValue());
        }
    }

}
