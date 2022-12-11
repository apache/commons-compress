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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;

/**
 * Bytecode bands (corresponds to the {@code bc_bands} set of bands in the pack200 specification)
 */
public class BcBands extends BandSet {

    private static final int MULTIANEWARRAY = 197;
    private static final int ALOAD_0 = 42;

    private static final int WIDE = 196;

    private static final int INVOKEINTERFACE = 185;
    private static final int TABLESWITCH = 170;
    private static final int IINC = 132;
    private static final int LOOKUPSWITCH = 171;
    private static final int endMarker = 255;
    private final CpBands cpBands;

    private final Segment segment;
    private final IntList bcCodes = new IntList();
    private final IntList bcCaseCount = new IntList();
    private final IntList bcCaseValue = new IntList();
    private final IntList bcByte = new IntList();
    private final IntList bcShort = new IntList();
    private final IntList bcLocal = new IntList();
    // Integers and/or Labels?
    private final List bcLabel = new ArrayList();
    private final List<CPInt> bcIntref = new ArrayList<>();
    private final List<CPFloat> bcFloatRef = new ArrayList<>();
    private final List<CPLong> bcLongRef = new ArrayList<>();
    private final List<CPDouble> bcDoubleRef = new ArrayList<>();
    private final List<CPString> bcStringRef = new ArrayList<>();
    private final List<CPClass> bcClassRef = new ArrayList<>();
    private final List<CPMethodOrField> bcFieldRef = new ArrayList<>();

    private final List<CPMethodOrField> bcMethodRef = new ArrayList<>();
    private final List<CPMethodOrField> bcIMethodRef = new ArrayList<>();
    private List bcThisField = new ArrayList<>();

    private final List bcSuperField = new ArrayList<>();
    private List bcThisMethod = new ArrayList<>();
    private List bcSuperMethod = new ArrayList<>();
    private List bcInitRef = new ArrayList<>();
    private String currentClass;
    private String superClass;
    private String currentNewClass;
    private final IntList bciRenumbering = new IntList();

    private final Map<Label, Integer> labelsToOffsets = new HashMap<>();
    private int byteCodeOffset;
    private int renumberedOffset;
    private final IntList bcLabelRelativeOffsets = new IntList();
    public BcBands(final CpBands cpBands, final Segment segment, final int effort) {
        super(effort, segment.getSegmentHeader());
        this.cpBands = cpBands;
        this.segment = segment;
    }

    /**
     * All input classes for the segment have now been read in, so this method is called so that this class can
     * calculate/complete anything it could not do while classes were being read.
     */
    public void finaliseBands() {
        bcThisField = getIndexInClass(bcThisField);
        bcThisMethod = getIndexInClass(bcThisMethod);
        bcSuperMethod = getIndexInClass(bcSuperMethod);
        bcInitRef = getIndexInClassForConstructor(bcInitRef);
    }

    private List<Integer> getIndexInClass(final List<CPMethodOrField> cPMethodOrFieldList) {
        return cPMethodOrFieldList.stream().collect(Collectors.mapping(CPMethodOrField::getIndexInClass, Collectors.toList()));
    }

    private List<Integer> getIndexInClassForConstructor(final List<CPMethodOrField> cPMethodList) {
        return cPMethodList.stream().collect(Collectors.mapping(CPMethodOrField::getIndexInClassForConstructor, Collectors.toList()));
    }

    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing byte code bands...");
        byte[] encodedBand = encodeBandInt("bcCodes", bcCodes.toArray(), Codec.BYTE1);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcCodes[" + bcCodes.size() + "]");

