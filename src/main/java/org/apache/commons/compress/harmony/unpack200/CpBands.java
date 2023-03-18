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
package org.apache.commons.compress.harmony.unpack200;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
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

/**
 * Constant Pool bands
 */
public class CpBands extends BandSet {

    private final SegmentConstantPool pool = new SegmentConstantPool(this);

    private String[] cpClass;

    private int[] cpClassInts;
    private int[] cpDescriptorNameInts;
    private int[] cpDescriptorTypeInts;
    private String[] cpDescriptor;
    private double[] cpDouble;
    private String[] cpFieldClass;
    private String[] cpFieldDescriptor;
    private int[] cpFieldClassInts;
    private int[] cpFieldDescriptorInts;
    private float[] cpFloat;
    private String[] cpIMethodClass;
    private String[] cpIMethodDescriptor;
    private int[] cpIMethodClassInts;
    private int[] cpIMethodDescriptorInts;
    private int[] cpInt;
    private long[] cpLong;
    private String[] cpMethodClass;
    private String[] cpMethodDescriptor;
    private int[] cpMethodClassInts;
    private int[] cpMethodDescriptorInts;
    private String[] cpSignature;
    private int[] cpSignatureInts;
    private String[] cpString;
    private int[] cpStringInts;
    private String[] cpUTF8;
    private final Map<String, CPUTF8> stringsToCPUTF8 = new HashMap<>();

    private final Map<String, CPString> stringsToCPStrings = new HashMap<>();
    private final Map<Long, CPLong> longsToCPLongs = new HashMap<>();
    private final Map<Integer, CPInteger> integersToCPIntegers = new HashMap<>();
    private final Map<Float, CPFloat> floatsToCPFloats = new HashMap<>();
    private final Map<String, CPClass> stringsToCPClass = new HashMap<>();
    private final Map<Double, CPDouble> doublesToCPDoubles = new HashMap<>();
    private final Map<String, CPNameAndType> descriptorsToCPNameAndTypes = new HashMap<>();
    private Map<String, Integer> mapClass;

    private Map<String, Integer> mapDescriptor;
    private Map<String, Integer> mapUTF8;
    // TODO: Not used
    private Map<String, Integer> mapSignature;

    private int intOffset;

    private int floatOffset;
    private int longOffset;
    private int doubleOffset;
    private int stringOffset;
    private int classOffset;
    private int signatureOffset;
    private int descrOffset;
    private int fieldOffset;
    private int methodOffset;
    private int imethodOffset;
    public CpBands(final Segment segment) {
        super(segment);
    }

    public CPClass cpClassValue(final int index) {
        final String string = cpClass[index];
        final int utf8Index = cpClassInts[index];
        final int globalIndex = classOffset + index;
        CPClass cpString = stringsToCPClass.get(string);
        if (cpString == null) {
            cpString = new CPClass(cpUTF8Value(utf8Index), globalIndex);
            stringsToCPClass.put(string, cpString);
        }
        return cpString;
    }

    public CPClass cpClassValue(final String string) {
        CPClass cpString = stringsToCPClass.get(string);
        if (cpString == null) {
            final Integer index = mapClass.get(string);
            if (index != null) {
                return cpClassValue(index.intValue());
            }
            cpString = new CPClass(cpUTF8Value(string, false), -1);
            stringsToCPClass.put(string, cpString);
        }
        return cpString;
    }

    public CPDouble cpDoubleValue(final int index) {
        final Double dbl = Double.valueOf(cpDouble[index]);
        CPDouble cpDouble = doublesToCPDoubles.get(dbl);
        if (cpDouble == null) {
            cpDouble = new CPDouble(dbl, index + doubleOffset);
            doublesToCPDoubles.put(dbl, cpDouble);
        }
        return cpDouble;
    }

    public CPFieldRef cpFieldValue(final int index) {
        return new CPFieldRef(cpClassValue(cpFieldClassInts[index]), cpNameAndTypeValue(cpFieldDescriptorInts[index]),
            index + fieldOffset);
    }

