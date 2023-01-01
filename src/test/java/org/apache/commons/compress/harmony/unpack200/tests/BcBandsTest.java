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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.AttrDefinitionBands;
import org.apache.commons.compress.harmony.unpack200.BcBands;
import org.apache.commons.compress.harmony.unpack200.ClassBands;
import org.apache.commons.compress.harmony.unpack200.CpBands;
import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPDouble;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFieldRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFloat;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInteger;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInterfaceMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPLong;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPNameAndType;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for Pack200 bytecode bands
 */

/*
 * TODO: The number 8 is used in most of the tests in this class as a low
 * (non-zero) number that is not likely to indicate a multiple byte number, but
 * should be replaced with properly encoded byte arrays when encoding is
 * implemented.
 */
public class BcBandsTest extends AbstractBandsTestCase {

    public class MockClassBands extends ClassBands {

        public MockClassBands(final Segment segment) {
            super(segment);
        }

        @Override
        public int[] getClassSuperInts() {
            final int[] superClasses = new int[numClasses];
            for (int index = 0; index < numClasses; index++) {
                superClasses[index] = 0;
            }
            return superClasses;
        }

        @Override
        public int[] getClassThisInts() {
            final int[] thisClasses = new int[numClasses];
            for (int index = 0; index < numClasses; index++) {
                thisClasses[index] = 0;
            }
            return thisClasses;
        }

        @Override
        public boolean[] getCodeHasAttributes() {
            int totalMethods = 0;
            for (int i = 0; i < numClasses; i++) {
                totalMethods += numMethods[i];
            }
            return new boolean[totalMethods];
        }

        @Override
        public int[] getCodeMaxNALocals() {
            int totalMethods = 0;
            for (int i = 0; i < numClasses; i++) {
                totalMethods += numMethods[i];
            }
            return new int[totalMethods];
        }

        @Override
        public int[] getCodeMaxStack() {
            int totalMethods = 0;
            for (int i = 0; i < numClasses; i++) {
                totalMethods += numMethods[i];
            }
            return new int[totalMethods];
        }

        @Override
        public ArrayList[][] getMethodAttributes() {
            final ArrayList[][] attributes = new ArrayList[numClasses][];
            for (int i = 0; i < attributes.length; i++) {
                attributes[i] = new ArrayList[numMethods[i]];
                for (int j = 0; j < attributes[i].length; j++) {
                    attributes[i][j] = new ArrayList();
                }
            }
            return attributes;
        }

        @Override
        public String[][] getMethodDescr() {
            final String[][] descr = new String[numClasses][];
            for (int i = 0; i < descr.length; i++) {
                descr[i] = new String[numMethods[i]];
                for (int j = 0; j < descr[i].length; j++) {
                    descr[i][j] = "hello()";
                }
            }
            return descr;
        }

        @Override
        public long[][] getMethodFlags() {
            final long[][] flags = new long[numClasses][];
            for (int i = 0; i < flags.length; i++) {
                flags[i] = new long[numMethods[i]];
            }
            return flags;
        }

        @Override
        public ArrayList getOrderedCodeAttributes() {
            int totalMethods = 0;
            for (final int numMethod : numMethods) {
                totalMethods = totalMethods + numMethod;
            }
            final ArrayList orderedAttributeList = new ArrayList();
            for (int classIndex = 0; classIndex < totalMethods; classIndex++) {
                orderedAttributeList.add(new ArrayList());
            }
            return orderedAttributeList;
        }
    }

    public class MockCpBands extends CpBands {

        private final CPUTF8 cpUTF8 = new CPUTF8("java/lang/String");
        private final CPClass cpClass = new CPClass(cpUTF8, -1);
        private final CPNameAndType descriptor = new CPNameAndType(new CPUTF8(
                "Hello"), new CPUTF8("(a, b, c)"), -1);

        public MockCpBands(final Segment segment) {
            super(segment);
        }

        @Override
        public CPClass cpClassValue(final int index) {
            return cpClass;
        }

        @Override
        public CPDouble cpDoubleValue(final int index) {
            return new CPDouble(Double.valueOf(2.5D), index);
        }

        @Override
        public CPFieldRef cpFieldValue(final int index) {
            return new CPFieldRef(cpClass, descriptor, index);
        }

