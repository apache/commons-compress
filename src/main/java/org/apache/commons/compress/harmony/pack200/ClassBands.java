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
package org.apache.commons.compress.harmony.pack200;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands.AttributeDefinition;
import org.apache.commons.compress.harmony.pack200.IcBands.IcTuple;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * Class bands (corresponds to the <code>class_bands</code> set of bands in the
 * pack200 specification)
 */
public class ClassBands extends BandSet {

    private final CpBands cpBands;
    private final AttributeDefinitionBands attrBands;

    private final CPClass[] class_this;
    private final CPClass[] class_super;
    private final CPClass[][] class_interface;
    private final int[] class_interface_count;

    private final int[] major_versions;

    private final long[] class_flags;
    private int[] class_attr_calls;
    private final List classSourceFile = new ArrayList();
    private final List classEnclosingMethodClass = new ArrayList();
    private final List classEnclosingMethodDesc = new ArrayList();
    private final List classSignature = new ArrayList();

    private final IntList classFileVersionMinor = new IntList();
    private final IntList classFileVersionMajor = new IntList();

    private final int[] class_field_count;
    private final CPNameAndType[][] field_descr;
    private final long[][] field_flags;
    private int[] field_attr_calls;
    private final List fieldConstantValueKQ = new ArrayList();
    private final List fieldSignature = new ArrayList();

    private final int[] class_method_count;
    private final CPNameAndType[][] method_descr;
    private final long[][] method_flags;
    private int[] method_attr_calls;
    private final List methodSignature = new ArrayList();
    private final IntList methodExceptionNumber = new IntList();
    private final List methodExceptionClasses = new ArrayList();

    private int[] codeHeaders;
    private final IntList codeMaxStack = new IntList();
    private final IntList codeMaxLocals = new IntList();
    private final IntList codeHandlerCount = new IntList();
    private final List codeHandlerStartP = new ArrayList();
    private final List codeHandlerEndPO = new ArrayList();
    private final List codeHandlerCatchPO = new ArrayList();
    private final List codeHandlerClass = new ArrayList();
    private final List codeFlags = new ArrayList();
    private int[] code_attr_calls;
    private final IntList codeLineNumberTableN = new IntList();
    private final List codeLineNumberTableBciP = new ArrayList();
    private final IntList codeLineNumberTableLine = new IntList();
    private final IntList codeLocalVariableTableN = new IntList();
    private final List codeLocalVariableTableBciP = new ArrayList();
    private final List codeLocalVariableTableSpanO = new ArrayList();
    private final List codeLocalVariableTableNameRU = new ArrayList();
    private final List codeLocalVariableTableTypeRS = new ArrayList();
    private final IntList codeLocalVariableTableSlot = new IntList();
    private final IntList codeLocalVariableTypeTableN = new IntList();
    private final List codeLocalVariableTypeTableBciP = new ArrayList();
    private final List codeLocalVariableTypeTableSpanO = new ArrayList();
    private final List codeLocalVariableTypeTableNameRU = new ArrayList();
    private final List codeLocalVariableTypeTableTypeRS = new ArrayList();
    private final IntList codeLocalVariableTypeTableSlot = new IntList();

    private final MetadataBandGroup class_RVA_bands;
    private final MetadataBandGroup class_RIA_bands;
    private final MetadataBandGroup field_RVA_bands;
    private final MetadataBandGroup field_RIA_bands;
    private final MetadataBandGroup method_RVA_bands;
    private final MetadataBandGroup method_RIA_bands;
    private final MetadataBandGroup method_RVPA_bands;
    private final MetadataBandGroup method_RIPA_bands;
    private final MetadataBandGroup method_AD_bands;

    private final List classAttributeBands = new ArrayList();
    private final List methodAttributeBands = new ArrayList();
    private final List fieldAttributeBands = new ArrayList();
    private final List codeAttributeBands = new ArrayList();

    private final List tempFieldFlags = new ArrayList();
    private final List tempFieldDesc = new ArrayList();
    private final List tempMethodFlags = new ArrayList();
    private final List tempMethodDesc = new ArrayList();
    private TempParamAnnotation tempMethodRVPA;
    private TempParamAnnotation tempMethodRIPA;

    private boolean anySyntheticClasses = false;
    private boolean anySyntheticFields = false;
    private boolean anySyntheticMethods = false;
    private final Segment segment;

    private final Map classReferencesInnerClass = new HashMap();
    private final boolean stripDebug;

    private int index = 0;

    private int numMethodArgs = 0;
    private int[] class_InnerClasses_N;
    private CPClass[] class_InnerClasses_RC;
    private int[] class_InnerClasses_F;
    private List classInnerClassesOuterRCN;
    private List classInnerClassesNameRUN;

    public ClassBands(Segment segment, int numClasses, int effort, boolean stripDebug) throws IOException {
        super(effort, segment.getSegmentHeader());
        this.stripDebug = stripDebug;
        this.segment = segment;
        this.cpBands = segment.getCpBands();
        this.attrBands = segment.getAttrBands();
        class_this = new CPClass[numClasses];
        class_super = new CPClass[numClasses];
        class_interface_count = new int[numClasses];
        class_interface = new CPClass[numClasses][];
        class_field_count = new int[numClasses];
        class_method_count = new int[numClasses];
        field_descr = new CPNameAndType[numClasses][];
        field_flags = new long[numClasses][];
        method_descr = new CPNameAndType[numClasses][];
        method_flags = new long[numClasses][];
        for (int i = 0; i < numClasses; i++) {
            field_flags[i] = new long[0];
            method_flags[i] = new long[0];
        }
        // minor_versions = new int[numClasses];
        major_versions = new int[numClasses];
        class_flags = new long[numClasses];

        class_RVA_bands = new MetadataBandGroup("RVA", MetadataBandGroup.CONTEXT_CLASS, cpBands, segmentHeader, effort);
        class_RIA_bands = new MetadataBandGroup("RIA", MetadataBandGroup.CONTEXT_CLASS, cpBands, segmentHeader, effort);
        field_RVA_bands = new MetadataBandGroup("RVA", MetadataBandGroup.CONTEXT_FIELD, cpBands, segmentHeader, effort);
        field_RIA_bands = new MetadataBandGroup("RIA", MetadataBandGroup.CONTEXT_FIELD, cpBands, segmentHeader, effort);
        method_RVA_bands = new MetadataBandGroup("RVA", MetadataBandGroup.CONTEXT_METHOD, cpBands, segmentHeader, effort);
        method_RIA_bands = new MetadataBandGroup("RIA", MetadataBandGroup.CONTEXT_METHOD, cpBands, segmentHeader, effort);
        method_RVPA_bands = new MetadataBandGroup("RVPA", MetadataBandGroup.CONTEXT_METHOD, cpBands, segmentHeader, effort);
        method_RIPA_bands = new MetadataBandGroup("RIPA", MetadataBandGroup.CONTEXT_METHOD, cpBands, segmentHeader, effort);
        method_AD_bands = new MetadataBandGroup("AD", MetadataBandGroup.CONTEXT_METHOD, cpBands, segmentHeader, effort);

        createNewAttributeBands();
    }