    public CPFloat cpFloatValue(final int index) {
        final Float f = Float.valueOf(cpFloat[index]);
        CPFloat cpFloat = floatsToCPFloats.get(f);
        if (cpFloat == null) {
            cpFloat = new CPFloat(f, index + floatOffset);
            floatsToCPFloats.put(f, cpFloat);
        }
        return cpFloat;
    }

    public CPInterfaceMethodRef cpIMethodValue(final int index) {
        return new CPInterfaceMethodRef(cpClassValue(cpIMethodClassInts[index]),
            cpNameAndTypeValue(cpIMethodDescriptorInts[index]), index + imethodOffset);
    }

    public CPInteger cpIntegerValue(final int index) {
        final Integer i = Integer.valueOf(cpInt[index]);
        CPInteger cpInteger = integersToCPIntegers.get(i);
        if (cpInteger == null) {
            cpInteger = new CPInteger(i, index + intOffset);
            integersToCPIntegers.put(i, cpInteger);
        }
        return cpInteger;
    }

    public CPLong cpLongValue(final int index) {
        final Long l = Long.valueOf(cpLong[index]);
        CPLong cpLong = longsToCPLongs.get(l);
        if (cpLong == null) {
            cpLong = new CPLong(l, index + longOffset);
            longsToCPLongs.put(l, cpLong);
        }
        return cpLong;
    }

    public CPMethodRef cpMethodValue(final int index) {
        return new CPMethodRef(cpClassValue(cpMethodClassInts[index]),
            cpNameAndTypeValue(cpMethodDescriptorInts[index]), index + methodOffset);
    }

    public CPNameAndType cpNameAndTypeValue(final int index) {
        final String descriptor = cpDescriptor[index];
        CPNameAndType cpNameAndType = descriptorsToCPNameAndTypes.get(descriptor);
        if (cpNameAndType == null) {
            final int nameIndex = cpDescriptorNameInts[index];
            final int descriptorIndex = cpDescriptorTypeInts[index];

            final CPUTF8 name = cpUTF8Value(nameIndex);
            final CPUTF8 descriptorU = cpSignatureValue(descriptorIndex);
            cpNameAndType = new CPNameAndType(name, descriptorU, index + descrOffset);
            descriptorsToCPNameAndTypes.put(descriptor, cpNameAndType);
        }
        return cpNameAndType;
    }

    public CPNameAndType cpNameAndTypeValue(final String descriptor) {
        CPNameAndType cpNameAndType = descriptorsToCPNameAndTypes.get(descriptor);
        if (cpNameAndType == null) {
            final Integer index = mapDescriptor.get(descriptor);
            if (index != null) {
                return cpNameAndTypeValue(index.intValue());
            }
            final int colon = descriptor.indexOf(':');
            final String nameString = descriptor.substring(0, colon);
            final String descriptorString = descriptor.substring(colon + 1);

            final CPUTF8 name = cpUTF8Value(nameString, true);
            final CPUTF8 descriptorU = cpUTF8Value(descriptorString, true);
            cpNameAndType = new CPNameAndType(name, descriptorU, -1 + descrOffset);
            descriptorsToCPNameAndTypes.put(descriptor, cpNameAndType);
        }
        return cpNameAndType;
    }

    public CPUTF8 cpSignatureValue(final int index) {
        int globalIndex;
        if (cpSignatureInts[index] != -1) {
            globalIndex = cpSignatureInts[index];
        } else {
            globalIndex = index + signatureOffset;
        }
        final String string = cpSignature[index];
        CPUTF8 cpUTF8 = stringsToCPUTF8.get(string);
        if (cpUTF8 == null) {
            cpUTF8 = new CPUTF8(string, globalIndex);
            stringsToCPUTF8.put(string, cpUTF8);
        }
        return cpUTF8;
    }