        @Override
        public CPFloat cpFloatValue(final int index) {
            return new CPFloat(Float.valueOf(2.5F), index);
        }

        @Override
        public CPInterfaceMethodRef cpIMethodValue(final int index) {
            return new CPInterfaceMethodRef(cpClass, descriptor, index);
        }

        @Override
        public CPInteger cpIntegerValue(final int index) {
            return new CPInteger(Integer.valueOf(21), index);
        }

        @Override
        public CPLong cpLongValue(final int index) {
            return new CPLong(Long.valueOf(21L), index);
        }

        @Override
        public CPMethodRef cpMethodValue(final int index) {
            return new CPMethodRef(cpClass, descriptor, index);
        }

        @Override
        public CPString cpStringValue(final int index) {
            return new CPString(cpUTF8, index);
        }

        @Override
        public String[] getCpClass() {
            return new String[] {"Hello"};
        }

        @Override
        public String[] getCpFieldClass() {
            return new String[]{};
        }

        @Override
        public String[] getCpIMethodClass() {
            return new String[]{};
        }

        @Override
        public String[] getCpMethodClass() {
            return new String[]{};
        }
    }

    public class MockSegment extends AbstractBandsTestCase.MockSegment {

        public CpBands cpBands;

        @Override
        protected AttrDefinitionBands getAttrDefinitionBands() {
            return new MockAttributeDefinitionBands(this);
        }

        @Override
        protected ClassBands getClassBands() {
            return new MockClassBands(this);
        }

        @Override
        public SegmentConstantPool getConstantPool() {
            return cpBands.getConstantPool();
        }

        @Override
        protected CpBands getCpBands() {
            if (null == cpBands) {
                cpBands = new MockCpBands(this);
            }
            return cpBands;
        }
    }

    BcBands bcBands = new BcBands(new MockSegment());