    private void createNewAttributeBands() throws IOException {
        List classAttributeLayouts = attrBands.getClassAttributeLayouts();
        for (Iterator iterator = classAttributeLayouts.iterator(); iterator.hasNext();) {
            AttributeDefinition def = (AttributeDefinition) iterator.next();
            classAttributeBands.add(new NewAttributeBands(effort, cpBands, segment.getSegmentHeader(), def));
        }
        List methodAttributeLayouts = attrBands.getMethodAttributeLayouts();
        for (Iterator iterator = methodAttributeLayouts.iterator(); iterator.hasNext();) {
            AttributeDefinition def = (AttributeDefinition) iterator.next();
            methodAttributeBands.add(new NewAttributeBands(effort, cpBands, segment.getSegmentHeader(), def));
        }
        List fieldAttributeLayouts = attrBands.getFieldAttributeLayouts();
        for (Iterator iterator = fieldAttributeLayouts.iterator(); iterator.hasNext();) {
            AttributeDefinition def = (AttributeDefinition) iterator.next();
            fieldAttributeBands.add(new NewAttributeBands(effort, cpBands, segment.getSegmentHeader(), def));
        }
        List codeAttributeLayouts = attrBands.getCodeAttributeLayouts();
        for (Iterator iterator = codeAttributeLayouts.iterator(); iterator.hasNext();) {
            AttributeDefinition def = (AttributeDefinition) iterator.next();
            codeAttributeBands.add(new NewAttributeBands(effort, cpBands, segment.getSegmentHeader(), def));
        }
    }

    public void addClass(int major, int flags, String className,
            String signature, String superName, String[] interfaces) {
        class_this[index] = cpBands.getCPClass(className);
        class_super[index] = cpBands.getCPClass(superName);
        class_interface_count[index] = interfaces.length;
        class_interface[index] = new CPClass[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            class_interface[index][i] = cpBands.getCPClass(interfaces[i]);
        }
        major_versions[index] = major;
        class_flags[index] = flags;
        if(!anySyntheticClasses && ((flags & (1 << 12)) != 0) && segment.getCurrentClassReader().hasSyntheticAttributes()) {
            cpBands.addCPUtf8("Synthetic");
            anySyntheticClasses = true;
        }
        if((flags & Opcodes.ACC_DEPRECATED) != 0) { // ASM uses (1<<17) flag for deprecated
            flags = flags & ~Opcodes.ACC_DEPRECATED;
            flags = flags | (1<<20);
        }
        if(signature != null) {
            class_flags[index] |= (1 << 19);
            classSignature.add(cpBands.getCPSignature(signature));
        }
    }

    public void currentClassReferencesInnerClass(CPClass inner) {
        if(!(index >= class_this.length)) {
            CPClass currentClass = class_this[index];
            if(currentClass != null && !currentClass.equals(inner) && !isInnerClassOf(currentClass.toString(), inner)) {
                Set referencedInnerClasses = (Set)classReferencesInnerClass.get(currentClass);
                if(referencedInnerClasses == null) {
                    referencedInnerClasses = new HashSet();
                    classReferencesInnerClass.put(currentClass, referencedInnerClasses);
                }
                referencedInnerClasses.add(inner);
            }
        }
    }

    private boolean isInnerClassOf(String possibleInner, CPClass possibleOuter) {
        if(isInnerClass(possibleInner)) {
            String superClassName = possibleInner.substring(0, possibleInner.lastIndexOf('$'));
            if(superClassName.equals(possibleOuter.toString())) {
                return true;
            } else { // do this recursively
                return isInnerClassOf(superClassName, possibleOuter);
            }
        }
        return false;
    }

    private boolean isInnerClass(String possibleInner) {
        return possibleInner.indexOf('$') != -1;
    }

    public void addField(int flags, String name, String desc, String signature,
            Object value) {
        flags = flags & 0xFFFF;
        tempFieldDesc.add(cpBands.getCPNameAndType(name, desc));
        if (signature != null) {
            fieldSignature.add(cpBands.getCPSignature(signature));
            flags |= (1 << 19);
        }
        if((flags & Opcodes.ACC_DEPRECATED) != 0) { // ASM uses (1<<17) flag for deprecated
            flags = flags & ~Opcodes.ACC_DEPRECATED;
            flags = flags | (1<<20);
        }
        if (value != null) {
            fieldConstantValueKQ.add(cpBands.getConstant(value));
            flags |= (1 << 17);
        }
        if(!anySyntheticFields && ((flags & (1 << 12)) != 0) && segment.getCurrentClassReader().hasSyntheticAttributes()) {
            cpBands.addCPUtf8("Synthetic");
            anySyntheticFields = true;
        }
        tempFieldFlags.add(new Long(flags));
    }