    public CPString cpStringValue(final int index) {
        final String string = cpString[index];
        final int utf8Index = cpStringInts[index];
        final int globalIndex = stringOffset + index;
        CPString cpString = stringsToCPStrings.get(string);
        if (cpString == null) {
            cpString = new CPString(cpUTF8Value(utf8Index), globalIndex);
            stringsToCPStrings.put(string, cpString);
        }
        return cpString;
    }

    public CPUTF8 cpUTF8Value(final int index) {
        final String string = cpUTF8[index];
        CPUTF8 cputf8 = stringsToCPUTF8.get(string);
        if (cputf8 == null) {
            cputf8 = new CPUTF8(string, index);
            stringsToCPUTF8.put(string, cputf8);
        } else if (cputf8.getGlobalIndex() > index) {
            cputf8.setGlobalIndex(index);
        }
        return cputf8;
    }

    public CPUTF8 cpUTF8Value(final String string) {
        return cpUTF8Value(string, true);
    }

    public CPUTF8 cpUTF8Value(final String string, final boolean searchForIndex) {
        CPUTF8 cputf8 = stringsToCPUTF8.get(string);
        if (cputf8 == null) {
            Integer index = null;
            if (searchForIndex) {
                index = mapUTF8.get(string);
            }
            if (index != null) {
                return cpUTF8Value(index.intValue());
            }
            if (searchForIndex) {
                index = mapSignature.get(string);
            }
            if (index != null) {
                return cpSignatureValue(index.intValue());
            }
            cputf8 = new CPUTF8(string, -1);
            stringsToCPUTF8.put(string, cputf8);
        }
        return cputf8;
    }

    public SegmentConstantPool getConstantPool() {
        return pool;
    }

    public String[] getCpClass() {
        return cpClass;
    }

    public String[] getCpDescriptor() {
        return cpDescriptor;
    }

    public int[] getCpDescriptorNameInts() {
        return cpDescriptorNameInts;
    }

    public int[] getCpDescriptorTypeInts() {
        return cpDescriptorTypeInts;
    }

    public String[] getCpFieldClass() {
        return cpFieldClass;
    }

    public String[] getCpIMethodClass() {
        return cpIMethodClass;
    }

    public int[] getCpInt() {
        return cpInt;
    }

    public long[] getCpLong() {
        return cpLong;
    }

    public String[] getCpMethodClass() {
        return cpMethodClass;
    }

    public String[] getCpMethodDescriptor() {
        return cpMethodDescriptor;
    }

    public String[] getCpSignature() {
        return cpSignature;
    }

    public String[] getCpUTF8() {
        return cpUTF8;
    }

