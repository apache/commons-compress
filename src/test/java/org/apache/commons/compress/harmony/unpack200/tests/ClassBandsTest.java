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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.ClassBands;
import org.apache.commons.compress.harmony.unpack200.CpBands;
import org.apache.commons.compress.harmony.unpack200.Segment;

/**
 *
 */
public class ClassBandsTest extends AbstractBandsTestCase {

    private String[] cpClasses;
    private String[] cpDescriptor;
    private String[] cpUTF8;

    public class MockCpBands extends CpBands {

        public MockCpBands(Segment segment) {
            super(segment);
        }

        @Override
        public String[] getCpClass() {
            return cpClasses;
        }

        @Override
        public String[] getCpDescriptor() {
            return cpDescriptor;
        }

        @Override
        public String[] getCpUTF8() {
            return cpUTF8;
        }

        @Override
        public int[] getCpInt() {
            return new int[0];
        }

        public double[] getCpDouble() {
            return new double[0];
        }

        @Override
        public long[] getCpLong() {
            return new long[0];
        }

        public float[] getCpFloat() {
            return new float[0];
        }

        @Override
        public String[] getCpSignature() {
            return new String[0];
        }
    }

    public class MockSegment extends AbstractBandsTestCase.MockSegment {

        @Override
        protected CpBands getCpBands() {
            return new MockCpBands(this);
        }
    }

    ClassBands classBands = new ClassBands(new MockSegment());

    public void testSimple() throws IOException, Pack200Exception {
        cpClasses = new String[] { "Class1", "Class2", "Class3", "Interface1",
                "Interface2" };
        cpDescriptor = new String[0];
        cpUTF8 = new String[0];
        byte[] classThis = Codec.DELTA5.encode(1, 0);
        byte[] classSuper = Codec.DELTA5.encode(2, 0);
        byte[] classInterfaceCount = Codec.DELTA5.encode(2, 0);
        byte[] classInterfaceRef1 = encodeBandInt(
                new int[] { 3, 4 }, Codec.DELTA5);
        byte[] classFieldCount = Codec.DELTA5.encode(0, 0);
        byte[] classMethodCount = Codec.DELTA5.encode(0, 0);
        byte[] classFlags = Codec.UNSIGNED5.encode(0, 0);
        byte[][] allArrays = new byte[][] { classThis, classSuper,
                classInterfaceCount, classInterfaceRef1, classFieldCount,
                classMethodCount, classFlags };
        int total = classThis.length + classSuper.length
                + classInterfaceCount.length + classInterfaceRef1.length
                + classFieldCount.length + classMethodCount.length
                + classFlags.length;
        byte[] bytes = new byte[total];
        int index = 0;
        for (byte[] array : allArrays) {
            for (byte element : array) {
                bytes[index] = element;
                index++;
            }
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        classBands.unpack(in);
        assertEquals(1, classBands.getClassThisInts()[0]);
        assertEquals(2, classBands.getClassSuperInts()[0]);
        assertEquals(1, classBands.getClassInterfacesInts().length);
        assertEquals(2, classBands.getClassInterfacesInts()[0].length);
        assertEquals(3, classBands.getClassInterfacesInts()[0][0]);
        assertEquals(4, classBands.getClassInterfacesInts()[0][1]);
        cpClasses = null;
    }

    public void testWithMethods() throws Pack200Exception, IOException {
        cpClasses = new String[] { "Class1", "Class2", "Class3" };
        cpDescriptor = new String[] { "method1", "method2", "method3" };
        cpUTF8 = new String[0];
        byte[] classThis = Codec.DELTA5.encode(1, 0);
        byte[] classSuper = Codec.DELTA5.encode(2, 0);
        byte[] classInterfaceCount = Codec.DELTA5.encode(0, 0);
        byte[] classFieldCount = Codec.DELTA5.encode(0, 0);
        byte[] classMethodCount = Codec.DELTA5.encode(3, 0);
        byte[] methodDescr = encodeBandInt(new int[] { 0, 1, 2 },
                Codec.MDELTA5);
        byte[] methodFlagsLo = encodeBandInt(
                new int[] { 0, 0, 0 }, Codec.UNSIGNED5);
        byte[] classFlags = Codec.UNSIGNED5.encode(0, 0);
        byte[][] allArrays = new byte[][] { classThis, classSuper,
                classInterfaceCount, classFieldCount, classMethodCount,
                methodDescr, methodFlagsLo, classFlags, };
        int total = 0;
        for (byte[] array : allArrays) {
            total += array.length;
        }
        byte[] bytes = new byte[total];
        int index = 0;
        for (byte[] array : allArrays) {
            for (byte element : array) {
                bytes[index] = element;
                index++;
            }
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        classBands.unpack(in);
        assertEquals(1, classBands.getClassThisInts()[0]);
        assertEquals(2, classBands.getClassSuperInts()[0]);
        assertEquals(1, classBands.getMethodDescr().length);
        assertEquals(3, classBands.getMethodDescr()[0].length);
        assertEquals(cpDescriptor[0], classBands.getMethodDescr()[0][0]);
        assertEquals(cpDescriptor[1], classBands.getMethodDescr()[0][1]);
        assertEquals(cpDescriptor[2], classBands.getMethodDescr()[0][2]);

        cpClasses = null;
        cpDescriptor = null;
    }

    public byte[] encodeBandInt(int[] data, BHSDCodec codec)
            throws IOException, Pack200Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < data.length; i++) {
            baos.write(codec.encode(data[i], i == 0 ? 0 : data[i - 1]));
        }
        return baos.toByteArray();
    }


}
