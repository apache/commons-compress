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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.Attribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPNameAndType;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.ConstantValueAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.DeprecatedAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.EnclosingMethodAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.ExceptionsAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LineNumberTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LocalVariableTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LocalVariableTypeTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.SignatureAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.SourceFileAttribute;

/**
 * Class Bands
 */
public class ClassBands extends BandSet {

    private int[] classFieldCount;

    private long[] classFlags;

    private long[] classAccessFlags; // Access flags for writing to the class
    // file

    private int[][] classInterfacesInts;

    private int[] classMethodCount;

    private int[] classSuperInts;

    private String[] classThis;

    private int[] classThisInts;

    private ArrayList<Attribute>[] classAttributes;

    private int[] classVersionMajor;

    private int[] classVersionMinor;

    private IcTuple[][] icLocal;

    private List<Attribute>[] codeAttributes;

    private int[] codeHandlerCount;

    private int[] codeMaxNALocals;

    private int[] codeMaxStack;

    private ArrayList<Attribute>[][] fieldAttributes;

    private String[][] fieldDescr;

    private int[][] fieldDescrInts;

    private long[][] fieldFlags;

    private long[][] fieldAccessFlags;

    private ArrayList<Attribute>[][] methodAttributes;

    private String[][] methodDescr;

    private int[][] methodDescrInts;

    private long[][] methodFlags;

    private long[][] methodAccessFlags;

    private final AttributeLayoutMap attrMap;

    private final CpBands cpBands;

    private final SegmentOptions options;

    private final int classCount;

    private int[] methodAttrCalls;

    private int[][] codeHandlerStartP;

    private int[][] codeHandlerEndPO;

    private int[][] codeHandlerCatchPO;

    private int[][] codeHandlerClassRCN;

    private boolean[] codeHasAttributes;

    /**
     * @param segment TODO
     */
    public ClassBands(final Segment segment) {
        super(segment);
        this.attrMap = segment.getAttrDefinitionBands().getAttributeDefinitionMap();
        this.cpBands = segment.getCpBands();
        this.classCount = header.getClassCount();
        this.options = header.getOptions();

    }

    private int getCallCount(final int[][] methodAttrIndexes, final long[][] flags, final int context) {
        int callCount = 0;
        for (final int[] element : methodAttrIndexes) {
            for (final int index : element) {
                final AttributeLayout layout = attrMap.getAttributeLayout(index, context);
                callCount += layout.numBackwardsCallables();
            }
        }
        int layoutsUsed = 0;
        for (final long[] flag : flags) {
            for (final long element : flag) {
                layoutsUsed |= element;
            }
        }
        for (int i = 0; i < 26; i++) {
            if ((layoutsUsed & 1 << i) != 0) {
                final AttributeLayout layout = attrMap.getAttributeLayout(i, context);
                callCount += layout.numBackwardsCallables();
            }
        }
        return callCount;
    }

    public ArrayList<Attribute>[] getClassAttributes() {
        return classAttributes;
    }

    public int[] getClassFieldCount() {
        return classFieldCount;
    }