    /**
     * All input classes for the segment have now been read in, so this method
     * is called so that this class can calculate/complete anything it could not
     * do while classes were being read.
     */
    public void finaliseBands() {
        int defaultMajorVersion = segmentHeader.getDefaultMajorVersion();
        for (int i = 0; i < class_flags.length; i++) {
            int major = major_versions[i];
            if (major != defaultMajorVersion) {
                class_flags[i] |= 1 << 24;
                classFileVersionMajor.add(major);
                classFileVersionMinor.add(0);
            }
        }
        // Calculate code headers
        codeHeaders = new int[codeHandlerCount.size()];
        int removed = 0;
        for (int i = 0; i < codeHeaders.length; i++) {
            int numHandlers = codeHandlerCount.get(i - removed);
            int maxLocals = codeMaxLocals.get(i - removed);
            int maxStack = codeMaxStack.get(i - removed);
            if (numHandlers == 0) {
                int header = maxLocals * 12 + maxStack + 1;
                if (header < 145 && maxStack < 12) {
                    codeHeaders[i] = header;
                }
            } else if (numHandlers == 1) {
                int header = maxLocals * 8 + maxStack + 145;
                if (header < 209 && maxStack < 8) {
                    codeHeaders[i] = header;
                }
            } else if (numHandlers == 2) {
                int header = maxLocals * 7 + maxStack + 209;
                if (header < 256 && maxStack < 7) {
                    codeHeaders[i] = header;
                }
            }
            if (codeHeaders[i] != 0) { // Remove the redundant values from
                                        // codeHandlerCount, codeMaxLocals and
                                        // codeMaxStack
                codeHandlerCount.remove(i - removed);
                codeMaxLocals.remove(i - removed);
                codeMaxStack.remove(i - removed);
                removed++;
            } else if (!segment.getSegmentHeader().have_all_code_flags()) {
                codeFlags.add(new Long(0));
            }
        }

        // Compute any required IcLocals
        IntList innerClassesN = new IntList();
        List icLocal = new ArrayList();
        for (int i = 0; i < class_this.length; i++) {
            CPClass cpClass = class_this[i];
            Set referencedInnerClasses = (Set) classReferencesInnerClass.get(cpClass);
            if(referencedInnerClasses != null) {
                int innerN = 0;
                List innerClasses = segment.getIcBands().getInnerClassesForOuter(cpClass.toString());
                if(innerClasses != null) {
                    for (Iterator iterator2 = innerClasses.iterator(); iterator2
                            .hasNext();) {
                        referencedInnerClasses.remove(((IcTuple)iterator2.next()).C);
                    }
                }
                for (Iterator iterator2 = referencedInnerClasses.iterator(); iterator2
                        .hasNext();) {
                    CPClass inner = (CPClass) iterator2.next();
                    IcTuple icTuple = segment.getIcBands().getIcTuple(inner);
                    if(icTuple != null && ! icTuple.isAnonymous()) {
                        // should transmit an icLocal entry
                        icLocal.add(icTuple);
                        innerN++;
                    }
                }
                if(innerN != 0) {
                    innerClassesN.add(innerN);
                    class_flags[i] |= (1 << 23);
                }
            }
        }
        class_InnerClasses_N = innerClassesN.toArray();
        class_InnerClasses_RC = new CPClass[icLocal.size()];
        class_InnerClasses_F = new int[icLocal.size()];
        classInnerClassesOuterRCN = new ArrayList();
        classInnerClassesNameRUN = new ArrayList();
        for (int i = 0; i < class_InnerClasses_RC.length; i++) {
            IcTuple icTuple = (IcTuple) icLocal.get(i);
            class_InnerClasses_RC[i] = (icTuple.C);
            if(icTuple.C2 == null && icTuple.N == null) {
                class_InnerClasses_F[i] = 0;
            } else {
                if (icTuple.F == 0) {
                    class_InnerClasses_F[i] = 0x00010000;
                } else {
                    class_InnerClasses_F[i] = icTuple.F;
                }
                classInnerClassesOuterRCN.add(icTuple.C2);
                classInnerClassesNameRUN.add(icTuple.N);
            }
        }
        // Calculate any backwards calls from metadata bands
        IntList classAttrCalls = new IntList();
        IntList fieldAttrCalls = new IntList();
        IntList methodAttrCalls = new IntList();
        IntList codeAttrCalls = new IntList();

        if(class_RVA_bands.hasContent()) {
            classAttrCalls.add(class_RVA_bands.numBackwardsCalls());
        }
        if(class_RIA_bands.hasContent()) {
            classAttrCalls.add(class_RIA_bands.numBackwardsCalls());
        }
        if(field_RVA_bands.hasContent()) {
            fieldAttrCalls.add(field_RVA_bands.numBackwardsCalls());
        }
        if(field_RIA_bands.hasContent()) {
            fieldAttrCalls.add(field_RIA_bands.numBackwardsCalls());
        }
        if(method_RVA_bands.hasContent()) {
            methodAttrCalls.add(method_RVA_bands.numBackwardsCalls());
        }
        if(method_RIA_bands.hasContent()) {
            methodAttrCalls.add(method_RIA_bands.numBackwardsCalls());
        }
        if(method_RVPA_bands.hasContent()) {
            methodAttrCalls.add(method_RVPA_bands.numBackwardsCalls());
        }
        if(method_RIPA_bands.hasContent()) {
            methodAttrCalls.add(method_RIPA_bands.numBackwardsCalls());
        }
        if(method_AD_bands.hasContent()) {
            methodAttrCalls.add(method_AD_bands.numBackwardsCalls());
        }

        // Sort non-predefined attribute bands
        Comparator comparator = new Comparator() {
            public int compare(Object arg0, Object arg1) {
                NewAttributeBands bands0 = (NewAttributeBands)arg0;
                NewAttributeBands bands1 = (NewAttributeBands)arg1;
                return bands0.getFlagIndex() - bands1.getFlagIndex();
            }
        };
        Collections.sort(classAttributeBands, comparator);
        Collections.sort(methodAttributeBands, comparator);
        Collections.sort(fieldAttributeBands, comparator);
        Collections.sort(codeAttributeBands, comparator);

        for (Iterator iterator = classAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.isUsedAtLeastOnce()) {
                int[] backwardsCallCounts = bands.numBackwardsCalls();
                for (int i = 0; i < backwardsCallCounts.length; i++) {
                    classAttrCalls.add(backwardsCallCounts[i]);
                }
            }
        }
        for (Iterator iterator = methodAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.isUsedAtLeastOnce()) {
                int[] backwardsCallCounts = bands.numBackwardsCalls();
                for (int i = 0; i < backwardsCallCounts.length; i++) {
                    methodAttrCalls.add(backwardsCallCounts[i]);
                }
            }
        }
        for (Iterator iterator = fieldAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.isUsedAtLeastOnce()) {
                int[] backwardsCallCounts = bands.numBackwardsCalls();
                for (int i = 0; i < backwardsCallCounts.length; i++) {
                    fieldAttrCalls.add(backwardsCallCounts[i]);
                }
            }
        }
        for (Iterator iterator = codeAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.isUsedAtLeastOnce()) {
                int[] backwardsCallCounts = bands.numBackwardsCalls();
                for (int i = 0; i < backwardsCallCounts.length; i++) {
                    codeAttrCalls.add(backwardsCallCounts[i]);
                }
            }
        }

        class_attr_calls = classAttrCalls.toArray();
        field_attr_calls = fieldAttrCalls.toArray();
        method_attr_calls = methodAttrCalls.toArray();
        code_attr_calls = codeAttrCalls.toArray();
    }

    public void pack(OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing class bands...");

        byte[] encodedBand = encodeBandInt("class_this", getInts(class_this),
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_this[" + class_this.length + "]");

        encodedBand = encodeBandInt("class_super", getInts(class_super),
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_super[" + class_super.length + "]");

        encodedBand = encodeBandInt("class_interface_count",
                class_interface_count, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_interface_count["
                + class_interface_count.length + "]");

        int totalInterfaces = sum(class_interface_count);
        int[] classInterface = new int[totalInterfaces];
        int k = 0;
        for (int i = 0; i < class_interface.length; i++) {
            if (class_interface[i] != null) {
                for (int j = 0; j < class_interface[i].length; j++) {
                    CPClass cpClass = class_interface[i][j];
                    classInterface[k] = cpClass.getIndex();
                    k++;
                }
            }
        }

        encodedBand = encodeBandInt("class_interface", classInterface,
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_interface[" + classInterface.length + "]");

        encodedBand = encodeBandInt("class_field_count", class_field_count,
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_field_count[" + class_field_count.length
                + "]");

        encodedBand = encodeBandInt("class_method_count", class_method_count,
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_method_count[" + class_method_count.length
                + "]");

        int totalFields = sum(class_field_count);
        int[] fieldDescr = new int[totalFields];
        k = 0;
        for (int i = 0; i < index; i++) {
            for (int j = 0; j < field_descr[i].length; j++) {
                CPNameAndType descr = field_descr[i][j];
                fieldDescr[k] = descr.getIndex();
                k++;
            }
        }

        encodedBand = encodeBandInt("field_descr", fieldDescr, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from field_descr[" + fieldDescr.length + "]");

        writeFieldAttributeBands(out);

        int totalMethods = sum(class_method_count);
        int[] methodDescr = new int[totalMethods];
        k = 0;
        for (int i = 0; i < index; i++) {
            for (int j = 0; j < method_descr[i].length; j++) {
                CPNameAndType descr = method_descr[i][j];
                methodDescr[k] = descr.getIndex();
                k++;
            }
        }

        encodedBand = encodeBandInt("method_descr", methodDescr, Codec.MDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from method_descr[" + methodDescr.length + "]");

        writeMethodAttributeBands(out);
        writeClassAttributeBands(out);
        writeCodeBands(out);
    }

    private int sum(int[] ints) {
        int sum = 0;
        for (int i = 0; i < ints.length; i++) {
            sum += ints[i];
        }
        return sum;
    }

    private void writeFieldAttributeBands(OutputStream out) throws IOException,
            Pack200Exception {
        byte[] encodedBand = encodeFlags("field_flags", field_flags,
                Codec.UNSIGNED5, Codec.UNSIGNED5, segmentHeader
                        .have_field_flags_hi());
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from field_flags[" + field_flags.length + "]");

        // *field_attr_count :UNSIGNED5 [COUNT(1<<16,...)]
        // *field_attr_indexes :UNSIGNED5 [SUM(*field_attr_count)]
        encodedBand = encodeBandInt("field_attr_calls", field_attr_calls,
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from field_attr_calls[" + field_attr_calls.length
                + "]");

        encodedBand = encodeBandInt("fieldConstantValueKQ",
                cpEntryListToArray(fieldConstantValueKQ), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from fieldConstantValueKQ["
                + fieldConstantValueKQ.size() + "]");

        encodedBand = encodeBandInt("fieldSignature",
                cpEntryListToArray(fieldSignature), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from fieldSignature[" + fieldSignature.size() + "]");

        field_RVA_bands.pack(out);
        field_RIA_bands.pack(out);
        for (Iterator iterator = fieldAttributeBands.iterator(); iterator
                .hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            bands.pack(out);
        }
    }

    private void writeMethodAttributeBands(OutputStream out)
            throws IOException, Pack200Exception {
        byte[] encodedBand = encodeFlags("method_flags", method_flags,
                Codec.UNSIGNED5, Codec.UNSIGNED5, segmentHeader
                        .have_method_flags_hi());
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from method_flags[" + method_flags.length + "]");

        // *method_attr_count :UNSIGNED5 [COUNT(1<<16,...)]
        // *method_attr_indexes :UNSIGNED5 [SUM(*method_attr_count)]
        encodedBand = encodeBandInt("method_attr_calls", method_attr_calls,
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from method_attr_calls[" + method_attr_calls.length
                + "]");

        encodedBand = encodeBandInt("methodExceptionNumber",
                methodExceptionNumber.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from methodExceptionNumber["
                + methodExceptionNumber.size() + "]");

        encodedBand = encodeBandInt("methodExceptionClasses",
                cpEntryListToArray(methodExceptionClasses), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from methodExceptionClasses["
                + methodExceptionClasses.size() + "]");

        encodedBand = encodeBandInt("methodSignature",
                cpEntryListToArray(methodSignature), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils
                .log("Wrote " + encodedBand.length
                        + " bytes from methodSignature["
                        + methodSignature.size() + "]");

        method_RVA_bands.pack(out);
        method_RIA_bands.pack(out);
        method_RVPA_bands.pack(out);
        method_RIPA_bands.pack(out);
        method_AD_bands.pack(out);
        for (Iterator iterator = methodAttributeBands.iterator(); iterator
                .hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            bands.pack(out);
        }
    }

    private void writeClassAttributeBands(OutputStream out) throws IOException,
            Pack200Exception {
        byte[] encodedBand = encodeFlags("class_flags", class_flags,
                Codec.UNSIGNED5, Codec.UNSIGNED5, segmentHeader
                        .have_class_flags_hi());
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_flags[" + class_flags.length + "]");

        // These bands are not needed, but could be used to reduce the size of
        // the archive if there are enough different non-standard attributes
        // defined that segmentHeader.have_class_flags_hi() is true. The same
        // applies to method_attr_count, field_attr_count, code_attr_count etc.

        // *class_attr_count :UNSIGNED5 [COUNT(1<<16,...)]
        // *class_attr_indexes :UNSIGNED5 [SUM(*class_attr_count)]

        encodedBand = encodeBandInt("class_attr_calls", class_attr_calls,
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_attr_calls[" + class_attr_calls.length
                + "]");

        encodedBand = encodeBandInt("classSourceFile",
                cpEntryOrNullListToArray(classSourceFile), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils
                .log("Wrote " + encodedBand.length
                        + " bytes from classSourceFile["
                        + classSourceFile.size() + "]");

        encodedBand = encodeBandInt("class_enclosing_method_RC",
                cpEntryListToArray(classEnclosingMethodClass), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_enclosing_method_RC["
                + classEnclosingMethodClass.size() + "]");

        encodedBand = encodeBandInt("class_EnclosingMethod_RDN",
                cpEntryOrNullListToArray(classEnclosingMethodDesc),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_EnclosingMethod_RDN["
                + classEnclosingMethodDesc.size() + "]");

        encodedBand = encodeBandInt("class_Signature_RS",
                cpEntryListToArray(classSignature), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_Signature_RS[" + classSignature.size()
                + "]");

        class_RVA_bands.pack(out);
        class_RIA_bands.pack(out);

        encodedBand = encodeBandInt("class_InnerClasses_N",
                class_InnerClasses_N, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_InnerClasses_N["
                + class_InnerClasses_N.length + "]");

        encodedBand = encodeBandInt("class_InnerClasses_RC",
                getInts(class_InnerClasses_RC), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_InnerClasses_RC["
                + class_InnerClasses_RC.length + "]");

        encodedBand = encodeBandInt("class_InnerClasses_F",
                class_InnerClasses_F, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_InnerClasses_F["
                + class_InnerClasses_F.length + "]");

        encodedBand = encodeBandInt("class_InnerClasses_outer_RCN",
                cpEntryOrNullListToArray(classInnerClassesOuterRCN),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_InnerClasses_outer_RCN["
                + classInnerClassesOuterRCN.size() + "]");

        encodedBand = encodeBandInt("class_InnerClasses_name_RUN",
                cpEntryOrNullListToArray(classInnerClassesNameRUN),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from class_InnerClasses_name_RUN["
                + classInnerClassesNameRUN.size() + "]");

        encodedBand = encodeBandInt("classFileVersionMinor",
                classFileVersionMinor.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from classFileVersionMinor["
                + classFileVersionMinor.size() + "]");

        encodedBand = encodeBandInt("classFileVersionMajor",
                classFileVersionMajor.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from classFileVersionMajor["
                + classFileVersionMajor.size() + "]");

        for (Iterator iterator = classAttributeBands.iterator(); iterator
                .hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            bands.pack(out);
        }
    }

    private int[] getInts(CPClass[] cpClasses) {
        int[] ints = new int[cpClasses.length];
        for (int i = 0; i < ints.length; i++) {
            if(cpClasses[i] != null) {
                ints[i] = cpClasses[i].getIndex();
            }
        }
        return ints;
    }

    private void writeCodeBands(OutputStream out) throws IOException,
            Pack200Exception {
        byte[] encodedBand = encodeBandInt("codeHeaders", codeHeaders,
                Codec.BYTE1);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHeaders[" + codeHeaders.length + "]");

        encodedBand = encodeBandInt("codeMaxStack", codeMaxStack.toArray(),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeMaxStack[" + codeMaxStack.size() + "]");

        encodedBand = encodeBandInt("codeMaxLocals", codeMaxLocals.toArray(),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeMaxLocals[" + codeMaxLocals.size() + "]");

        encodedBand = encodeBandInt("codeHandlerCount", codeHandlerCount
                .toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHandlerCount[" + codeHandlerCount.size()
                + "]");

        encodedBand = encodeBandInt("codeHandlerStartP",
                integerListToArray(codeHandlerStartP), Codec.BCI5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHandlerStartP[" + codeHandlerStartP.size()
                + "]");

        encodedBand = encodeBandInt("codeHandlerEndPO",
                integerListToArray(codeHandlerEndPO), Codec.BRANCH5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHandlerEndPO[" + codeHandlerEndPO.size()
                + "]");

        encodedBand = encodeBandInt("codeHandlerCatchPO",
                integerListToArray(codeHandlerCatchPO), Codec.BRANCH5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHandlerCatchPO[" + codeHandlerCatchPO.size()
                + "]");

        encodedBand = encodeBandInt("codeHandlerClass",
                cpEntryOrNullListToArray(codeHandlerClass), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeHandlerClass[" + codeHandlerClass.size()
                + "]");

        writeCodeAttributeBands(out);
    }

    private void writeCodeAttributeBands(OutputStream out) throws IOException,
            Pack200Exception {
        byte[] encodedBand = encodeFlags("codeFlags",
                longListToArray(codeFlags), Codec.UNSIGNED5, Codec.UNSIGNED5,
                segmentHeader.have_code_flags_hi());
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from codeFlags[" + codeFlags.size() + "]");

        // *code_attr_count :UNSIGNED5 [COUNT(1<<16,...)]
        // *code_attr_indexes :UNSIGNED5 [SUM(*code_attr_count)]
        encodedBand = encodeBandInt("code_attr_calls", code_attr_calls,
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils
                .log("Wrote " + encodedBand.length
                        + " bytes from code_attr_calls["
                        + code_attr_calls.length + "]");

        encodedBand = encodeBandInt("code_LineNumberTable_N",
                codeLineNumberTableN.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LineNumberTable_N["
                + codeLineNumberTableN.size() + "]");

        encodedBand = encodeBandInt("code_LineNumberTable_bci_P",
                integerListToArray(codeLineNumberTableBciP), Codec.BCI5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LineNumberTable_bci_P["
                + codeLineNumberTableBciP.size() + "]");

        encodedBand = encodeBandInt("code_LineNumberTable_line",
                codeLineNumberTableLine.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LineNumberTable_line["
                + codeLineNumberTableLine.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_N",
                codeLocalVariableTableN.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_N["
                + codeLocalVariableTableN.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_bci_P",
                integerListToArray(codeLocalVariableTableBciP), Codec.BCI5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_bci_P["
                + codeLocalVariableTableBciP.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_span_O",
                integerListToArray(codeLocalVariableTableSpanO), Codec.BRANCH5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_span_O["
                + codeLocalVariableTableSpanO.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_name_RU",
                cpEntryListToArray(codeLocalVariableTableNameRU),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_name_RU["
                + codeLocalVariableTableNameRU.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_type_RS",
                cpEntryListToArray(codeLocalVariableTableTypeRS),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_type_RS["
                + codeLocalVariableTableTypeRS.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTable_slot",
                codeLocalVariableTableSlot.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTable_slot["
                + codeLocalVariableTableSlot.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_N",
                codeLocalVariableTypeTableN.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_N["
                + codeLocalVariableTypeTableN.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_bci_P",
                integerListToArray(codeLocalVariableTypeTableBciP), Codec.BCI5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_bci_P["
                + codeLocalVariableTypeTableBciP.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_span_O",
                integerListToArray(codeLocalVariableTypeTableSpanO), Codec.BRANCH5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_span_O["
                + codeLocalVariableTypeTableSpanO.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_name_RU",
                cpEntryListToArray(codeLocalVariableTypeTableNameRU),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_name_RU["
                + codeLocalVariableTypeTableNameRU.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_type_RS",
                cpEntryListToArray(codeLocalVariableTypeTableTypeRS),
                Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_type_RS["
                + codeLocalVariableTypeTableTypeRS.size() + "]");

        encodedBand = encodeBandInt("code_LocalVariableTypeTable_slot",
                codeLocalVariableTypeTableSlot.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from code_LocalVariableTypeTable_slot["
                + codeLocalVariableTypeTableSlot.size() + "]");

        for (Iterator iterator = codeAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            bands.pack(out);
        }
    }

    public void addMethod(int flags, String name, String desc,
            String signature, String[] exceptions) {
        CPNameAndType nt = cpBands.getCPNameAndType(name, desc);
        tempMethodDesc.add(nt);
        if (signature != null) {
            methodSignature.add(cpBands.getCPSignature(signature));
            flags |= (1 << 19);
        }
        if (exceptions != null) {
            methodExceptionNumber.add(exceptions.length);
            for (int i = 0; i < exceptions.length; i++) {
                methodExceptionClasses.add(cpBands.getCPClass(exceptions[i]));
            }
            flags |= (1 << 18);
        }
        if((flags & Opcodes.ACC_DEPRECATED) != 0) { // ASM uses (1<<17) flag for deprecated
            flags = flags & ~Opcodes.ACC_DEPRECATED;
            flags = flags | (1<<20);
        }
        tempMethodFlags.add(new Long(flags));
        numMethodArgs = countArgs(desc);
        if(!anySyntheticMethods && ((flags & (1 << 12)) != 0) && segment.getCurrentClassReader().hasSyntheticAttributes()) {
            cpBands.addCPUtf8("Synthetic");
            anySyntheticMethods = true;
        }
    }

    public void endOfMethod() {
        if (tempMethodRVPA != null) {
            method_RVPA_bands.addParameterAnnotation(tempMethodRVPA.numParams,
                    tempMethodRVPA.annoN, tempMethodRVPA.pairN,
                    tempMethodRVPA.typeRS, tempMethodRVPA.nameRU,
                    tempMethodRVPA.t, tempMethodRVPA.values,
                    tempMethodRVPA.caseArrayN, tempMethodRVPA.nestTypeRS,
                    tempMethodRVPA.nestNameRU, tempMethodRVPA.nestPairN);
            tempMethodRVPA = null;
        }
        if (tempMethodRIPA != null) {
            method_RIPA_bands.addParameterAnnotation(tempMethodRIPA.numParams,
                    tempMethodRIPA.annoN, tempMethodRIPA.pairN,
                    tempMethodRIPA.typeRS, tempMethodRIPA.nameRU,
                    tempMethodRIPA.t, tempMethodRIPA.values,
                    tempMethodRIPA.caseArrayN, tempMethodRIPA.nestTypeRS,
                    tempMethodRIPA.nestNameRU, tempMethodRIPA.nestPairN);
            tempMethodRIPA = null;
        }
        if(codeFlags.size() > 0) {
            long latestCodeFlag = ((Long)codeFlags.get(codeFlags.size() - 1)).longValue();
            int latestLocalVariableTableN = codeLocalVariableTableN.get(codeLocalVariableTableN.size() - 1);
            if(latestCodeFlag == (1 << 2) && latestLocalVariableTableN == 0) {
                codeLocalVariableTableN.remove(codeLocalVariableTableN.size() - 1);
                codeFlags.remove(codeFlags.size() - 1);
                codeFlags.add(new Integer(0));
            }
        }
    }

    protected static int countArgs(String descriptor) {
        int bra = descriptor.indexOf('(');
        int ket = descriptor.indexOf(')');
        if (bra == -1 || ket == -1 || ket < bra)
            throw new IllegalArgumentException("No arguments");

        boolean inType = false;
        boolean consumingNextType = false;
        int count = 0;
        for (int i = bra + 1; i < ket; i++) {
            char charAt = descriptor.charAt(i);
            if (inType && charAt == ';') {
                inType = false;
                consumingNextType = false;
            } else if (!inType && charAt == 'L') {
                inType = true;
                count++;
            } else if (charAt == '[') {
                consumingNextType = true;
            } else if (inType) {
                // NOP
            } else {
                if (consumingNextType) {
                    count++;
                    consumingNextType = false;
                } else {
                    if (charAt == 'D' || charAt == 'J') {
                        count += 2;
                    } else {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public void endOfClass() { // All the data for the current class has been
                                // read
        int numFields = tempFieldDesc.size();
        class_field_count[index] = numFields;
        field_descr[index] = new CPNameAndType[numFields];
        field_flags[index] = new long[numFields];
        for (int i = 0; i < numFields; i++) {
            field_descr[index][i] = (CPNameAndType) tempFieldDesc.get(i);
            field_flags[index][i] = ((Long) tempFieldFlags.get(i)).longValue();
        }
        int numMethods = tempMethodDesc.size();
        class_method_count[index] = numMethods;
        method_descr[index] = new CPNameAndType[numMethods];
        method_flags[index] = new long[numMethods];
        for (int i = 0; i < numMethods; i++) {
            method_descr[index][i] = (CPNameAndType) tempMethodDesc.get(i);
            method_flags[index][i] = ((Long) tempMethodFlags.get(i))
                    .longValue();
        }
        tempFieldDesc.clear();
        tempFieldFlags.clear();
        tempMethodDesc.clear();
        tempMethodFlags.clear();
        index++;
    }

    public void addSourceFile(String source) {
        String implicitSourceFileName = class_this[index].toString();
        if(implicitSourceFileName.indexOf('$') != -1) {
            implicitSourceFileName = implicitSourceFileName.substring(0, implicitSourceFileName.indexOf('$'));
        }
        implicitSourceFileName = implicitSourceFileName
                .substring(implicitSourceFileName.lastIndexOf('/') + 1)
                + ".java";
        if (source.equals(implicitSourceFileName)) {
            classSourceFile.add(null);
        } else {
            classSourceFile.add(cpBands.getCPUtf8(source));
        }
        class_flags[index] |= (1 << 17);
    }

    public void addEnclosingMethod(String owner, String name, String desc) {
        class_flags[index] |= (1 << 18);
        classEnclosingMethodClass.add(cpBands.getCPClass(owner));
        classEnclosingMethodDesc.add(name == null ? null : cpBands
                .getCPNameAndType(name, desc));
    }

    public void addClassAttribute(NewAttribute attribute) {
        // TODO: backwards calls
        String attributeName = attribute.type;
        for (Iterator iterator = classAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.getAttributeName().equals(attributeName)) {
                bands.addAttribute(attribute);
                int flagIndex = bands.getFlagIndex();
                class_flags[index] |= (1 << flagIndex);
                return;
            }
        }
        throw new RuntimeException("No suitable definition for " + attributeName);
    }

    public void addFieldAttribute(NewAttribute attribute) {
        String attributeName = attribute.type;
        for (Iterator iterator = fieldAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.getAttributeName().equals(attributeName)) {
                bands.addAttribute(attribute);
                int flagIndex = bands.getFlagIndex();
                Long flags = (Long)tempFieldFlags.remove(tempFieldFlags.size() - 1);
                tempFieldFlags.add(new Long(flags.longValue() | (1 << flagIndex)));
                return;
            }
        }
        throw new RuntimeException("No suitable definition for " + attributeName);
    }

    public void addMethodAttribute(NewAttribute attribute) {
        String attributeName = attribute.type;
        for (Iterator iterator = methodAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.getAttributeName().equals(attributeName)) {
                bands.addAttribute(attribute);
                int flagIndex = bands.getFlagIndex();
                Long flags = (Long)tempMethodFlags.remove(tempMethodFlags.size() - 1);
                tempMethodFlags.add(new Long(flags.longValue() | (1 << flagIndex)));
                return;
            }
        }
        throw new RuntimeException("No suitable definition for " + attributeName);
    }

    public void addCodeAttribute(NewAttribute attribute) {
        String attributeName = attribute.type;
        for (Iterator iterator = codeAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands bands = (NewAttributeBands) iterator.next();
            if(bands.getAttributeName().equals(attributeName)) {
                bands.addAttribute(attribute);
                int flagIndex = bands.getFlagIndex();
                Long flags = (Long)codeFlags.remove(codeFlags.size() - 1);
                codeFlags.add(new Long(flags.longValue() | (1 << flagIndex)));
                return;
            }
        }
        throw new RuntimeException("No suitable definition for " + attributeName);
    }

    public void addMaxStack(int maxStack, int maxLocals) {
        Long latestFlag = (Long) tempMethodFlags
                .remove(tempMethodFlags.size() - 1);
        Long newFlag = new Long(latestFlag.intValue() | (1 << 17));
        tempMethodFlags.add(newFlag);
        codeMaxStack.add(maxStack);
        if ((newFlag.longValue() & (1 << 3)) == 0) { // not static
            maxLocals--; // minus 'this' local
        }
        maxLocals -= numMethodArgs;
        codeMaxLocals.add(maxLocals);
    }

    public void addCode() {
        codeHandlerCount.add(0);
        if(!stripDebug) {
            codeFlags.add(new Long((1 << 2)));
            codeLocalVariableTableN.add(0);
        }
    }

    public void addHandler(Label start, Label end, Label handler, String type) {
        int handlers = codeHandlerCount.remove(codeHandlerCount
                .size() - 1);
        codeHandlerCount.add(handlers + 1);
        codeHandlerStartP.add(start);
        codeHandlerEndPO.add(end);
        codeHandlerCatchPO.add(handler);
        codeHandlerClass.add(type == null ? null : cpBands.getCPClass(type));
    }

    public void addLineNumber(int line, Label start) {
        Long latestCodeFlag = (Long) codeFlags.get(codeFlags.size() - 1);
        if ((latestCodeFlag.intValue() & (1 << 1)) == 0) {
            codeFlags.remove(codeFlags.size() - 1);
            codeFlags.add(new Long(latestCodeFlag.intValue() | (1 << 1)));
            codeLineNumberTableN.add(1);
        } else {
            codeLineNumberTableN
                    .increment(codeLineNumberTableN.size() - 1);
        }
        codeLineNumberTableLine.add(line);
        codeLineNumberTableBciP.add(start);
    }

    public void addLocalVariable(String name, String desc, String signature,
            Label start, Label end, int indx) {
        if (signature != null) { // LocalVariableTypeTable attribute
            Long latestCodeFlag = (Long) codeFlags.get(codeFlags.size() - 1);
            if ((latestCodeFlag.intValue() & (1 << 3)) == 0) {
                codeFlags.remove(codeFlags.size() - 1);
                codeFlags.add(new Long(latestCodeFlag.intValue() | (1 << 3)));
                codeLocalVariableTypeTableN.add(1);
            } else {
                codeLocalVariableTypeTableN
                        .increment(codeLocalVariableTypeTableN.size() - 1);
            }
            codeLocalVariableTypeTableBciP.add(start);
            codeLocalVariableTypeTableSpanO.add(end);
            codeLocalVariableTypeTableNameRU.add(cpBands.getCPUtf8(name));
            codeLocalVariableTypeTableTypeRS.add(cpBands
                    .getCPSignature(signature));
            codeLocalVariableTypeTableSlot.add(indx);
        }
        // LocalVariableTable attribute
        codeLocalVariableTableN
                .increment(codeLocalVariableTableN.size() - 1);
        codeLocalVariableTableBciP.add(start);
        codeLocalVariableTableSpanO.add(end);
        codeLocalVariableTableNameRU.add(cpBands.getCPUtf8(name));
        codeLocalVariableTableTypeRS.add(cpBands.getCPSignature(desc));
        codeLocalVariableTableSlot.add(indx);
    }

    public void doBciRenumbering(IntList bciRenumbering, Map labelsToOffsets) {
        renumberBci(codeLineNumberTableBciP, bciRenumbering, labelsToOffsets);
        renumberBci(codeLocalVariableTableBciP, bciRenumbering, labelsToOffsets);
        renumberOffsetBci(codeLocalVariableTableBciP,
                codeLocalVariableTableSpanO, bciRenumbering, labelsToOffsets);
        renumberBci(codeLocalVariableTypeTableBciP, bciRenumbering,
                labelsToOffsets);
        renumberOffsetBci(codeLocalVariableTypeTableBciP,
                codeLocalVariableTypeTableSpanO, bciRenumbering,
                labelsToOffsets);
        renumberBci(codeHandlerStartP, bciRenumbering, labelsToOffsets);
        renumberOffsetBci(codeHandlerStartP, codeHandlerEndPO,
                bciRenumbering, labelsToOffsets);
        renumberDoubleOffsetBci(codeHandlerStartP, codeHandlerEndPO, codeHandlerCatchPO,
                bciRenumbering, labelsToOffsets);

        for (Iterator iterator = classAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands newAttributeBandSet = (NewAttributeBands) iterator.next();
            newAttributeBandSet.renumberBci(bciRenumbering, labelsToOffsets);
        }
        for (Iterator iterator = methodAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands newAttributeBandSet = (NewAttributeBands) iterator.next();
            newAttributeBandSet.renumberBci(bciRenumbering, labelsToOffsets);
        }
        for (Iterator iterator = fieldAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands newAttributeBandSet = (NewAttributeBands) iterator.next();
            newAttributeBandSet.renumberBci(bciRenumbering, labelsToOffsets);
        }
        for (Iterator iterator = codeAttributeBands.iterator(); iterator.hasNext();) {
            NewAttributeBands newAttributeBandSet = (NewAttributeBands) iterator.next();
            newAttributeBandSet.renumberBci(bciRenumbering, labelsToOffsets);
        }
    }

    private void renumberBci(List list, IntList bciRenumbering, Map labelsToOffsets) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Object label = list.get(i);
            if (label instanceof Integer) {
                break;
            } else if (label instanceof Label) {
                list.remove(i);
                Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                list.add(i, new Integer(bciRenumbering.get(bytecodeIndex.intValue())));
            }
        }
    }

    private void renumberOffsetBci(List relative, List list,
            IntList bciRenumbering, Map labelsToOffsets) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Object label = list.get(i);
            if (label instanceof Integer) {
                break;
            } else if (label instanceof Label) {
                list.remove(i);
                Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                Integer renumberedOffset = new Integer(bciRenumbering
                        .get(bytecodeIndex.intValue())
                        - ((Integer) relative.get(i)).intValue());
                list.add(i, renumberedOffset);
            }
        }
    }

    private void renumberDoubleOffsetBci(List relative, List firstOffset, List list,
            IntList bciRenumbering, Map labelsToOffsets) {
        // TODO: There's probably a nicer way of doing this...
        for (int i = list.size() - 1; i >= 0; i--) {
            Object label = list.get(i);
            if (label instanceof Integer) {
                break;
            } else if (label instanceof Label) {
                list.remove(i);
                Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                Integer renumberedOffset = new Integer(bciRenumbering
                        .get(bytecodeIndex.intValue())
                        - ((Integer) relative.get(i)).intValue() - ((Integer) firstOffset.get(i)).intValue());
                list.add(i, renumberedOffset);
            }
        }
    }

    public boolean isAnySyntheticClasses() {
        return anySyntheticClasses;
    }

    public boolean isAnySyntheticFields() {
        return anySyntheticFields;
    }

    public boolean isAnySyntheticMethods() {
        return anySyntheticMethods;
    }

    public void addParameterAnnotation(int parameter, String desc,
            boolean visible, List nameRU, List t, List values, List caseArrayN, List nestTypeRS, List nestNameRU, List nestPairN) {
        if(visible) {
            if(tempMethodRVPA == null) {
                tempMethodRVPA = new TempParamAnnotation(numMethodArgs);
                tempMethodRVPA.addParameterAnnotation(parameter, desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
            }
            Long flag = (Long) tempMethodFlags.remove(tempMethodFlags.size() - 1);
            tempMethodFlags.add(new Long(flag.longValue() | (1<<23)));
        } else {
            if(tempMethodRIPA == null) {
                tempMethodRIPA = new TempParamAnnotation(numMethodArgs);
                tempMethodRIPA.addParameterAnnotation(parameter, desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
            }
            Long flag = (Long) tempMethodFlags.remove(tempMethodFlags.size() - 1);
            tempMethodFlags.add(new Long(flag.longValue() | (1<<24)));
        }
    }

    private static class TempParamAnnotation {

        int numParams;
        int[] annoN;
        IntList pairN = new IntList();
        List typeRS = new ArrayList();
        List nameRU = new ArrayList();
        List t = new ArrayList();
        List values = new ArrayList();
        List caseArrayN = new ArrayList();
        List nestTypeRS = new ArrayList();
        List nestNameRU = new ArrayList();
        List nestPairN = new ArrayList();

        public TempParamAnnotation (int numParams) {
            this.numParams = numParams;
            annoN = new int[numParams];
        }

        public void addParameterAnnotation(int parameter, String desc,
                List nameRU, List t, List values, List caseArrayN,
                List nestTypeRS, List nestNameRU, List nestPairN) {
            annoN[parameter]++;
            typeRS.add(desc);
            pairN.add(nameRU.size());
            this.nameRU.addAll(nameRU);
            this.t.addAll(t);
            this.values.addAll(values);
            this.caseArrayN.addAll(caseArrayN);
            this.nestTypeRS.addAll(nestTypeRS);
            this.nestNameRU.addAll(nestNameRU);
            this.nestPairN.addAll(nestPairN);
        }
    }

    public void addAnnotation(int context, String desc, boolean visible, List nameRU, List t, List values, List caseArrayN, List nestTypeRS, List nestNameRU, List nestPairN) {
        switch (context) {
        case MetadataBandGroup.CONTEXT_CLASS:
            if(visible) {
                class_RVA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                if((class_flags[index] & (1<<21)) != 0) {
                    class_RVA_bands.incrementAnnoN();
                } else {
                    class_RVA_bands.newEntryInAnnoN();
                    class_flags[index] = class_flags[index] | (1<<21);
                }
            } else {
                class_RIA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                if((class_flags[index] & (1<<22)) != 0) {
                    class_RIA_bands.incrementAnnoN();
                } else {
                    class_RIA_bands.newEntryInAnnoN();
                    class_flags[index] = class_flags[index] | (1<<22);
                }
            }
            break;
        case MetadataBandGroup.CONTEXT_FIELD:
            if(visible) {
                field_RVA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                Long flag = (Long) tempFieldFlags.remove(tempFieldFlags.size() - 1);
                if((flag.intValue() & (1<<21)) != 0) {
                    field_RVA_bands.incrementAnnoN();
                } else {
                    field_RVA_bands.newEntryInAnnoN();
                }
                tempFieldFlags.add(new Long(flag.intValue() | (1<<21)));
            } else {
                field_RIA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                Long flag = (Long) tempFieldFlags.remove(tempFieldFlags.size() - 1);
                if((flag.intValue() & (1<<22)) != 0) {
                    field_RIA_bands.incrementAnnoN();
                } else {
                    field_RIA_bands.newEntryInAnnoN();
                }
                tempFieldFlags.add(new Long(flag.intValue() | (1<<22)));
            }
            break;
        case MetadataBandGroup.CONTEXT_METHOD:
            if(visible) {
                method_RVA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                Long flag = (Long) tempMethodFlags.remove(tempMethodFlags.size() - 1);
                if((flag.intValue() & (1<<21)) != 0) {
                    method_RVA_bands.incrementAnnoN();
                } else {
                    method_RVA_bands.newEntryInAnnoN();
                }
                tempMethodFlags.add(new Long(flag.intValue() | (1<<21)));
            } else {
                method_RIA_bands.addAnnotation(desc, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
                Long flag = (Long) tempMethodFlags.remove(tempMethodFlags.size() - 1);
                if((flag.intValue() & (1<<22)) != 0) {
                    method_RIA_bands.incrementAnnoN();
                } else {
                    method_RIA_bands.newEntryInAnnoN();
                }
                tempMethodFlags.add(new Long(flag.intValue() | (1<<22)));
            }
            break;
        }
    }

    public void addAnnotationDefault(List nameRU, List t, List values, List caseArrayN, List nestTypeRS, List nestNameRU, List nestPairN) {
        method_AD_bands.addAnnotation(null, nameRU, t, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
        Long flag = (Long) tempMethodFlags.remove(tempMethodFlags.size() - 1);
        tempMethodFlags.add(new Long(flag.longValue() | (1<<25)));
    }

    /**
     * Remove all entries for the current class
     */
    public void removeCurrentClass() {
        // Note - this doesn't remove any entries added to the constant pool but
        // that shouldn't be a problem
        if ((class_flags[index] & (1 << 17)) != 0) {
            classSourceFile.remove(classSourceFile.size() - 1);
        }
        if ((class_flags[index] & (1 << 18)) != 0) {
            classEnclosingMethodClass
                    .remove(classEnclosingMethodClass.size() - 1);
            classEnclosingMethodDesc
                    .remove(classEnclosingMethodDesc.size() - 1);
        }
        if ((class_flags[index] & (1 << 19)) != 0) {
            classSignature.remove(classSignature.size() - 1);
        }
        if ((class_flags[index] & (1 << 21)) != 0) {
            class_RVA_bands.removeLatest();
        }
        if ((class_flags[index] & (1 << 22)) != 0) {
            class_RIA_bands.removeLatest();
        }
        for (Iterator iterator = tempFieldFlags.iterator(); iterator.hasNext();) {
            Long flagsL = (Long) iterator.next();
            long flags = flagsL.longValue();
            if ((flags & (1 << 19)) != 0) {
                fieldSignature.remove(fieldSignature.size() - 1);
            }
            if ((flags & (1 << 17)) != 0) {
                fieldConstantValueKQ.remove(fieldConstantValueKQ.size() - 1);
            }
            if ((flags & (1 << 21)) != 0) {
                field_RVA_bands.removeLatest();
            }
            if ((flags & (1 << 22)) != 0) {
                field_RIA_bands.removeLatest();
            }
        }
        for (Iterator iterator = tempMethodFlags.iterator(); iterator.hasNext();) {
            Long flagsL = (Long) iterator.next();
            long flags = flagsL.longValue();
            if ((flags & (1 << 19)) != 0) {
                methodSignature.remove(methodSignature.size() - 1);
            }
            if ((flags & (1 << 18)) != 0) {
                int exceptions = methodExceptionNumber
                        .remove(methodExceptionNumber.size() - 1);
                for (int i = 0; i < exceptions; i++) {
                    methodExceptionClasses
                            .remove(methodExceptionClasses.size() - 1);
                }
            }
            if ((flags & (1 << 17)) != 0) { // has code attribute
                codeMaxLocals.remove(codeMaxLocals.size() - 1);
                codeMaxStack.remove(codeMaxStack.size() - 1);
                int handlers = codeHandlerCount
                        .remove(codeHandlerCount.size() - 1);
                for (int i = 0; i < handlers; i++) {
                    int index = codeHandlerStartP.size() - 1;
                    codeHandlerStartP.remove(index);
                    codeHandlerEndPO.remove(index);
                    codeHandlerCatchPO.remove(index);
                    codeHandlerClass.remove(index);
                }
                if (!stripDebug) {
                    long cdeFlags = ((Long) codeFlags
                            .remove(codeFlags.size() - 1)).longValue();
                    int numLocalVariables = codeLocalVariableTableN
                            .remove(codeLocalVariableTableN.size() - 1);
                    for (int i = 0; i < numLocalVariables; i++) {
                        int location = codeLocalVariableTableBciP.size() - 1;
                        codeLocalVariableTableBciP.remove(location);
                        codeLocalVariableTableSpanO.remove(location);
                        codeLocalVariableTableNameRU.remove(location);
                        codeLocalVariableTableTypeRS.remove(location);
                        codeLocalVariableTableSlot.remove(location);
                    }
                    if ((cdeFlags & (1 << 3)) != 0) {
                        int numLocalVariablesInTypeTable = codeLocalVariableTypeTableN
                                .remove(codeLocalVariableTypeTableN.size() - 1);
                        for (int i = 0; i < numLocalVariablesInTypeTable; i++) {
                            int location = codeLocalVariableTypeTableBciP
                                    .size() - 1;
                            codeLocalVariableTypeTableBciP.remove(location);
                            codeLocalVariableTypeTableSpanO.remove(location);
                            codeLocalVariableTypeTableNameRU.remove(location);
                            codeLocalVariableTypeTableTypeRS.remove(location);
                            codeLocalVariableTypeTableSlot.remove(location);
                        }
                    }
                    if ((cdeFlags & (1 << 1)) != 0) {
                        int numLineNumbers = codeLineNumberTableN
                                .remove(codeLineNumberTableN.size() - 1);
                        for (int i = 0; i < numLineNumbers; i++) {
                            int location = codeLineNumberTableBciP.size() - 1;
                            codeLineNumberTableBciP.remove(location);
                            codeLineNumberTableLine.remove(location);
                        }
                    }
                }
            }
            if ((flags & (1 << 21)) != 0) {
                method_RVA_bands.removeLatest();
            }
            if ((flags & (1 << 22)) != 0) {
                method_RIA_bands.removeLatest();
            }
            if ((flags & (1 << 23)) != 0) {
                method_RVPA_bands.removeLatest();
            }
            if ((flags & (1 << 24)) != 0) {
                method_RIPA_bands.removeLatest();
            }
            if ((flags & (1 << 25)) != 0) {
                method_AD_bands.removeLatest();
            }
        }
        class_this[index] = null;
        class_super[index] = null;
        class_interface_count[index] = 0;
        class_interface[index] = null;
        major_versions[index] = 0;
        class_flags[index] = 0;
        tempFieldDesc.clear();
        tempFieldFlags.clear();
        tempMethodDesc.clear();
        tempMethodFlags.clear();
        if(index > 0) {
            index--;
        }
    }

    public int numClassesProcessed() {
        return index;
    }
}