    /**
     * Test with codes that should require entries in the bc_byte band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcByteBand() throws IOException, Pack200Exception {
        final byte[] bytes = { 16, (byte) 132, (byte) 188, (byte) 197,
                (byte) 255, 8, 8, 8, 8, // bc_byte band
                8, // bc_locals band (required by iinc (132))
                8 }; // bc_class band (required by multianewarray (197))
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(4, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_byte = bcBands.getBcByte();
        assertEquals(4, bc_byte.length);
        for (final int element : bc_byte) {
            assertEquals(8, element);
        }
        assertEquals(1, bcBands.getBcLocal().length);
        assertEquals(1, bcBands.getBcClassRef().length);
    }

    /**
     * Test with codes that require entries in the bc_case_count and
     * bc_case_value bands
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcCaseBands() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 170, (byte) 171, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 255, 2, 5, // bc_case_count
                0, 0, 0, 0, 0, 0, 0, // bc_case_value
                0, 0, 0, 0, 0, 0, 0, 0, 0 }; // bc_label
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(18, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_case_count = bcBands.getBcCaseCount();
        assertEquals(2, bc_case_count.length);
        assertEquals(2, bc_case_count[0]);
        assertEquals(5, bc_case_count[1]);
        final int[] bc_case_value = bcBands.getBcCaseValue();
        assertEquals(0, bc_case_value[0]);
        assertEquals(0, bc_case_value[1]);
        assertEquals(9, bcBands.getBcLabel().length);
    }

    /**
     * Test with codes that should require entries in the bc_classref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcClassRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 233, (byte) 236, (byte) 255, 8, 8 }; // bc_classref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_classref = bcBands.getBcClassRef();
        assertEquals(2, bc_classref.length);
    }

    /**
     * Test with codes that should require entries in the bc_doubleref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcDoubleRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 239, (byte) 255, 8 }; // bc_doubleref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(1, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_doubleref = bcBands.getBcDoubleRef();
        assertEquals(1, bc_doubleref.length);
    }

    @Test
    @Disabled("TODO: Implement")
    public void testBcEscBands() {
        // TODO
    }

    @Test
    @Disabled("TODO: Implement")
    public void testBcEscRefBands() {
        // TODO
    }

    /**
     * Test with codes that should require entries in the bc_fieldref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcFieldRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 178, (byte) 179, (byte) 180,
                (byte) 181, (byte) 255, 8, 8, 8, 8 }; // bc_fieldref band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(4, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_fieldref = bcBands.getBcFieldRef();
        assertEquals(4, bc_fieldref.length);
    }

    /**
     * Test with codes that should require entries in the bc_floatref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcFloatRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 235, (byte) 238, (byte) 255, 8, 8 }; // bc_floatref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_floatref = bcBands.getBcFloatRef();
        assertEquals(2, bc_floatref.length);
    }

    /**
     * Test with codes that should require entries in the bc_imethodref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcIMethodRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 185, (byte) 255, 8 }; // bc_imethodref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(1, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_imethodref = bcBands.getBcIMethodRef();
        assertEquals(1, bc_imethodref.length);
    }

    /**
     * Test with codes that should require entries in the bc_initrefref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    @Disabled("TODO: Need to fix this test so it has enough data to pass.")
    public void testBcInitRefRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 230, (byte) 231, (byte) 232,
                (byte) 255, 8, 8, 8 }; // bc_initrefref band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(3, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_initrefref = bcBands.getBcInitRef();
        assertEquals(3, bc_initrefref.length);
    }

    /**
     * Test with codes that should require entries in the bc_intref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcIntRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 234, (byte) 237, (byte) 255, 8, 8 }; // bc_intref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_intref = bcBands.getBcIntRef();
        assertEquals(2, bc_intref.length);
    }

    /**
     * Test with codes that should require entries in the bc_label band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcLabelBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 159, (byte) 160, (byte) 161,
                (byte) 162, (byte) 163, (byte) 164, (byte) 165, (byte) 166,
                (byte) 167, (byte) 168, (byte) 170, (byte) 171, (byte) 198,
                (byte) 199, (byte) 200, (byte) 201, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 255, 2, 2, // bc_case_count
                // (required
                // by
                // tableswitch
                // (170) and
                // lookupswitch
                // (171))
                0, 0, 0, 0, // bc_case_value
                // Now that we're actually doing real label lookup, need valid
                // labels
                // 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 }; // bc_label
                // band
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // bc_label
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(36, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(20, bcBands.getBcLabel().length);
    }

    /**
     * Test with codes that should require entries in the bc_local band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcLocalBand() throws IOException, Pack200Exception {
        final byte[] bytes = { 21, 22, 23, 24, 25, 54, 55, 56, 57, 58,
                (byte) 169, (byte) 255, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 }; // bc_local
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(11, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(11, bcBands.getBcLocal().length);
    }

    /**
     * Test with codes that should require entries in the bc_longref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcLongRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { 20, (byte) 255, 8 }; // bc_longref band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(1, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_longref = bcBands.getBcLongRef();
        assertEquals(1, bc_longref.length);
    }

    /**
     * Test with codes that should require entries in the bc_methodref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcMethodRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 182, (byte) 183, (byte) 184,
                (byte) 255, 8, 8, 8 }; // bc_methodref band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(3, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_methodref = bcBands.getBcMethodRef();
        assertEquals(3, bc_methodref.length);
    }

    /**
     * Test with codes that should require entries in the bc_short band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcShortBand() throws IOException, Pack200Exception {
        // TODO: Need to fix this test so it has enough data to pass.
        final byte[] bytes = { 17, (byte) 196, (byte) 132, (byte) 255, 8,
                8,// bc_short band
                8 }; // bc_locals band (required by wide iinc (196, 132))
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(3, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(2, bcBands.getBcShort().length);
        assertEquals(1, bcBands.getBcLocal().length);
    }

    /**
     * Test with codes that should require entries in the bc_stringref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcStringRefBand() throws IOException, Pack200Exception {
        final byte[] bytes = { 18, 19, (byte) 255, 8, 8 }; // bc_stringref
        // band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_stringref = bcBands.getBcStringRef();
        assertEquals(2, bc_stringref.length);
    }

    /**
     * Test with codes that should require entries in the bc_superfield band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcSuperFieldBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 216, (byte) 217, (byte) 218,
                (byte) 219, (byte) 223, (byte) 224, (byte) 225, (byte) 226,
                (byte) 255, 8, 8, 8, 8, 8, 8, 8, 8 }; // bc_superfield band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(8, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_superfield = bcBands.getBcSuperField();
        assertEquals(8, bc_superfield.length);
    }

    /**
     * Test with codes that should require entries in the bc_supermethod band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    @Disabled("TODO: Need to fix this test so it has enough data to pass.")
    public void testBcSuperMethodBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 220, (byte) 221, (byte) 222,
                (byte) 227, (byte) 228, (byte) 229, (byte) 255, 8, 8, 8, 8, 8,
                8 }; // bc_supermethod band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(6, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_supermethod = bcBands.getBcSuperMethod();
        assertEquals(6, bc_supermethod.length);
    }

    /**
     * Test with codes that should require entries in the bc_thisfieldref band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcThisFieldBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 202, (byte) 203, (byte) 204,
                (byte) 205, (byte) 209, (byte) 210, (byte) 211, (byte) 212,
                (byte) 255, 8, 8, 8, 8, 8, 8, 8, 8 }; // bc_thisfieldref band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(8, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_thisfield = bcBands.getBcThisField();
        assertEquals(8, bc_thisfield.length);
    }

    /**
     * Test with codes that should require entries in the bc_thismethod band
     *
     * @throws Pack200Exception
     * @throws IOException
     */
    @Test
    public void testBcThisMethodBand() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 206, (byte) 207, (byte) 208,
                (byte) 213, (byte) 214, (byte) 215, (byte) 255, 8, 8, 8, 8, 8,
                8 }; // bc_thismethod band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(6, bcBands.getMethodByteCodePacked()[0][0].length);
        final int[] bc_thismethod = bcBands.getBcThisMethod();
        assertEquals(6, bc_thismethod.length);
    }

    /**
     * Test with multiple classes but single byte instructions
     *
     * @throws IOException
     * @throws Pack200Exception
     */
    @Test
    public void testMultipleClassesSimple() throws IOException,
            Pack200Exception {
        numClasses = 2;
        numMethods = new int[] { 1, 1 };
        final byte[] bytes = { 50, 50, (byte) 255, 50, 50, (byte) 255 };
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);

        numClasses = 1;
        numMethods = new int[] { 1 };
    }

    /**
     * Test with multiple classes and multiple methods but single byte
     * instructions
     *
     * @throws IOException
     * @throws Pack200Exception
     */
    @Test
    public void testMultipleMethodsSimple() throws IOException,
            Pack200Exception {
        numClasses = 2;
        numMethods = new int[] { 3, 1 };
        final byte[] bytes = { 50, 50, (byte) 255, 50, 50, (byte) 255, 50,
                50, (byte) 255, 50, 50, (byte) 255 };
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(2, bcBands.getMethodByteCodePacked()[0][0].length);

        numClasses = 1;
        numMethods = new int[] { 1 };
    }

    /**
     * Test with single byte instructions that mean all other bands apart from
     * bc_codes will be empty.
     *
     * @throws IOException
     * @throws Pack200Exception
     */
    @Test
    public void testSimple() throws IOException, Pack200Exception {
        final byte[] bytes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13, 14, 15, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
                39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 59,
                60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75,
                76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91,
                92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
                106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117,
                118, 119, 120, 121, 122, 123, 124, 125, 126, 127, (byte) 128,
                (byte) 129, (byte) 130, (byte) 131, (byte) 133, (byte) 134,
                (byte) 135, (byte) 136, (byte) 137, (byte) 138, (byte) 139,
                (byte) 140, (byte) 141, (byte) 142, (byte) 143, (byte) 144,
                (byte) 145, (byte) 146, (byte) 147, (byte) 148, (byte) 149,
                (byte) 150, (byte) 151, (byte) 172, (byte) 173, (byte) 174,
                (byte) 175, (byte) 176, (byte) 177, (byte) 190, (byte) 191,
                (byte) 194, (byte) 195, (byte) 255 };
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(bytes.length - 1,
                bcBands.getMethodByteCodePacked()[0][0].length);
    }

    @Test
    public void testWideForms() throws IOException, Pack200Exception {
        final byte[] bytes = { (byte) 196, (byte) 54, // wide istore
                (byte) 196, (byte) 132, // wide iinc
                (byte) 255, 0, // bc_short band
                0, 1 }; // bc_locals band
        final InputStream in = new ByteArrayInputStream(bytes);
        bcBands.unpack(in);
        assertEquals(4, bcBands.getMethodByteCodePacked()[0][0].length);
        assertEquals(2, bcBands.getBcLocal().length);
        assertEquals(1, bcBands.getBcShort().length);
    }

}