        encodedBand = encodeBandInt("bcCaseCount", bcCaseCount.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcCaseCount[" + bcCaseCount.size() + "]");

        encodedBand = encodeBandInt("bcCaseValue", bcCaseValue.toArray(), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcCaseValue[" + bcCaseValue.size() + "]");

        encodedBand = encodeBandInt("bcByte", bcByte.toArray(), Codec.BYTE1);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcByte[" + bcByte.size() + "]");

        encodedBand = encodeBandInt("bcShort", bcShort.toArray(), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcShort[" + bcShort.size() + "]");

        encodedBand = encodeBandInt("bcLocal", bcLocal.toArray(), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcLocal[" + bcLocal.size() + "]");

        encodedBand = encodeBandInt("bcLabel", integerListToArray(bcLabel), Codec.BRANCH5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcLabel[" + bcLabel.size() + "]");

        encodedBand = encodeBandInt("bcIntref", cpEntryListToArray(bcIntref), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcIntref[" + bcIntref.size() + "]");

        encodedBand = encodeBandInt("bcFloatRef", cpEntryListToArray(bcFloatRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcFloatRef[" + bcFloatRef.size() + "]");

        encodedBand = encodeBandInt("bcLongRef", cpEntryListToArray(bcLongRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcLongRef[" + bcLongRef.size() + "]");

        encodedBand = encodeBandInt("bcDoubleRef", cpEntryListToArray(bcDoubleRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcDoubleRef[" + bcDoubleRef.size() + "]");

        encodedBand = encodeBandInt("bcStringRef", cpEntryListToArray(bcStringRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcStringRef[" + bcStringRef.size() + "]");

        encodedBand = encodeBandInt("bcClassRef", cpEntryOrNullListToArray(bcClassRef), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcClassRef[" + bcClassRef.size() + "]");

        encodedBand = encodeBandInt("bcFieldRef", cpEntryListToArray(bcFieldRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcFieldRef[" + bcFieldRef.size() + "]");

        encodedBand = encodeBandInt("bcMethodRef", cpEntryListToArray(bcMethodRef), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcMethodRef[" + bcMethodRef.size() + "]");

        encodedBand = encodeBandInt("bcIMethodRef", cpEntryListToArray(bcIMethodRef), Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcIMethodRef[" + bcIMethodRef.size() + "]");

        encodedBand = encodeBandInt("bcThisField", integerListToArray(bcThisField), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcThisField[" + bcThisField.size() + "]");

        encodedBand = encodeBandInt("bcSuperField", integerListToArray(bcSuperField), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcSuperField[" + bcSuperField.size() + "]");

        encodedBand = encodeBandInt("bcThisMethod", integerListToArray(bcThisMethod), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcThisMethod[" + bcThisMethod.size() + "]");

        encodedBand = encodeBandInt("bcSuperMethod", integerListToArray(bcSuperMethod), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcSuperMethod[" + bcSuperMethod.size() + "]");

        encodedBand = encodeBandInt("bcInitRef", integerListToArray(bcInitRef), Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from bcInitRef[" + bcInitRef.size() + "]");

        // out.write(encodeBandInt(cpEntryintegerListToArray(bcEscRef),
        // Codec.UNSIGNED5));
        // out.write(encodeBandInt(integerListToArray(bcEscRefSize),
        // Codec.UNSIGNED5));
        // out.write(encodeBandInt(integerListToArray(bcEscSize),
        // Codec.UNSIGNED5));
        // out.write(encodeBandInt(integerListToArray(bcEscByte), Codec.BYTE1));
    }

    public void setCurrentClass(final String name, final String superName) {
        currentClass = name;
        superClass = superName;
    }

    private void updateRenumbering() {
        if (bciRenumbering.isEmpty()) {
            bciRenumbering.add(0);
        }
        renumberedOffset++;
        for (int i = bciRenumbering.size(); i < byteCodeOffset; i++) {
            bciRenumbering.add(-1);
        }
        bciRenumbering.add(renumberedOffset);
    }

    public void visitEnd() {
        for (int i = 0; i < bciRenumbering.size(); i++) {
            if (bciRenumbering.get(i) == -1) {
                bciRenumbering.remove(i);
                bciRenumbering.add(i, ++renumberedOffset);
            }
        }
        if (renumberedOffset != 0) {
            if (renumberedOffset + 1 != bciRenumbering.size()) {
                throw new IllegalStateException("Mistake made with renumbering");
            }
            for (int i = bcLabel.size() - 1; i >= 0; i--) {
                final Object label = bcLabel.get(i);
                if (label instanceof Integer) {
                    break;
                }
                if (label instanceof Label) {
                    bcLabel.remove(i);
                    final Integer offset = labelsToOffsets.get(label);
                    final int relativeOffset = bcLabelRelativeOffsets.get(i);
                    bcLabel.add(i,
                        Integer.valueOf(bciRenumbering.get(offset.intValue()) - bciRenumbering.get(relativeOffset)));
                }
            }
            bcCodes.add(endMarker);
            segment.getClassBands().doBciRenumbering(bciRenumbering, labelsToOffsets);
            bciRenumbering.clear();
            labelsToOffsets.clear();
            byteCodeOffset = 0;
            renumberedOffset = 0;
        }
    }

    public void visitFieldInsn(int opcode, final String owner, final String name, final String desc) {
        byteCodeOffset += 3;
        updateRenumbering();
        boolean aload_0 = false;
        if (bcCodes.size() > 0 && (bcCodes.get(bcCodes.size() - 1)) == ALOAD_0) {
            bcCodes.remove(bcCodes.size() - 1);
            aload_0 = true;
        }
        final CPMethodOrField cpField = cpBands.getCPField(owner, name, desc);
        if (aload_0) {
            opcode += 7;
        }
        if (owner.equals(currentClass)) {
            opcode += 24; // change to getstatic_this, putstatic_this etc.
            bcThisField.add(cpField);
//        } else if (owner.equals(superClass)) {
//            opcode += 38; // change to getstatic_super etc.
//            bcSuperField.add(cpField);
        } else {
            if (aload_0) {
                opcode -= 7;
                bcCodes.add(ALOAD_0); // add aload_0 back in because
                // there's no special rewrite in
                // this case.
            }
            bcFieldRef.add(cpField);
        }
        aload_0 = false;
        bcCodes.add(opcode);
    }

    public void visitIincInsn(final int var, final int increment) {
        if (var > 255 || increment > 255) {
            byteCodeOffset += 6;
            bcCodes.add(WIDE);
            bcCodes.add(IINC);
            bcLocal.add(var);
            bcShort.add(increment);
        } else {
            byteCodeOffset += 3;
            bcCodes.add(IINC);
            bcLocal.add(var);
            bcByte.add(increment & 0xFF);
        }
        updateRenumbering();
    }

    public void visitInsn(final int opcode) {
        if (opcode >= 202) {
            throw new IllegalArgumentException("Non-standard bytecode instructions not supported");
        }
        bcCodes.add(opcode);
        byteCodeOffset++;
        updateRenumbering();
    }

    public void visitIntInsn(final int opcode, final int operand) {
        switch (opcode) {
        case 17: // sipush
            bcCodes.add(opcode);
            bcShort.add(operand);
            byteCodeOffset += 3;
            break;
        case 16: // bipush
        case 188: // newarray
            bcCodes.add(opcode);
            bcByte.add(operand & 0xFF);
            byteCodeOffset += 2;
        }
        updateRenumbering();
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        bcCodes.add(opcode);
        bcLabel.add(label);
        bcLabelRelativeOffsets.add(byteCodeOffset);
        byteCodeOffset += 3;
        updateRenumbering();
    }

    public void visitLabel(final Label label) {
        labelsToOffsets.put(label, Integer.valueOf(byteCodeOffset));
    }

    public void visitLdcInsn(final Object cst) {
        final CPConstant<?> constant = cpBands.getConstant(cst);
        if (segment.lastConstantHadWideIndex() || constant instanceof CPLong || constant instanceof CPDouble) {
            byteCodeOffset += 3;
            if (constant instanceof CPInt) {
                bcCodes.add(237); // ildc_w
                bcIntref.add((CPInt) constant);
            } else if (constant instanceof CPFloat) {
                bcCodes.add(238); // fldc
                bcFloatRef.add((CPFloat) constant);
            } else if (constant instanceof CPLong) {
                bcCodes.add(20); // lldc2_w
                bcLongRef.add((CPLong) constant);
            } else if (constant instanceof CPDouble) {
                bcCodes.add(239); // dldc2_w
                bcDoubleRef.add((CPDouble) constant);
            } else if (constant instanceof CPString) {
                bcCodes.add(19); // aldc
                bcStringRef.add((CPString) constant);
            } else if (constant instanceof CPClass) {
                bcCodes.add(236); // cldc
                bcClassRef.add((CPClass) constant);
            } else {
                throw new IllegalArgumentException("Constant should not be null");
            }
        } else {
            byteCodeOffset += 2;
            if (constant instanceof CPInt) {
                bcCodes.add(234); // ildc
                bcIntref.add((CPInt) constant);
            } else if (constant instanceof CPFloat) {
                bcCodes.add(235); // fldc
                bcFloatRef.add((CPFloat) constant);
            } else if (constant instanceof CPString) {
                bcCodes.add(18); // aldc
                bcStringRef.add((CPString) constant);
            } else if (constant instanceof CPClass) {
                bcCodes.add(233); // cldc
                bcClassRef.add((CPClass) constant);
            }
        }
        updateRenumbering();
    }

    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        bcCodes.add(LOOKUPSWITCH);
        bcLabel.add(dflt);
        bcLabelRelativeOffsets.add(byteCodeOffset);
        bcCaseCount.add(keys.length);
        for (int i = 0; i < labels.length; i++) {
            bcCaseValue.add(keys[i]);
            bcLabel.add(labels[i]);
            bcLabelRelativeOffsets.add(byteCodeOffset);
        }
        final int padding = (byteCodeOffset + 1) % 4 == 0 ? 0 : 4 - ((byteCodeOffset + 1) % 4);
        byteCodeOffset += 1 + padding + 8 + 8 * keys.length;
        updateRenumbering();
    }

    public void visitMethodInsn(int opcode, final String owner, final String name, final String desc) {
        byteCodeOffset += 3;
        switch (opcode) {
        case 182: // invokevirtual
        case 183: // invokespecial
        case 184: // invokestatic
            boolean aload_0 = false;
            if (bcCodes.size() > 0 && (bcCodes.get(bcCodes.size() - 1)) == (ALOAD_0)) {
                bcCodes.remove(bcCodes.size() - 1);
                aload_0 = true;
                opcode += 7;
            }
            if (owner.equals(currentClass)) {
                opcode += 24; // change to invokevirtual_this,
                // invokespecial_this etc.

                if (name.equals("<init>") && opcode == 207) {
                    opcode = 230; // invokespecial_this_init
                    bcInitRef.add(cpBands.getCPMethod(owner, name, desc));
                } else {
                    bcThisMethod.add(cpBands.getCPMethod(owner, name, desc));
                }
            } else if (owner.equals(superClass)) { // TODO
                opcode += 38; // change to invokevirtual_super,
                // invokespecial_super etc.
                if (name.equals("<init>") && opcode == 221) {
                    opcode = 231; // invokespecial_super_init
                    bcInitRef.add(cpBands.getCPMethod(owner, name, desc));
                } else {
                    bcSuperMethod.add(cpBands.getCPMethod(owner, name, desc));
                }
            } else {
                if (aload_0) {
                    opcode -= 7;
                    bcCodes.add(ALOAD_0); // add aload_0 back in
                    // because there's no
                    // special rewrite in this
                    // case.
                }
                if (name.equals("<init>") && opcode == 183 && owner.equals(currentNewClass)) {
                    opcode = 232; // invokespecial_new_init
                    bcInitRef.add(cpBands.getCPMethod(owner, name, desc));
                } else {
                    bcMethodRef.add(cpBands.getCPMethod(owner, name, desc));
                }
            }
            bcCodes.add(opcode);
            break;
        case 185: // invokeinterface
            byteCodeOffset += 2;
            final CPMethodOrField cpIMethod = cpBands.getCPIMethod(owner, name, desc);
            bcIMethodRef.add(cpIMethod);
            bcCodes.add(INVOKEINTERFACE);
            break;
        }
        updateRenumbering();
    }

    public void visitMultiANewArrayInsn(final String desc, final int dimensions) {
        byteCodeOffset += 4;
        updateRenumbering();
        bcCodes.add(MULTIANEWARRAY);
        bcClassRef.add(cpBands.getCPClass(desc));
        bcByte.add(dimensions & 0xFF);
    }

    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        bcCodes.add(TABLESWITCH);
        bcLabel.add(dflt);
        bcLabelRelativeOffsets.add(byteCodeOffset);
        bcCaseValue.add(min);
        final int count = labels.length;
        bcCaseCount.add(count);
        for (int i = 0; i < count; i++) {
            bcLabel.add(labels[i]);
            bcLabelRelativeOffsets.add(byteCodeOffset);
        }
        final int padding = byteCodeOffset % 4 == 0 ? 0 : 4 - (byteCodeOffset % 4);
        byteCodeOffset += (padding + 12 + 4 * labels.length);
        updateRenumbering();
    }

    public void visitTypeInsn(final int opcode, final String type) {
        // NEW, ANEWARRAY, CHECKCAST or INSTANCEOF
        byteCodeOffset += 3;
        updateRenumbering();
        bcCodes.add(opcode);
        bcClassRef.add(cpBands.getCPClass(type));
        if (opcode == 187) { // NEW
            currentNewClass = type;
        }
    }

    public void visitVarInsn(final int opcode, final int var) {
        // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET
        if (var > 255) {
            byteCodeOffset += 4;
            bcCodes.add(WIDE);
            bcCodes.add(opcode);
            bcLocal.add(var);
        } else if (var > 3 || opcode == 169 /* RET */) {
            byteCodeOffset += 2;
            bcCodes.add(opcode);
            bcLocal.add(var);
        } else {
            byteCodeOffset += 1;
            switch (opcode) {
            case 21: // ILOAD
            case 54: // ISTORE
                bcCodes.add(opcode + 5 + var);
                break;
            case 22: // LLOAD
            case 55: // LSTORE
                bcCodes.add(opcode + 8 + var);
                break;
            case 23: // FLOAD
            case 56: // FSTORE
                bcCodes.add(opcode + 11 + var);
                break;
            case 24: // DLOAD
            case 57: // DSTORE
                bcCodes.add(opcode + 14 + var);
                break;
            case 25: // A_LOAD
            case 58: // A_STORE
                bcCodes.add(opcode + 17 + var);
                break;
            }
        }
        updateRenumbering();
    }

}