    public long[] getClassFlags() {
        if (classAccessFlags == null) {
            long mask = 0x7FFF;
            for (int i = 0; i < 16; i++) {
                final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_CLASS);
                if (layout != null && !layout.isDefaultLayout()) {
                    mask &= ~(1 << i);
                }
            }
            classAccessFlags = new long[classFlags.length];
            for (int i = 0; i < classFlags.length; i++) {
                classAccessFlags[i] = classFlags[i] & mask;
            }
        }
        return classAccessFlags;
    }

    public int[][] getClassInterfacesInts() {
        return classInterfacesInts;
    }

    public int[] getClassMethodCount() {
        return classMethodCount;
    }

    public int[] getClassSuperInts() {
        return classSuperInts;
    }

    public int[] getClassThisInts() {
        return classThisInts;
    }

    /**
     * Returns null if all classes should use the default major and minor version or an array of integers containing the
     * major version numberss to use for each class in the segment
     *
     * @return Class file major version numbers, or null if none specified
     */
    public int[] getClassVersionMajor() {
        return classVersionMajor;
    }

    /**
     * Returns null if all classes should use the default major and minor version or an array of integers containing the
     * minor version numberss to use for each class in the segment
     *
     * @return Class file minor version numbers, or null if none specified
     */
    public int[] getClassVersionMinor() {
        return classVersionMinor;
    }

    public int[][] getCodeHandlerCatchPO() {
        return codeHandlerCatchPO;
    }

    public int[][] getCodeHandlerClassRCN() {
        return codeHandlerClassRCN;
    }

    public int[] getCodeHandlerCount() {
        return codeHandlerCount;
    }

    public int[][] getCodeHandlerEndPO() {
        return codeHandlerEndPO;
    }

    public int[][] getCodeHandlerStartP() {
        return codeHandlerStartP;
    }

    public boolean[] getCodeHasAttributes() {
        return codeHasAttributes;
    }

    public int[] getCodeMaxNALocals() {
        return codeMaxNALocals;
    }

    public int[] getCodeMaxStack() {
        return codeMaxStack;
    }

    public ArrayList<Attribute>[][] getFieldAttributes() {
        return fieldAttributes;
    }

    public int[][] getFieldDescrInts() {
        return fieldDescrInts;
    }

    public long[][] getFieldFlags() {
        if (fieldAccessFlags == null) {
            long mask = 0x7FFF;
            for (int i = 0; i < 16; i++) {
                final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_FIELD);
                if (layout != null && !layout.isDefaultLayout()) {
                    mask &= ~(1 << i);
                }
            }
            fieldAccessFlags = new long[fieldFlags.length][];
            for (int i = 0; i < fieldFlags.length; i++) {
                fieldAccessFlags[i] = new long[fieldFlags[i].length];
                for (int j = 0; j < fieldFlags[i].length; j++) {
                    fieldAccessFlags[i][j] = fieldFlags[i][j] & mask;
                }
            }
        }
        return fieldAccessFlags;
    }

    public IcTuple[][] getIcLocal() {
        return icLocal;
    }

    public ArrayList<Attribute>[][] getMethodAttributes() {
        return methodAttributes;
    }

    public String[][] getMethodDescr() {
        return methodDescr;
    }

    public int[][] getMethodDescrInts() {
        return methodDescrInts;
    }

    public long[][] getMethodFlags() {
        if (methodAccessFlags == null) {
            long mask = 0x7FFF;
            for (int i = 0; i < 16; i++) {
                final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_METHOD);
                if (layout != null && !layout.isDefaultLayout()) {
                    mask &= ~(1 << i);
                }
            }
            methodAccessFlags = new long[methodFlags.length][];
            for (int i = 0; i < methodFlags.length; i++) {
                methodAccessFlags[i] = new long[methodFlags[i].length];
                for (int j = 0; j < methodFlags[i].length; j++) {
                    methodAccessFlags[i][j] = methodFlags[i][j] & mask;
                }
            }
        }
        return methodAccessFlags;
    }

    /**
     * Gets an ArrayList of ArrayLists which hold the code attributes corresponding to all classes in order.
     *
     * If a class doesn't have any attributes, the corresponding element in this list will be an empty ArrayList.
     *
     * @return ArrayList
     */
    public ArrayList<List<Attribute>> getOrderedCodeAttributes() {
        return Stream.of(codeAttributes).map(ArrayList::new).collect(Collectors.toCollection(ArrayList::new));
    }

    public long[] getRawClassFlags() {
        return classFlags;
    }

    private void parseClassAttrBands(final InputStream in) throws IOException, Pack200Exception {
        final String[] cpUTF8 = cpBands.getCpUTF8();
        final String[] cpClass = cpBands.getCpClass();

        // Prepare empty attribute lists
        classAttributes = new ArrayList[classCount];
        Arrays.setAll(classAttributes, i -> new ArrayList<>());

        classFlags = parseFlags("class_flags", in, classCount, Codec.UNSIGNED5, options.hasClassFlagsHi());
        final int classAttrCount = SegmentUtils.countBit16(classFlags);
        final int[] classAttrCounts = decodeBandInt("class_attr_count", in, Codec.UNSIGNED5, classAttrCount);
        final int[][] classAttrIndexes = decodeBandInt("class_attr_indexes", in, Codec.UNSIGNED5, classAttrCounts);
        final int callCount = getCallCount(classAttrIndexes, new long[][] {classFlags}, AttributeLayout.CONTEXT_CLASS);
        final int[] classAttrCalls = decodeBandInt("class_attr_calls", in, Codec.UNSIGNED5, callCount);

        final AttributeLayout deprecatedLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_DEPRECATED,
            AttributeLayout.CONTEXT_CLASS);

        final AttributeLayout sourceFileLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_SOURCE_FILE,
            AttributeLayout.CONTEXT_CLASS);
        final int sourceFileCount = SegmentUtils.countMatches(classFlags, sourceFileLayout);
        final int[] classSourceFile = decodeBandInt("class_SourceFile_RUN", in, Codec.UNSIGNED5, sourceFileCount);

        final AttributeLayout enclosingMethodLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_ENCLOSING_METHOD, AttributeLayout.CONTEXT_CLASS);
        final int enclosingMethodCount = SegmentUtils.countMatches(classFlags, enclosingMethodLayout);
        final int[] enclosingMethodRC = decodeBandInt("class_EnclosingMethod_RC", in, Codec.UNSIGNED5,
            enclosingMethodCount);
        final int[] enclosingMethodRDN = decodeBandInt("class_EnclosingMethod_RDN", in, Codec.UNSIGNED5,
            enclosingMethodCount);

        final AttributeLayout signatureLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_SIGNATURE,
            AttributeLayout.CONTEXT_CLASS);
        final int signatureCount = SegmentUtils.countMatches(classFlags, signatureLayout);
        final int[] classSignature = decodeBandInt("class_Signature_RS", in, Codec.UNSIGNED5, signatureCount);

        final int backwardsCallsUsed = parseClassMetadataBands(in, classAttrCalls);

        final AttributeLayout innerClassLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_INNER_CLASSES,
            AttributeLayout.CONTEXT_CLASS);
        final int innerClassCount = SegmentUtils.countMatches(classFlags, innerClassLayout);
        final int[] classInnerClassesN = decodeBandInt("class_InnerClasses_N", in, Codec.UNSIGNED5, innerClassCount);
        final int[][] classInnerClassesRC = decodeBandInt("class_InnerClasses_RC", in, Codec.UNSIGNED5,
            classInnerClassesN);
        final int[][] classInnerClassesF = decodeBandInt("class_InnerClasses_F", in, Codec.UNSIGNED5,
            classInnerClassesN);
        int flagsCount = 0;
        for (final int[] element : classInnerClassesF) {
            for (final int element2 : element) {
                if (element2 != 0) {
                    flagsCount++;
                }
            }
        }
        final int[] classInnerClassesOuterRCN = decodeBandInt("class_InnerClasses_outer_RCN", in, Codec.UNSIGNED5,
            flagsCount);
        final int[] classInnerClassesNameRUN = decodeBandInt("class_InnerClasses_name_RUN", in, Codec.UNSIGNED5,
            flagsCount);

        final AttributeLayout versionLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_CLASS_FILE_VERSION,
            AttributeLayout.CONTEXT_CLASS);
        final int versionCount = SegmentUtils.countMatches(classFlags, versionLayout);
        final int[] classFileVersionMinorH = decodeBandInt("class_file_version_minor_H", in, Codec.UNSIGNED5,
            versionCount);
        final int[] classFileVersionMajorH = decodeBandInt("class_file_version_major_H", in, Codec.UNSIGNED5,
            versionCount);
        if (versionCount > 0) {
            classVersionMajor = new int[classCount];
            classVersionMinor = new int[classCount];
        }
        final int defaultVersionMajor = header.getDefaultClassMajorVersion();
        final int defaultVersionMinor = header.getDefaultClassMinorVersion();

        // Parse non-predefined attribute bands
        int backwardsCallIndex = backwardsCallsUsed;
        final int limit = options.hasClassFlagsHi() ? 62 : 31;
        final AttributeLayout[] otherLayouts = new AttributeLayout[limit + 1];
        final int[] counts = new int[limit + 1];
        final List<Attribute>[] otherAttributes = new List[limit + 1];
        for (int i = 0; i < limit; i++) {
            final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_CLASS);
            if (layout != null && !(layout.isDefaultLayout())) {
                otherLayouts[i] = layout;
                counts[i] = SegmentUtils.countMatches(classFlags, layout);
            }
        }
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                final NewAttributeBands bands = attrMap.getAttributeBands(otherLayouts[i]);
                otherAttributes[i] = bands.parseAttributes(in, counts[i]);
                final int numBackwardsCallables = otherLayouts[i].numBackwardsCallables();
                if (numBackwardsCallables > 0) {
                    final int[] backwardsCalls = new int[numBackwardsCallables];
                    System.arraycopy(classAttrCalls, backwardsCallIndex, backwardsCalls, 0, numBackwardsCallables);
                    bands.setBackwardsCalls(backwardsCalls);
                    backwardsCallIndex += numBackwardsCallables;
                }
            }
        }

        // Now process the attribute bands we have parsed
        int sourceFileIndex = 0;
        int enclosingMethodIndex = 0;
        int signatureIndex = 0;
        int innerClassIndex = 0;
        int innerClassC2NIndex = 0;
        int versionIndex = 0;
        icLocal = new IcTuple[classCount][];
        for (int i = 0; i < classCount; i++) {
            final long flag = classFlags[i];
            if (deprecatedLayout.matches(classFlags[i])) {
                classAttributes[i].add(new DeprecatedAttribute());
            }
            if (sourceFileLayout.matches(flag)) {
                final long result = classSourceFile[sourceFileIndex];
                ClassFileEntry value = sourceFileLayout.getValue(result, cpBands.getConstantPool());
                if (value == null) {
                    // Remove package prefix
                    String className = classThis[i].substring(classThis[i].lastIndexOf('/') + 1);
                    className = className.substring(className.lastIndexOf('.') + 1);

                    // Remove mangled nested class names
                    final char[] chars = className.toCharArray();
                    int index = -1;
                    for (int j = 0; j < chars.length; j++) {
                        if (chars[j] <= 0x2D) {
                            index = j;
                            break;
                        }
                    }
                    if (index > -1) {
                        className = className.substring(0, index);
                    }
                    // Add .java to the end
                    value = cpBands.cpUTF8Value(className + ".java", true);
                }
                classAttributes[i].add(new SourceFileAttribute((CPUTF8) value));
                sourceFileIndex++;
            }
            if (enclosingMethodLayout.matches(flag)) {
                final CPClass theClass = cpBands.cpClassValue(enclosingMethodRC[enclosingMethodIndex]);
                CPNameAndType theMethod = null;
                if (enclosingMethodRDN[enclosingMethodIndex] != 0) {
                    theMethod = cpBands.cpNameAndTypeValue(enclosingMethodRDN[enclosingMethodIndex] - 1);
                }
                classAttributes[i].add(new EnclosingMethodAttribute(theClass, theMethod));
                enclosingMethodIndex++;
            }
            if (signatureLayout.matches(flag)) {
                final long result = classSignature[signatureIndex];
                final CPUTF8 value = (CPUTF8) signatureLayout.getValue(result, cpBands.getConstantPool());
                classAttributes[i].add(new SignatureAttribute(value));
                signatureIndex++;
            }
            if (innerClassLayout.matches(flag)) {
                // Just create the tuples for now because the attributes are
                // decided at the end when creating class constant pools
                icLocal[i] = new IcTuple[classInnerClassesN[innerClassIndex]];
                for (int j = 0; j < icLocal[i].length; j++) {
                    final int icTupleCIndex = classInnerClassesRC[innerClassIndex][j];
                    int icTupleC2Index = -1;
                    int icTupleNIndex = -1;

                    final String icTupleC = cpClass[icTupleCIndex];
                    int icTupleF = classInnerClassesF[innerClassIndex][j];
                    String icTupleC2 = null;
                    String icTupleN = null;

                    if (icTupleF != 0) {
                        icTupleC2Index = classInnerClassesOuterRCN[innerClassC2NIndex];
                        icTupleNIndex = classInnerClassesNameRUN[innerClassC2NIndex];
                        icTupleC2 = cpClass[icTupleC2Index];
                        icTupleN = cpUTF8[icTupleNIndex];
                        innerClassC2NIndex++;
                    } else {
                        // Get from icBands
                        final IcBands icBands = segment.getIcBands();
                        final IcTuple[] icAll = icBands.getIcTuples();
                        for (final IcTuple element : icAll) {
                            if (element.getC().equals(icTupleC)) {
                                icTupleF = element.getF();
                                icTupleC2 = element.getC2();
                                icTupleN = element.getN();
                                break;
                            }
                        }
                    }

                    final IcTuple icTuple = new IcTuple(icTupleC, icTupleF, icTupleC2, icTupleN, icTupleCIndex,
                        icTupleC2Index, icTupleNIndex, j);
                    icLocal[i][j] = icTuple;
                }
                innerClassIndex++;
            }
            if (versionLayout.matches(flag)) {
                classVersionMajor[i] = classFileVersionMajorH[versionIndex];
                classVersionMinor[i] = classFileVersionMinorH[versionIndex];
                versionIndex++;
            } else if (classVersionMajor != null) {
                // Fill in with defaults
                classVersionMajor[i] = defaultVersionMajor;
                classVersionMinor[i] = defaultVersionMinor;
            }
            // Non-predefined attributes
            for (int j = 0; j < otherLayouts.length; j++) {
                if (otherLayouts[j] != null && otherLayouts[j].matches(flag)) {
                    // Add the next attribute
                    classAttributes[i].add(otherAttributes[j].get(0));
                    otherAttributes[j].remove(0);
                }
            }
        }
    }

    /**
     * Parse the class metadata bands and return the number of backwards callables.
     *
     * @param in TODO
     * @param classAttrCalls TODO
     * @return the number of backwards callables.
     * @throws Pack200Exception TODO
     * @throws IOException If an I/O error occurs.
     */
    private int parseClassMetadataBands(final InputStream in, final int[] classAttrCalls)
        throws Pack200Exception, IOException {
        int numBackwardsCalls = 0;
        final String[] RxA = {"RVA", "RIA"};

        final AttributeLayout rvaLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_CLASS);
        final AttributeLayout riaLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_CLASS);
        final int rvaCount = SegmentUtils.countMatches(classFlags, rvaLayout);
        final int riaCount = SegmentUtils.countMatches(classFlags, riaLayout);
        final int[] RxACount = {rvaCount, riaCount};
        final int[] backwardsCalls = {0, 0};
        if (rvaCount > 0) {
            numBackwardsCalls++;
            backwardsCalls[0] = classAttrCalls[0];
            if (riaCount > 0) {
                numBackwardsCalls++;
                backwardsCalls[1] = classAttrCalls[1];
            }
        } else if (riaCount > 0) {
            numBackwardsCalls++;
            backwardsCalls[1] = classAttrCalls[0];
        }
        final MetadataBandGroup[] mbgs = parseMetadata(in, RxA, RxACount, backwardsCalls, "class");
        final List<Attribute> rvaAttributes = mbgs[0].getAttributes();
        final List<Attribute> riaAttributes = mbgs[1].getAttributes();
        int rvaAttributesIndex = 0;
        int riaAttributesIndex = 0;
        for (int i = 0; i < classFlags.length; i++) {
            if (rvaLayout.matches(classFlags[i])) {
                classAttributes[i].add(rvaAttributes.get(rvaAttributesIndex++));
            }
            if (riaLayout.matches(classFlags[i])) {
                classAttributes[i].add(riaAttributes.get(riaAttributesIndex++));
            }
        }
        return numBackwardsCalls;
    }

    private void parseCodeAttrBands(final InputStream in, final int codeFlagsCount)
        throws IOException, Pack200Exception {
        final long[] codeFlags = parseFlags("code_flags", in, codeFlagsCount, Codec.UNSIGNED5,
            segment.getSegmentHeader().getOptions().hasCodeFlagsHi());
        final int codeAttrCount = SegmentUtils.countBit16(codeFlags);
        final int[] codeAttrCounts = decodeBandInt("code_attr_count", in, Codec.UNSIGNED5, codeAttrCount);
        final int[][] codeAttrIndexes = decodeBandInt("code_attr_indexes", in, Codec.UNSIGNED5, codeAttrCounts);
        int callCount = 0;
        for (final int[] element : codeAttrIndexes) {
            for (final int index : element) {
                final AttributeLayout layout = attrMap.getAttributeLayout(index, AttributeLayout.CONTEXT_CODE);
                callCount += layout.numBackwardsCallables();
            }
        }
        final int[] codeAttrCalls = decodeBandInt("code_attr_calls", in, Codec.UNSIGNED5, callCount);

        final AttributeLayout lineNumberTableLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_LINE_NUMBER_TABLE, AttributeLayout.CONTEXT_CODE);
        final int lineNumberTableCount = SegmentUtils.countMatches(codeFlags, lineNumberTableLayout);
        final int[] lineNumberTableN = decodeBandInt("code_LineNumberTable_N", in, Codec.UNSIGNED5,
            lineNumberTableCount);
        final int[][] lineNumberTableBciP = decodeBandInt("code_LineNumberTable_bci_P", in, Codec.BCI5,
            lineNumberTableN);
        final int[][] lineNumberTableLine = decodeBandInt("code_LineNumberTable_line", in, Codec.UNSIGNED5,
            lineNumberTableN);

        final AttributeLayout localVariableTableLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_LOCAL_VARIABLE_TABLE, AttributeLayout.CONTEXT_CODE);
        final AttributeLayout localVariableTypeTableLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE, AttributeLayout.CONTEXT_CODE);

        final int lengthLocalVariableNBand = SegmentUtils.countMatches(codeFlags, localVariableTableLayout);
        final int[] localVariableTableN = decodeBandInt("code_LocalVariableTable_N", in, Codec.UNSIGNED5,
            lengthLocalVariableNBand);
        final int[][] localVariableTableBciP = decodeBandInt("code_LocalVariableTable_bci_P", in, Codec.BCI5,
            localVariableTableN);
        final int[][] localVariableTableSpanO = decodeBandInt("code_LocalVariableTable_span_O", in, Codec.BRANCH5,
            localVariableTableN);
        final CPUTF8[][] localVariableTableNameRU = parseCPUTF8References("code_LocalVariableTable_name_RU", in,
            Codec.UNSIGNED5, localVariableTableN);
        final CPUTF8[][] localVariableTableTypeRS = parseCPSignatureReferences("code_LocalVariableTable_type_RS", in,
            Codec.UNSIGNED5, localVariableTableN);
        final int[][] localVariableTableSlot = decodeBandInt("code_LocalVariableTable_slot", in, Codec.UNSIGNED5,
            localVariableTableN);

        final int lengthLocalVariableTypeTableNBand = SegmentUtils.countMatches(codeFlags,
            localVariableTypeTableLayout);
        final int[] localVariableTypeTableN = decodeBandInt("code_LocalVariableTypeTable_N", in, Codec.UNSIGNED5,
            lengthLocalVariableTypeTableNBand);
        final int[][] localVariableTypeTableBciP = decodeBandInt("code_LocalVariableTypeTable_bci_P", in, Codec.BCI5,
            localVariableTypeTableN);
        final int[][] localVariableTypeTableSpanO = decodeBandInt("code_LocalVariableTypeTable_span_O", in,
            Codec.BRANCH5, localVariableTypeTableN);
        final CPUTF8[][] localVariableTypeTableNameRU = parseCPUTF8References("code_LocalVariableTypeTable_name_RU", in,
            Codec.UNSIGNED5, localVariableTypeTableN);
        final CPUTF8[][] localVariableTypeTableTypeRS = parseCPSignatureReferences(
            "code_LocalVariableTypeTable_type_RS", in, Codec.UNSIGNED5, localVariableTypeTableN);
        final int[][] localVariableTypeTableSlot = decodeBandInt("code_LocalVariableTypeTable_slot", in,
            Codec.UNSIGNED5, localVariableTypeTableN);

        // Parse non-predefined attribute bands
        int backwardsCallIndex = 0;
        final int limit = options.hasCodeFlagsHi() ? 62 : 31;
        final AttributeLayout[] otherLayouts = new AttributeLayout[limit + 1];
        final int[] counts = new int[limit + 1];
        final List<Attribute>[] otherAttributes = new List[limit + 1];
        for (int i = 0; i < limit; i++) {
            final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_CODE);
            if (layout != null && !(layout.isDefaultLayout())) {
                otherLayouts[i] = layout;
                counts[i] = SegmentUtils.countMatches(codeFlags, layout);
            }
        }
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                final NewAttributeBands bands = attrMap.getAttributeBands(otherLayouts[i]);
                otherAttributes[i] = bands.parseAttributes(in, counts[i]);
                final int numBackwardsCallables = otherLayouts[i].numBackwardsCallables();
                if (numBackwardsCallables > 0) {
                    final int[] backwardsCalls = new int[numBackwardsCallables];
                    System.arraycopy(codeAttrCalls, backwardsCallIndex, backwardsCalls, 0, numBackwardsCallables);
                    bands.setBackwardsCalls(backwardsCalls);
                    backwardsCallIndex += numBackwardsCallables;
                }
            }
        }

        int lineNumberIndex = 0;
        int lvtIndex = 0;
        int lvttIndex = 0;
        for (int i = 0; i < codeFlagsCount; i++) {
            if (lineNumberTableLayout.matches(codeFlags[i])) {
                final LineNumberTableAttribute lnta = new LineNumberTableAttribute(lineNumberTableN[lineNumberIndex],
                    lineNumberTableBciP[lineNumberIndex], lineNumberTableLine[lineNumberIndex]);
                lineNumberIndex++;
                codeAttributes[i].add(lnta);
            }
            if (localVariableTableLayout.matches(codeFlags[i])) {
                final LocalVariableTableAttribute lvta = new LocalVariableTableAttribute(localVariableTableN[lvtIndex],
                    localVariableTableBciP[lvtIndex], localVariableTableSpanO[lvtIndex],
                    localVariableTableNameRU[lvtIndex], localVariableTableTypeRS[lvtIndex],
                    localVariableTableSlot[lvtIndex]);
                lvtIndex++;
                codeAttributes[i].add(lvta);
            }
            if (localVariableTypeTableLayout.matches(codeFlags[i])) {
                final LocalVariableTypeTableAttribute lvtta = new LocalVariableTypeTableAttribute(
                    localVariableTypeTableN[lvttIndex], localVariableTypeTableBciP[lvttIndex],
                    localVariableTypeTableSpanO[lvttIndex], localVariableTypeTableNameRU[lvttIndex],
                    localVariableTypeTableTypeRS[lvttIndex], localVariableTypeTableSlot[lvttIndex]);
                lvttIndex++;
                codeAttributes[i].add(lvtta);
            }
            // Non-predefined attributes
            for (int j = 0; j < otherLayouts.length; j++) {
                if (otherLayouts[j] != null && otherLayouts[j].matches(codeFlags[i])) {
                    // Add the next attribute
                    codeAttributes[i].add(otherAttributes[j].get(0));
                    otherAttributes[j].remove(0);
                }
            }
        }

    }

    private void parseCodeBands(final InputStream in) throws Pack200Exception, IOException {
        final AttributeLayout layout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_CODE,
            AttributeLayout.CONTEXT_METHOD);

        final int codeCount = SegmentUtils.countMatches(methodFlags, layout);
        final int[] codeHeaders = decodeBandInt("code_headers", in, Codec.BYTE1, codeCount);

        final boolean allCodeHasFlags = segment.getSegmentHeader().getOptions().hasAllCodeFlags();
        if (!allCodeHasFlags) {
            codeHasAttributes = new boolean[codeCount];
        }
        int codeSpecialHeader = 0;
        for (int i = 0; i < codeCount; i++) {
            if (codeHeaders[i] == 0) {
                codeSpecialHeader++;
                if (!allCodeHasFlags) {
                    codeHasAttributes[i] = true;
                }
            }
        }
        final int[] codeMaxStackSpecials = decodeBandInt("code_max_stack", in, Codec.UNSIGNED5, codeSpecialHeader);
        final int[] codeMaxNALocalsSpecials = decodeBandInt("code_max_na_locals", in, Codec.UNSIGNED5,
            codeSpecialHeader);
        final int[] codeHandlerCountSpecials = decodeBandInt("code_handler_count", in, Codec.UNSIGNED5,
            codeSpecialHeader);

        codeMaxStack = new int[codeCount];
        codeMaxNALocals = new int[codeCount];
        codeHandlerCount = new int[codeCount];
        int special = 0;
        for (int i = 0; i < codeCount; i++) {
            final int header = 0xff & codeHeaders[i];
            if (header < 0) {
                throw new IllegalStateException("Shouldn't get here");
            }
            if (header == 0) {
                codeMaxStack[i] = codeMaxStackSpecials[special];
                codeMaxNALocals[i] = codeMaxNALocalsSpecials[special];
                codeHandlerCount[i] = codeHandlerCountSpecials[special];
                special++;
            } else if (header <= 144) {
                codeMaxStack[i] = (header - 1) % 12;
                codeMaxNALocals[i] = (header - 1) / 12;
                codeHandlerCount[i] = 0;
            } else if (header <= 208) {
                codeMaxStack[i] = (header - 145) % 8;
                codeMaxNALocals[i] = (header - 145) / 8;
                codeHandlerCount[i] = 1;
            } else if (header <= 255) {
                codeMaxStack[i] = (header - 209) % 7;
                codeMaxNALocals[i] = (header - 209) / 7;
                codeHandlerCount[i] = 2;
            } else {
                throw new IllegalStateException("Shouldn't get here either");
            }
        }
        codeHandlerStartP = decodeBandInt("code_handler_start_P", in, Codec.BCI5, codeHandlerCount);
        codeHandlerEndPO = decodeBandInt("code_handler_end_PO", in, Codec.BRANCH5, codeHandlerCount);
        codeHandlerCatchPO = decodeBandInt("code_handler_catch_PO", in, Codec.BRANCH5, codeHandlerCount);
        codeHandlerClassRCN = decodeBandInt("code_handler_class_RCN", in, Codec.UNSIGNED5, codeHandlerCount);

        final int codeFlagsCount = allCodeHasFlags ? codeCount : codeSpecialHeader;

        codeAttributes = new List[codeFlagsCount];
        Arrays.setAll(codeAttributes, i -> new ArrayList<>());
        parseCodeAttrBands(in, codeFlagsCount);
    }

    private void parseFieldAttrBands(final InputStream in) throws IOException, Pack200Exception {
        fieldFlags = parseFlags("field_flags", in, classFieldCount, Codec.UNSIGNED5, options.hasFieldFlagsHi());
        final int fieldAttrCount = SegmentUtils.countBit16(fieldFlags);
        final int[] fieldAttrCounts = decodeBandInt("field_attr_count", in, Codec.UNSIGNED5, fieldAttrCount);
        final int[][] fieldAttrIndexes = decodeBandInt("field_attr_indexes", in, Codec.UNSIGNED5, fieldAttrCounts);
        final int callCount = getCallCount(fieldAttrIndexes, fieldFlags, AttributeLayout.CONTEXT_FIELD);
        final int[] fieldAttrCalls = decodeBandInt("field_attr_calls", in, Codec.UNSIGNED5, callCount);

        // Assign empty field attributes
        fieldAttributes = new ArrayList[classCount][];
        for (int i = 0; i < classCount; i++) {
            fieldAttributes[i] = new ArrayList[fieldFlags[i].length];
            for (int j = 0; j < fieldFlags[i].length; j++) {
                fieldAttributes[i][j] = new ArrayList<>();
            }
        }

        final AttributeLayout constantValueLayout = attrMap.getAttributeLayout("ConstantValue",
            AttributeLayout.CONTEXT_FIELD);
        final int constantCount = SegmentUtils.countMatches(fieldFlags, constantValueLayout);
        final int[] field_constantValue_KQ = decodeBandInt("field_ConstantValue_KQ", in, Codec.UNSIGNED5,
            constantCount);
        int constantValueIndex = 0;

        final AttributeLayout signatureLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_SIGNATURE,
            AttributeLayout.CONTEXT_FIELD);
        final int signatureCount = SegmentUtils.countMatches(fieldFlags, signatureLayout);
        final int[] fieldSignatureRS = decodeBandInt("field_Signature_RS", in, Codec.UNSIGNED5, signatureCount);
        int signatureIndex = 0;

        final AttributeLayout deprecatedLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_DEPRECATED,
            AttributeLayout.CONTEXT_FIELD);

        for (int i = 0; i < classCount; i++) {
            for (int j = 0; j < fieldFlags[i].length; j++) {
                final long flag = fieldFlags[i][j];
                if (deprecatedLayout.matches(flag)) {
                    fieldAttributes[i][j].add(new DeprecatedAttribute());
                }
                if (constantValueLayout.matches(flag)) {
                    // we've got a value to read
                    final long result = field_constantValue_KQ[constantValueIndex];
                    final String desc = fieldDescr[i][j];
                    final int colon = desc.indexOf(':');
                    String type = desc.substring(colon + 1);
                    if (type.equals("B") || type.equals("S") || type.equals("C") || type.equals("Z")) {
                        type = "I";
                    }
                    final ClassFileEntry value = constantValueLayout.getValue(result, type, cpBands.getConstantPool());
                    fieldAttributes[i][j].add(new ConstantValueAttribute(value));
                    constantValueIndex++;
                }
                if (signatureLayout.matches(flag)) {
                    // we've got a signature attribute
                    final long result = fieldSignatureRS[signatureIndex];
                    final String desc = fieldDescr[i][j];
                    final int colon = desc.indexOf(':');
                    final String type = desc.substring(colon + 1);
                    final CPUTF8 value = (CPUTF8) signatureLayout.getValue(result, type, cpBands.getConstantPool());
                    fieldAttributes[i][j].add(new SignatureAttribute(value));
                    signatureIndex++;
                }
            }
        }

        // Parse non-predefined attribute bands
        int backwardsCallIndex = parseFieldMetadataBands(in, fieldAttrCalls);
        final int limit = options.hasFieldFlagsHi() ? 62 : 31;
        final AttributeLayout[] otherLayouts = new AttributeLayout[limit + 1];
        final int[] counts = new int[limit + 1];
        final List<Attribute>[] otherAttributes = new List[limit + 1];
        for (int i = 0; i < limit; i++) {
            final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_FIELD);
            if (layout != null && !(layout.isDefaultLayout())) {
                otherLayouts[i] = layout;
                counts[i] = SegmentUtils.countMatches(fieldFlags, layout);
            }
        }
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                final NewAttributeBands bands = attrMap.getAttributeBands(otherLayouts[i]);
                otherAttributes[i] = bands.parseAttributes(in, counts[i]);
                final int numBackwardsCallables = otherLayouts[i].numBackwardsCallables();
                if (numBackwardsCallables > 0) {
                    final int[] backwardsCalls = new int[numBackwardsCallables];
                    System.arraycopy(fieldAttrCalls, backwardsCallIndex, backwardsCalls, 0, numBackwardsCallables);
                    bands.setBackwardsCalls(backwardsCalls);
                    backwardsCallIndex += numBackwardsCallables;
                }
            }
        }

        // Non-predefined attributes
        for (int i = 0; i < classCount; i++) {
            for (int j = 0; j < fieldFlags[i].length; j++) {
                final long flag = fieldFlags[i][j];
                int othersAddedAtStart = 0;
                for (int k = 0; k < otherLayouts.length; k++) {
                    if (otherLayouts[k] != null && otherLayouts[k].matches(flag)) {
                        // Add the next attribute
                        if (otherLayouts[k].getIndex() < 15) {
                            fieldAttributes[i][j].add(othersAddedAtStart++, otherAttributes[k].get(0));
                        } else {
                            fieldAttributes[i][j].add(otherAttributes[k].get(0));
                        }
                        otherAttributes[k].remove(0);
                    }
                }
            }
        }
    }

    private void parseFieldBands(final InputStream in) throws IOException, Pack200Exception {
        fieldDescrInts = decodeBandInt("field_descr", in, Codec.DELTA5, classFieldCount);
        fieldDescr = getReferences(fieldDescrInts, cpBands.getCpDescriptor());
        parseFieldAttrBands(in);
    }

    private int parseFieldMetadataBands(final InputStream in, final int[] fieldAttrCalls)
        throws Pack200Exception, IOException {
        int backwardsCallsUsed = 0;
        final String[] RxA = {"RVA", "RIA"};

        final AttributeLayout rvaLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_FIELD);
        final AttributeLayout riaLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_FIELD);

        final int rvaCount = SegmentUtils.countMatches(fieldFlags, rvaLayout);
        final int riaCount = SegmentUtils.countMatches(fieldFlags, riaLayout);
        final int[] RxACount = {rvaCount, riaCount};
        final int[] backwardsCalls = {0, 0};
        if (rvaCount > 0) {
            backwardsCalls[0] = fieldAttrCalls[0];
            backwardsCallsUsed++;
            if (riaCount > 0) {
                backwardsCalls[1] = fieldAttrCalls[1];
                backwardsCallsUsed++;
            }
        } else if (riaCount > 0) {
            backwardsCalls[1] = fieldAttrCalls[0];
            backwardsCallsUsed++;
        }
        final MetadataBandGroup[] mb = parseMetadata(in, RxA, RxACount, backwardsCalls, "field");
        final List<Attribute> rvaAttributes = mb[0].getAttributes();
        final List<Attribute> riaAttributes = mb[1].getAttributes();
        int rvaAttributesIndex = 0;
        int riaAttributesIndex = 0;
        for (int i = 0; i < fieldFlags.length; i++) {
            for (int j = 0; j < fieldFlags[i].length; j++) {
                if (rvaLayout.matches(fieldFlags[i][j])) {
                    fieldAttributes[i][j].add(rvaAttributes.get(rvaAttributesIndex++));
                }
                if (riaLayout.matches(fieldFlags[i][j])) {
                    fieldAttributes[i][j].add(riaAttributes.get(riaAttributesIndex++));
                }
            }
        }
        return backwardsCallsUsed;
    }

    private MetadataBandGroup[] parseMetadata(final InputStream in, final String[] RxA, final int[] RxACount,
        final int[] backwardsCallCounts, final String contextName) throws IOException, Pack200Exception {
        final MetadataBandGroup[] mbg = new MetadataBandGroup[RxA.length];
        for (int i = 0; i < RxA.length; i++) {
            mbg[i] = new MetadataBandGroup(RxA[i], cpBands);
            final String rxa = RxA[i];
            if (rxa.indexOf('P') >= 0) {
                mbg[i].param_NB = decodeBandInt(contextName + "_" + rxa + "_param_NB", in, Codec.BYTE1, RxACount[i]);
            }
            int pairCount = 0;
            if (!rxa.equals("AD")) {
                mbg[i].anno_N = decodeBandInt(contextName + "_" + rxa + "_anno_N", in, Codec.UNSIGNED5, RxACount[i]);
                mbg[i].type_RS = parseCPSignatureReferences(contextName + "_" + rxa + "_type_RS", in, Codec.UNSIGNED5,
                    mbg[i].anno_N);
                mbg[i].pair_N = decodeBandInt(contextName + "_" + rxa + "_pair_N", in, Codec.UNSIGNED5, mbg[i].anno_N);
                for (final int[] element : mbg[i].pair_N) {
                    for (final int element2 : element) {
                        pairCount += element2;
                    }
                }

                mbg[i].name_RU = parseCPUTF8References(contextName + "_" + rxa + "_name_RU", in, Codec.UNSIGNED5,
                    pairCount);
            } else {
                pairCount = RxACount[i];
            }
            mbg[i].T = decodeBandInt(contextName + "_" + rxa + "_T", in, Codec.BYTE1,
                pairCount + backwardsCallCounts[i]);
            int ICount = 0, DCount = 0, FCount = 0, JCount = 0, cCount = 0, eCount = 0, sCount = 0, arrayCount = 0,
                atCount = 0;
            for (final int element : mbg[i].T) {
                final char c = (char) element;
                switch (c) {
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                case 'Z':
                    ICount++;
                    break;
                case 'D':
                    DCount++;
                    break;
                case 'F':
                    FCount++;
                    break;
                case 'J':
                    JCount++;
                    break;
                case 'c':
                    cCount++;
                    break;
                case 'e':
                    eCount++;
                    break;
                case 's':
                    sCount++;
                    break;
                case '[':
                    arrayCount++;
                    break;
                case '@':
                    atCount++;
                    break;
                }
            }
            mbg[i].caseI_KI = parseCPIntReferences(contextName + "_" + rxa + "_caseI_KI", in, Codec.UNSIGNED5, ICount);
            mbg[i].caseD_KD = parseCPDoubleReferences(contextName + "_" + rxa + "_caseD_KD", in, Codec.UNSIGNED5,
                DCount);
            mbg[i].caseF_KF = parseCPFloatReferences(contextName + "_" + rxa + "_caseF_KF", in, Codec.UNSIGNED5,
                FCount);
            mbg[i].caseJ_KJ = parseCPLongReferences(contextName + "_" + rxa + "_caseJ_KJ", in, Codec.UNSIGNED5, JCount);
            mbg[i].casec_RS = parseCPSignatureReferences(contextName + "_" + rxa + "_casec_RS", in, Codec.UNSIGNED5,
                cCount);
            mbg[i].caseet_RS = parseReferences(contextName + "_" + rxa + "_caseet_RS", in, Codec.UNSIGNED5, eCount,
                cpBands.getCpSignature());
            mbg[i].caseec_RU = parseReferences(contextName + "_" + rxa + "_caseec_RU", in, Codec.UNSIGNED5, eCount,
                cpBands.getCpUTF8());
            mbg[i].cases_RU = parseCPUTF8References(contextName + "_" + rxa + "_cases_RU", in, Codec.UNSIGNED5, sCount);
            mbg[i].casearray_N = decodeBandInt(contextName + "_" + rxa + "_casearray_N", in, Codec.UNSIGNED5,
                arrayCount);
            mbg[i].nesttype_RS = parseCPUTF8References(contextName + "_" + rxa + "_nesttype_RS", in, Codec.UNSIGNED5,
                atCount);
            mbg[i].nestpair_N = decodeBandInt(contextName + "_" + rxa + "_nestpair_N", in, Codec.UNSIGNED5, atCount);
            int nestPairCount = 0;
            for (final int element : mbg[i].nestpair_N) {
                nestPairCount += element;
            }
            mbg[i].nestname_RU = parseCPUTF8References(contextName + "_" + rxa + "_nestname_RU", in, Codec.UNSIGNED5,
                nestPairCount);
        }
        return mbg;
    }

    private void parseMethodAttrBands(final InputStream in) throws IOException, Pack200Exception {
        methodFlags = parseFlags("method_flags", in, classMethodCount, Codec.UNSIGNED5, options.hasMethodFlagsHi());
        final int methodAttrCount = SegmentUtils.countBit16(methodFlags);
        final int[] methodAttrCounts = decodeBandInt("method_attr_count", in, Codec.UNSIGNED5, methodAttrCount);
        final int[][] methodAttrIndexes = decodeBandInt("method_attr_indexes", in, Codec.UNSIGNED5, methodAttrCounts);
        final int callCount = getCallCount(methodAttrIndexes, methodFlags, AttributeLayout.CONTEXT_METHOD);
        methodAttrCalls = decodeBandInt("method_attr_calls", in, Codec.UNSIGNED5, callCount);

        // assign empty method attributes
        methodAttributes = new ArrayList[classCount][];
        for (int i = 0; i < classCount; i++) {
            methodAttributes[i] = new ArrayList[methodFlags[i].length];
            for (int j = 0; j < methodFlags[i].length; j++) {
                methodAttributes[i][j] = new ArrayList<>();
            }
        }

        // Parse method exceptions attributes
        final AttributeLayout methodExceptionsLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_EXCEPTIONS,
            AttributeLayout.CONTEXT_METHOD);
        final int count = SegmentUtils.countMatches(methodFlags, methodExceptionsLayout);
        final int[] numExceptions = decodeBandInt("method_Exceptions_n", in, Codec.UNSIGNED5, count);
        final int[][] methodExceptionsRS = decodeBandInt("method_Exceptions_RC", in, Codec.UNSIGNED5, numExceptions);

        // Parse method signature attributes
        final AttributeLayout methodSignatureLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_SIGNATURE,
            AttributeLayout.CONTEXT_METHOD);
        final int count1 = SegmentUtils.countMatches(methodFlags, methodSignatureLayout);
        final int[] methodSignatureRS = decodeBandInt("method_signature_RS", in, Codec.UNSIGNED5, count1);

        final AttributeLayout deprecatedLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_DEPRECATED,
            AttributeLayout.CONTEXT_METHOD);

        // Add attributes to the attribute arrays
        int methodExceptionsIndex = 0;
        int methodSignatureIndex = 0;
        for (int i = 0; i < methodAttributes.length; i++) {
            for (int j = 0; j < methodAttributes[i].length; j++) {
                final long flag = methodFlags[i][j];
                if (methodExceptionsLayout.matches(flag)) {
                    final int n = numExceptions[methodExceptionsIndex];
                    final int[] exceptions = methodExceptionsRS[methodExceptionsIndex];
                    final CPClass[] exceptionClasses = new CPClass[n];
                    for (int k = 0; k < n; k++) {
                        exceptionClasses[k] = cpBands.cpClassValue(exceptions[k]);
                    }
                    methodAttributes[i][j].add(new ExceptionsAttribute(exceptionClasses));
                    methodExceptionsIndex++;
                }
                if (methodSignatureLayout.matches(flag)) {
                    // We've got a signature attribute
                    final long result = methodSignatureRS[methodSignatureIndex];
                    final String desc = methodDescr[i][j];
                    final int colon = desc.indexOf(':');
                    String type = desc.substring(colon + 1);
                    // TODO Got to get better at this ... in any case, it should
                    // be e.g. KIB or KIH
                    if (type.equals("B") || type.equals("H")) {
                        type = "I";
                    }
                    final CPUTF8 value = (CPUTF8) methodSignatureLayout.getValue(result, type,
                        cpBands.getConstantPool());
                    methodAttributes[i][j].add(new SignatureAttribute(value));
                    methodSignatureIndex++;
                }
                if (deprecatedLayout.matches(flag)) {
                    methodAttributes[i][j].add(new DeprecatedAttribute());
                }
            }
        }

        // Parse non-predefined attribute bands
        int backwardsCallIndex = parseMethodMetadataBands(in, methodAttrCalls);
        final int limit = options.hasMethodFlagsHi() ? 62 : 31;
        final AttributeLayout[] otherLayouts = new AttributeLayout[limit + 1];
        final int[] counts = new int[limit + 1];
        for (int i = 0; i < limit; i++) {
            final AttributeLayout layout = attrMap.getAttributeLayout(i, AttributeLayout.CONTEXT_METHOD);
            if (layout != null && !(layout.isDefaultLayout())) {
                otherLayouts[i] = layout;
                counts[i] = SegmentUtils.countMatches(methodFlags, layout);
            }
        }
        final List<Attribute>[] otherAttributes = new List[limit + 1];
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                final NewAttributeBands bands = attrMap.getAttributeBands(otherLayouts[i]);
                otherAttributes[i] = bands.parseAttributes(in, counts[i]);
                final int numBackwardsCallables = otherLayouts[i].numBackwardsCallables();
                if (numBackwardsCallables > 0) {
                    final int[] backwardsCalls = new int[numBackwardsCallables];
                    System.arraycopy(methodAttrCalls, backwardsCallIndex, backwardsCalls, 0, numBackwardsCallables);
                    bands.setBackwardsCalls(backwardsCalls);
                    backwardsCallIndex += numBackwardsCallables;
                }
            }
        }

        // Non-predefined attributes
        for (int i = 0; i < methodAttributes.length; i++) {
            for (int j = 0; j < methodAttributes[i].length; j++) {
                final long flag = methodFlags[i][j];
                int othersAddedAtStart = 0;
                for (int k = 0; k < otherLayouts.length; k++) {
                    if (otherLayouts[k] != null && otherLayouts[k].matches(flag)) {
                        // Add the next attribute
                        if (otherLayouts[k].getIndex() < 15) {
                            methodAttributes[i][j].add(othersAddedAtStart++, otherAttributes[k].get(0));
                        } else {
                            methodAttributes[i][j].add(otherAttributes[k].get(0));
                        }
                        otherAttributes[k].remove(0);
                    }
                }
            }
        }
    }

    private void parseMethodBands(final InputStream in) throws IOException, Pack200Exception {
        methodDescrInts = decodeBandInt("method_descr", in, Codec.MDELTA5, classMethodCount);
        methodDescr = getReferences(methodDescrInts, cpBands.getCpDescriptor());
        parseMethodAttrBands(in);
    }

    private int parseMethodMetadataBands(final InputStream in, final int[] methodAttrCalls)
        throws Pack200Exception, IOException {
        int backwardsCallsUsed = 0;
        final String[] RxA = {"RVA", "RIA", "RVPA", "RIPA", "AD"};
        final int[] rxaCounts = {0, 0, 0, 0, 0};

        final AttributeLayout rvaLayout = attrMap
            .getAttributeLayout(AttributeLayout.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout riaLayout = attrMap.getAttributeLayout(
            AttributeLayout.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS, AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout rvpaLayout = attrMap.getAttributeLayout(
            AttributeLayout.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS, AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout ripaLayout = attrMap.getAttributeLayout(
            AttributeLayout.ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS, AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout adLayout = attrMap.getAttributeLayout(AttributeLayout.ATTRIBUTE_ANNOTATION_DEFAULT,
            AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout[] rxaLayouts = {rvaLayout, riaLayout, rvpaLayout, ripaLayout, adLayout};

        Arrays.setAll(rxaCounts, i -> SegmentUtils.countMatches(methodFlags, rxaLayouts[i]));
        final int[] backwardsCalls = new int[5];
        int methodAttrIndex = 0;
        for (int i = 0; i < backwardsCalls.length; i++) {
            if (rxaCounts[i] > 0) {
                backwardsCallsUsed++;
                backwardsCalls[i] = methodAttrCalls[methodAttrIndex];
                methodAttrIndex++;
            } else {
                backwardsCalls[i] = 0;
            }
        }
        final MetadataBandGroup[] mbgs = parseMetadata(in, RxA, rxaCounts, backwardsCalls, "method");
        final List<Attribute>[] attributeLists = new List[RxA.length];
        final int[] attributeListIndexes = new int[RxA.length];
        for (int i = 0; i < mbgs.length; i++) {
            attributeLists[i] = mbgs[i].getAttributes();
            attributeListIndexes[i] = 0;
        }
        for (int i = 0; i < methodFlags.length; i++) {
            for (int j = 0; j < methodFlags[i].length; j++) {
                for (int k = 0; k < rxaLayouts.length; k++) {
                    if (rxaLayouts[k].matches(methodFlags[i][j])) {
                        methodAttributes[i][j].add(attributeLists[k].get(attributeListIndexes[k]++));
                    }
                }
            }
        }
        return backwardsCallsUsed;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        final int classCount = header.getClassCount();
        classThisInts = decodeBandInt("class_this", in, Codec.DELTA5, classCount);
        classThis = getReferences(classThisInts, cpBands.getCpClass());
        classSuperInts = decodeBandInt("class_super", in, Codec.DELTA5, classCount);
        final int[] classInterfaceLengths = decodeBandInt("class_interface_count", in, Codec.DELTA5, classCount);
        classInterfacesInts = decodeBandInt("class_interface", in, Codec.DELTA5, classInterfaceLengths);
        classFieldCount = decodeBandInt("class_field_count", in, Codec.DELTA5, classCount);
        classMethodCount = decodeBandInt("class_method_count", in, Codec.DELTA5, classCount);
        parseFieldBands(in);
        parseMethodBands(in);
        parseClassAttrBands(in);
        parseCodeBands(in);

    }

    @Override
    public void unpack() {

    }

}