    /**
     * Parses the constant pool class names, using {@link #cpClassCount} to populate {@link #cpClass} from
     * {@link #cpUTF8}.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpClass(final InputStream in) throws IOException, Pack200Exception {
        final int cpClassCount = header.getCpClassCount();
        cpClassInts = decodeBandInt("cp_Class", in, Codec.UDELTA5, cpClassCount);
        cpClass = new String[cpClassCount];
        mapClass = new HashMap<>(cpClassCount);
        for (int i = 0; i < cpClassCount; i++) {
            cpClass[i] = cpUTF8[cpClassInts[i]];
            mapClass.put(cpClass[i], Integer.valueOf(i));
        }
    }

    /**
     * Parses the constant pool descriptor definitions, using {@link #cpDescriptorCount} to populate
     * {@link #cpDescriptor}. For ease of use, the cpDescriptor is stored as a string of the form <i>name:type</i>,
     * largely to make it easier for representing field and method descriptors (e.g.
     * {@code out:java.lang.PrintStream}) in a way that is compatible with passing String arrays.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpDescriptor(final InputStream in) throws IOException, Pack200Exception {
        final int cpDescriptorCount = header.getCpDescriptorCount();
        cpDescriptorNameInts = decodeBandInt("cp_Descr_name", in, Codec.DELTA5, cpDescriptorCount);
        cpDescriptorTypeInts = decodeBandInt("cp_Descr_type", in, Codec.UDELTA5, cpDescriptorCount);
        final String[] cpDescriptorNames = getReferences(cpDescriptorNameInts, cpUTF8);
        final String[] cpDescriptorTypes = getReferences(cpDescriptorTypeInts, cpSignature);
        cpDescriptor = new String[cpDescriptorCount];
        mapDescriptor = new HashMap<>(cpDescriptorCount);
        for (int i = 0; i < cpDescriptorCount; i++) {
            cpDescriptor[i] = cpDescriptorNames[i] + ":" + cpDescriptorTypes[i]; //$NON-NLS-1$
            mapDescriptor.put(cpDescriptor[i], Integer.valueOf(i));
        }
    }

    private void parseCpDouble(final InputStream in) throws IOException, Pack200Exception {
        final int cpDoubleCount = header.getCpDoubleCount();
        final long[] band = parseFlags("cp_Double", in, cpDoubleCount, Codec.UDELTA5, Codec.DELTA5);
        cpDouble = new double[band.length];
        Arrays.setAll(cpDouble, i -> Double.longBitsToDouble(band[i]));
    }

    /**
     * Parses the constant pool field definitions, using {@link #cpFieldCount} to populate {@link #cpFieldClass} and
     * {@link #cpFieldDescriptor}.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpField(final InputStream in) throws IOException, Pack200Exception {
        final int cpFieldCount = header.getCpFieldCount();
        cpFieldClassInts = decodeBandInt("cp_Field_class", in, Codec.DELTA5, cpFieldCount);
        cpFieldDescriptorInts = decodeBandInt("cp_Field_desc", in, Codec.UDELTA5, cpFieldCount);
        cpFieldClass = new String[cpFieldCount];
        cpFieldDescriptor = new String[cpFieldCount];
        for (int i = 0; i < cpFieldCount; i++) {
            cpFieldClass[i] = cpClass[cpFieldClassInts[i]];
            cpFieldDescriptor[i] = cpDescriptor[cpFieldDescriptorInts[i]];
        }
    }

    private void parseCpFloat(final InputStream in) throws IOException, Pack200Exception {
        final int cpFloatCount = header.getCpFloatCount();
        cpFloat = new float[cpFloatCount];
        final int[] floatBits = decodeBandInt("cp_Float", in, Codec.UDELTA5, cpFloatCount);
        for (int i = 0; i < cpFloatCount; i++) {
            cpFloat[i] = Float.intBitsToFloat(floatBits[i]);
        }
    }

    /**
     * Parses the constant pool interface method definitions, using {@link #cpIMethodCount} to populate
     * {@link #cpIMethodClass} and {@link #cpIMethodDescriptor}.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpIMethod(final InputStream in) throws IOException, Pack200Exception {
        final int cpIMethodCount = header.getCpIMethodCount();
        cpIMethodClassInts = decodeBandInt("cp_Imethod_class", in, Codec.DELTA5, cpIMethodCount);
        cpIMethodDescriptorInts = decodeBandInt("cp_Imethod_desc", in, Codec.UDELTA5, cpIMethodCount);
        cpIMethodClass = new String[cpIMethodCount];
        cpIMethodDescriptor = new String[cpIMethodCount];
        for (int i = 0; i < cpIMethodCount; i++) {
            cpIMethodClass[i] = cpClass[cpIMethodClassInts[i]];
            cpIMethodDescriptor[i] = cpDescriptor[cpIMethodDescriptorInts[i]];
        }
    }

    private void parseCpInt(final InputStream in) throws IOException, Pack200Exception {
        final int cpIntCount = header.getCpIntCount();
        cpInt = decodeBandInt("cpInt", in, Codec.UDELTA5, cpIntCount);
    }

    private void parseCpLong(final InputStream in) throws IOException, Pack200Exception {
        final int cpLongCount = header.getCpLongCount();
        cpLong = parseFlags("cp_Long", in, cpLongCount, Codec.UDELTA5, Codec.DELTA5);
    }

    /**
     * Parses the constant pool method definitions, using {@link #cpMethodCount} to populate {@link #cpMethodClass} and
     * {@link #cpMethodDescriptor}.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpMethod(final InputStream in) throws IOException, Pack200Exception {
        final int cpMethodCount = header.getCpMethodCount();
        cpMethodClassInts = decodeBandInt("cp_Method_class", in, Codec.DELTA5, cpMethodCount);
        cpMethodDescriptorInts = decodeBandInt("cp_Method_desc", in, Codec.UDELTA5, cpMethodCount);
        cpMethodClass = new String[cpMethodCount];
        cpMethodDescriptor = new String[cpMethodCount];
        for (int i = 0; i < cpMethodCount; i++) {
            cpMethodClass[i] = cpClass[cpMethodClassInts[i]];
            cpMethodDescriptor[i] = cpDescriptor[cpMethodDescriptorInts[i]];
        }
    }

    /**
     * Parses the constant pool signature classes, using {@link #cpSignatureCount} to populate {@link #cpSignature}. A
     * signature form is akin to the bytecode representation of a class; Z for boolean, I for int, [ for array etc.
     * However, although classes are started with L, the classname does not follow the form; instead, there is a
     * separate array of classes. So an array corresponding to {@code public static void main(String args[])} has a
     * form of {@code [L(V)} and a classes array of {@code [java.lang.String]}. The {@link #cpSignature} is a
     * string representation identical to the bytecode equivalent {@code [Ljava/lang/String;(V)} TODO Check that
     * the form is as above and update other types e.g. J
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpSignature(final InputStream in) throws IOException, Pack200Exception {
        final int cpSignatureCount = header.getCpSignatureCount();
        cpSignatureInts = decodeBandInt("cp_Signature_form", in, Codec.DELTA5, cpSignatureCount);
        final String[] cpSignatureForm = getReferences(cpSignatureInts, cpUTF8);
        cpSignature = new String[cpSignatureCount];
        mapSignature = new HashMap<>();
        int lCount = 0;
        for (int i = 0; i < cpSignatureCount; i++) {
            final String form = cpSignatureForm[i];
            final char[] chars = form.toCharArray();
            for (final char element : chars) {
                if (element == 'L') {
                    cpSignatureInts[i] = -1;
                    lCount++;
                }
            }
        }
        final String[] cpSignatureClasses = parseReferences("cp_Signature_classes", in, Codec.UDELTA5, lCount, cpClass);
        int index = 0;
        for (int i = 0; i < cpSignatureCount; i++) {
            final String form = cpSignatureForm[i];
            final int len = form.length();
            final StringBuilder signature = new StringBuilder(64);
            final ArrayList<String> list = new ArrayList<>();
            for (int j = 0; j < len; j++) {
                final char c = form.charAt(j);
                signature.append(c);
                if (c == 'L') {
                    final String className = cpSignatureClasses[index];
                    list.add(className);
                    signature.append(className);
                    index++;
                }
            }
            cpSignature[i] = signature.toString();
            mapSignature.put(signature.toString(), Integer.valueOf(i));
        }
//        for (int i = 0; i < cpSignatureInts.length; i++) {
//            if (cpSignatureInts[i] == -1) {
//                cpSignatureInts[i] = search(cpUTF8, cpSignature[i]);
//            }
//        }
    }

    /**
     * Parses the constant pool strings, using {@link #cpStringCount} to populate {@link #cpString} from indexes into
     * {@link #cpUTF8}.
     *
     * @param in the input stream to read from
     * @throws IOException if a problem occurs during reading from the underlying stream
     * @throws Pack200Exception if a problem occurs with an unexpected value or unsupported codec
     */
    private void parseCpString(final InputStream in) throws IOException, Pack200Exception {
        final int cpStringCount = header.getCpStringCount();
        cpStringInts = decodeBandInt("cp_String", in, Codec.UDELTA5, cpStringCount);
        cpString = new String[cpStringCount];
        Arrays.setAll(cpString, i -> cpUTF8[cpStringInts[i]]);
    }

    private void parseCpUtf8(final InputStream in) throws IOException, Pack200Exception {
        final int cpUTF8Count = header.getCpUTF8Count();
        cpUTF8 = new String[cpUTF8Count];
        mapUTF8 = new HashMap<>(cpUTF8Count + 1);
        cpUTF8[0] = ""; //$NON-NLS-1$
        mapUTF8.put("", Integer.valueOf(0));
        final int[] prefix = decodeBandInt("cpUTF8Prefix", in, Codec.DELTA5, cpUTF8Count - 2);
        int charCount = 0;
        int bigSuffixCount = 0;
        final int[] suffix = decodeBandInt("cpUTF8Suffix", in, Codec.UNSIGNED5, cpUTF8Count - 1);

        for (final int element : suffix) {
            if (element == 0) {
                bigSuffixCount++;
            } else {
                charCount += element;
            }
        }
        final char[] data = new char[charCount];
        final int[] dataBand = decodeBandInt("cp_Utf8_chars", in, Codec.CHAR3, charCount);
        for (int i = 0; i < data.length; i++) {
            data[i] = (char) dataBand[i];
        }

        // Read in the big suffix data
        final int[] bigSuffixCounts = decodeBandInt("cp_Utf8_big_suffix", in, Codec.DELTA5, bigSuffixCount);
        final int[][] bigSuffixDataBand = new int[bigSuffixCount][];
        for (int i = 0; i < bigSuffixDataBand.length; i++) {
            bigSuffixDataBand[i] = decodeBandInt("cp_Utf8_big_chars " + i, in, Codec.DELTA5, bigSuffixCounts[i]);
        }

        // Convert big suffix data to characters
        final char[][] bigSuffixData = new char[bigSuffixCount][];
        for (int i = 0; i < bigSuffixDataBand.length; i++) {
            bigSuffixData[i] = new char[bigSuffixDataBand[i].length];
            for (int j = 0; j < bigSuffixDataBand[i].length; j++) {
                bigSuffixData[i][j] = (char) bigSuffixDataBand[i][j];
            }
        }
        // Go through the strings
        charCount = 0;
        bigSuffixCount = 0;
        for (int i = 1; i < cpUTF8Count; i++) {
            final String lastString = cpUTF8[i - 1];
            if (suffix[i - 1] == 0) {
                // The big suffix stuff hasn't been tested, and I'll be
                // surprised if it works first time w/o errors ...
                cpUTF8[i] = lastString.substring(0, i > 1 ? prefix[i - 2] : 0)
                    + new String(bigSuffixData[bigSuffixCount++]);
                mapUTF8.put(cpUTF8[i], Integer.valueOf(i));
            } else {
                cpUTF8[i] = lastString.substring(0, i > 1 ? prefix[i - 2] : 0)
                    + new String(data, charCount, suffix[i - 1]);
                charCount += suffix[i - 1];
                mapUTF8.put(cpUTF8[i], Integer.valueOf(i));
            }
        }
    }

    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        parseCpUtf8(in);
        parseCpInt(in);
        parseCpFloat(in);
        parseCpLong(in);
        parseCpDouble(in);
        parseCpString(in);
        parseCpClass(in);
        parseCpSignature(in);
        parseCpDescriptor(in);
        parseCpField(in);
        parseCpMethod(in);
        parseCpIMethod(in);

        intOffset = cpUTF8.length;
        floatOffset = intOffset + cpInt.length;
        longOffset = floatOffset + cpFloat.length;
        doubleOffset = longOffset + cpLong.length;
        stringOffset = doubleOffset + cpDouble.length;
        classOffset = stringOffset + cpString.length;
        signatureOffset = classOffset + cpClass.length;
        descrOffset = signatureOffset + cpSignature.length;
        fieldOffset = descrOffset + cpDescriptor.length;
        methodOffset = fieldOffset + cpFieldClass.length;
        imethodOffset = methodOffset + cpMethodClass.length;
    }

    @Override
    public void unpack() {

    }

}