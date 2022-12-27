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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.Attribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.BCIRenumberedAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.ExceptionTableEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.NewAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Bytecode bands
 */
public class BcBands extends BandSet {

    // The bytecodes for each method in each class as they come (i.e. in their
    // packed format)
    private byte[][][] methodByteCodePacked;

    // The bands
    // TODO: Haven't resolved references yet. Do we want to?
    private int[] bcCaseCount;
    private int[] bcCaseValue;
    private int[] bcByte;
    private int[] bcLocal;
    private int[] bcShort;
    private int[] bcLabel;
    private int[] bcIntRef;
    private int[] bcFloatRef;
    private int[] bcLongRef;
    private int[] bcDoubleRef;
    private int[] bcStringRef;
    private int[] bcClassRef;
    private int[] bcFieldRef;
    private int[] bcMethodRef;
    private int[] bcIMethodRef;
    private int[] bcThisField;
    private int[] bcSuperField;
    private int[] bcThisMethod;
    private int[] bcSuperMethod;
    private int[] bcInitRef;
    private int[] bcEscRef;
    private int[] bcEscRefSize;
    private int[] bcEscSize;
    private int[][] bcEscByte;

    private List<Integer> wideByteCodes;

    /**
     * @param segment TODO
     */
    public BcBands(final Segment segment) {
        super(segment);
    }

    private boolean endsWithLoad(final int codePacked) {
        return (codePacked >= 21 && codePacked <= 25);
    }

    private boolean endsWithStore(final int codePacked) {
        return (codePacked >= 54 && codePacked <= 58);
    }

    public int[] getBcByte() {
        return bcByte;
    }

    public int[] getBcCaseCount() {
        return bcCaseCount;
    }

    public int[] getBcCaseValue() {
        return bcCaseValue;
    }

    public int[] getBcClassRef() {
        return bcClassRef;
    }

    public int[] getBcDoubleRef() {
        return bcDoubleRef;
    }

    public int[] getBcFieldRef() {
        return bcFieldRef;
    }

    public int[] getBcFloatRef() {
        return bcFloatRef;
    }

    public int[] getBcIMethodRef() {
        return bcIMethodRef;
    }

    public int[] getBcInitRef() {
        return bcInitRef;
    }

    public int[] getBcIntRef() {
        return bcIntRef;
    }

    public int[] getBcLabel() {
        return bcLabel;
    }

    public int[] getBcLocal() {
        return bcLocal;
    }

    public int[] getBcLongRef() {
        return bcLongRef;
    }

    public int[] getBcMethodRef() {
        return bcMethodRef;
    }

    public int[] getBcShort() {
        return bcShort;
    }

    public int[] getBcStringRef() {
        return bcStringRef;
    }

    public int[] getBcSuperField() {
        return bcSuperField;
    }

    public int[] getBcSuperMethod() {
        return bcSuperMethod;
    }

    public int[] getBcThisField() {
        return bcThisField;
    }

    public int[] getBcThisMethod() {
        return bcThisMethod;
    }

    public byte[][][] getMethodByteCodePacked() {
        return methodByteCodePacked;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {

        final AttributeLayoutMap attributeDefinitionMap = segment.getAttrDefinitionBands().getAttributeDefinitionMap();
        final int classCount = header.getClassCount();
        final long[][] methodFlags = segment.getClassBands().getMethodFlags();

        int bcCaseCountCount = 0;
        int bcByteCount = 0;
        int bcShortCount = 0;
        int bcLocalCount = 0;
        int bcLabelCount = 0;
        int bcIntRefCount = 0;
        int bcFloatRefCount = 0;
        int bcLongRefCount = 0;
        int bcDoubleRefCount = 0;
        int bcStringRefCount = 0;
        int bcClassRefCount = 0;
        int bcFieldRefCount = 0;
        int bcMethodRefCount = 0;
        int bcIMethodRefCount = 0;
        int bcThisFieldCount = 0;
        int bcSuperFieldCount = 0;
        int bcThisMethodCount = 0;
        int bcSuperMethodCount = 0;
        int bcInitRefCount = 0;
        int bcEscCount = 0;
        int bcEscRefCount = 0;

        final AttributeLayout abstractModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_ABSTRACT,
            AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout nativeModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_NATIVE,
            AttributeLayout.CONTEXT_METHOD);

        methodByteCodePacked = new byte[classCount][][];
        int bcParsed = 0;

        final List<Boolean> switchIsTableSwitch = new ArrayList<>();
        wideByteCodes = new ArrayList<>();
        for (int c = 0; c < classCount; c++) {
            final int numberOfMethods = methodFlags[c].length;
            methodByteCodePacked[c] = new byte[numberOfMethods][];
            for (int m = 0; m < numberOfMethods; m++) {
                final long methodFlag = methodFlags[c][m];
                if (!abstractModifier.matches(methodFlag) && !nativeModifier.matches(methodFlag)) {
                    final ByteArrayOutputStream codeBytes = new ByteArrayOutputStream();
                    byte code;
                    while ((code = (byte) (0xff & in.read())) != -1) {
                        codeBytes.write(code);
                    }
                    methodByteCodePacked[c][m] = codeBytes.toByteArray();
                    bcParsed += methodByteCodePacked[c][m].length;
                    final int[] codes = new int[methodByteCodePacked[c][m].length];
                    for (int i = 0; i < codes.length; i++) {
                        codes[i] = methodByteCodePacked[c][m][i] & 0xff;
                    }
                    for (int i = 0; i < methodByteCodePacked[c][m].length; i++) {
                        final int codePacked = 0xff & methodByteCodePacked[c][m][i];
                        switch (codePacked) {
                        case 16: // bipush
                        case 188: // newarray
                            bcByteCount++;
                            break;
                        case 17: // sipush
                            bcShortCount++;
                            break;
                        case 18: // (a)ldc
                        case 19: // aldc_w
                            bcStringRefCount++;
                            break;
                        case 234: // ildc
                        case 237: // ildc_w
                            bcIntRefCount++;
                            break;
                        case 235: // fldc
                        case 238: // fldc_w
                            bcFloatRefCount++;
                            break;
                        case 197: // multianewarray
                            bcByteCount++;
                            // fallthrough intended
                        case 233: // cldc
                        case 236: // cldc_w
                        case 187: // new
                        case 189: // anewarray
                        case 192: // checkcast
                        case 193: // instanceof
                            bcClassRefCount++;
                            break;
                        case 20: // lldc2_w
                            bcLongRefCount++;
                            break;
                        case 239: // dldc2_w
                            bcDoubleRefCount++;
                            break;
                        case 169: // ret
                            bcLocalCount++;
                            break;
                        case 167: // goto
                        case 168: // jsr
                        case 200: // goto_w
                        case 201: // jsr_w
                            bcLabelCount++;
                            break;
                        case 170: // tableswitch
                            switchIsTableSwitch.add(Boolean.TRUE);
                            bcCaseCountCount++;
                            bcLabelCount++;
                            break;
                        case 171: // lookupswitch
                            switchIsTableSwitch.add(Boolean.FALSE);
                            bcCaseCountCount++;
                            bcLabelCount++;
                            break;
                        case 178: // getstatic
                        case 179: // putstatic
                        case 180: // getfield
                        case 181: // putfield
                            bcFieldRefCount++;
                            break;
                        case 182: // invokevirtual
                        case 183: // invokespecial
                        case 184: // invokestatic
                            bcMethodRefCount++;
                            break;
                        case 185: // invokeinterface
                            bcIMethodRefCount++;
                            break;
                        case 202: // getstatic_this
                        case 203: // putstatic_this
                        case 204: // getfield_this
                        case 205: // putfield_this
                        case 209: // aload_0_getstatic_this
                        case 210: // aload_0_putstatic_this
                        case 211: // aload_0_putfield_this
                        case 212: // aload_0_putfield_this
                            bcThisFieldCount++;
                            break;
                        case 206: // invokevirtual_this
                        case 207: // invokespecial_this
                        case 208: // invokestatic_this
                        case 213: // aload_0_invokevirtual_this
                        case 214: // aload_0_invokespecial_this
                        case 215: // aload_0_invokestatic_this
                            bcThisMethodCount++;
                            break;
                        case 216: // getstatic_super
                        case 217: // putstatic_super
                        case 218: // getfield_super
                        case 219: // putfield_super
                        case 223: // aload_0_getstatic_super
                        case 224: // aload_0_putstatic_super
                        case 225: // aload_0_getfield_super
                        case 226: // aload_0_putfield_super
                            bcSuperFieldCount++;
                            break;
                        case 220: // invokevirtual_super
                        case 221: // invokespecial_super
                        case 222: // invokestatic_super
                        case 227: // aload_0_invokevirtual_super
                        case 228: // aload_0_invokespecial_super
                        case 229: // aload_0_invokestatic_super
                            bcSuperMethodCount++;
                            break;
                        case 132: // iinc
                            bcLocalCount++;
                            bcByteCount++;
                            break;
                        case 196: // wide
                            final int nextInstruction = 0xff & methodByteCodePacked[c][m][i + 1];
                            wideByteCodes.add(Integer.valueOf(nextInstruction));
                            if (nextInstruction == 132) { // iinc
                                bcLocalCount++;
                                bcShortCount++;
                            } else if (endsWithLoad(nextInstruction) || endsWithStore(nextInstruction)
                                || nextInstruction == 169) {
                                bcLocalCount++;
                            } else {
                                segment.log(Segment.LOG_LEVEL_VERBOSE,
                                    "Found unhandled " + ByteCode.getByteCode(nextInstruction));
                            }
                            i++;
                            break;
                        case 230: // invokespecial_this_init
                        case 231: // invokespecial_super_init
                        case 232: // invokespecial_new_init
                            bcInitRefCount++;
                            break;
                        case 253: // ref_escape
                            bcEscRefCount++;
                            break;
                        case 254: // byte_escape
                            bcEscCount++;
                            break;
                        default:
                            if (endsWithLoad(codePacked) || endsWithStore(codePacked)) {
                                bcLocalCount++;
                            } else if (startsWithIf(codePacked)) {
                                bcLabelCount++;
                            }
                        }
                    }
                }
            }
        }
        // other bytecode bands
        bcCaseCount = decodeBandInt("bc_case_count", in, Codec.UNSIGNED5, bcCaseCountCount);
        int bcCaseValueCount = 0;
        for (int i = 0; i < bcCaseCount.length; i++) {
            final boolean isTableSwitch = switchIsTableSwitch.get(i).booleanValue();
            if (isTableSwitch) {
                bcCaseValueCount += 1;
            } else {
                bcCaseValueCount += bcCaseCount[i];
            }
        }
        bcCaseValue = decodeBandInt("bc_case_value", in, Codec.DELTA5, bcCaseValueCount);
        // Every case value needs a label. We weren't able to count these
        // above, because we didn't know how many cases there were.
        // Have to correct it now.
        for (int index = 0; index < bcCaseCountCount; index++) {
            bcLabelCount += bcCaseCount[index];
        }
        bcByte = decodeBandInt("bc_byte", in, Codec.BYTE1, bcByteCount);
        bcShort = decodeBandInt("bc_short", in, Codec.DELTA5, bcShortCount);
        bcLocal = decodeBandInt("bc_local", in, Codec.UNSIGNED5, bcLocalCount);
        bcLabel = decodeBandInt("bc_label", in, Codec.BRANCH5, bcLabelCount);
        bcIntRef = decodeBandInt("bc_intref", in, Codec.DELTA5, bcIntRefCount);
        bcFloatRef = decodeBandInt("bc_floatref", in, Codec.DELTA5, bcFloatRefCount);
        bcLongRef = decodeBandInt("bc_longref", in, Codec.DELTA5, bcLongRefCount);
        bcDoubleRef = decodeBandInt("bc_doubleref", in, Codec.DELTA5, bcDoubleRefCount);
        bcStringRef = decodeBandInt("bc_stringref", in, Codec.DELTA5, bcStringRefCount);
        bcClassRef = decodeBandInt("bc_classref", in, Codec.UNSIGNED5, bcClassRefCount);
        bcFieldRef = decodeBandInt("bc_fieldref", in, Codec.DELTA5, bcFieldRefCount);
        bcMethodRef = decodeBandInt("bc_methodref", in, Codec.UNSIGNED5, bcMethodRefCount);
        bcIMethodRef = decodeBandInt("bc_imethodref", in, Codec.DELTA5, bcIMethodRefCount);
        bcThisField = decodeBandInt("bc_thisfield", in, Codec.UNSIGNED5, bcThisFieldCount);
        bcSuperField = decodeBandInt("bc_superfield", in, Codec.UNSIGNED5, bcSuperFieldCount);
        bcThisMethod = decodeBandInt("bc_thismethod", in, Codec.UNSIGNED5, bcThisMethodCount);
        bcSuperMethod = decodeBandInt("bc_supermethod", in, Codec.UNSIGNED5, bcSuperMethodCount);
        bcInitRef = decodeBandInt("bc_initref", in, Codec.UNSIGNED5, bcInitRefCount);
        bcEscRef = decodeBandInt("bc_escref", in, Codec.UNSIGNED5, bcEscRefCount);
        bcEscRefSize = decodeBandInt("bc_escrefsize", in, Codec.UNSIGNED5, bcEscRefCount);
        bcEscSize = decodeBandInt("bc_escsize", in, Codec.UNSIGNED5, bcEscCount);
        bcEscByte = decodeBandInt("bc_escbyte", in, Codec.BYTE1, bcEscSize);
    }

    private boolean startsWithIf(final int codePacked) {
        return (codePacked >= 153 && codePacked <= 166) || (codePacked == 198) || (codePacked == 199);
    }

    @Override
    public void unpack() throws Pack200Exception {
        final int classCount = header.getClassCount();
        final long[][] methodFlags = segment.getClassBands().getMethodFlags();
        final int[] codeMaxNALocals = segment.getClassBands().getCodeMaxNALocals();
        final int[] codeMaxStack = segment.getClassBands().getCodeMaxStack();
        final ArrayList<Attribute>[][] methodAttributes = segment.getClassBands().getMethodAttributes();
        final String[][] methodDescr = segment.getClassBands().getMethodDescr();

        final AttributeLayoutMap attributeDefinitionMap = segment.getAttrDefinitionBands().getAttributeDefinitionMap();

        final AttributeLayout abstractModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_ABSTRACT,
            AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout nativeModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_NATIVE,
            AttributeLayout.CONTEXT_METHOD);
        final AttributeLayout staticModifier = attributeDefinitionMap.getAttributeLayout(AttributeLayout.ACC_STATIC,
            AttributeLayout.CONTEXT_METHOD);

        final int[] wideByteCodeArray = new int[wideByteCodes.size()];
        for (int index = 0; index < wideByteCodeArray.length; index++) {
            wideByteCodeArray[index] = wideByteCodes.get(index).intValue();
        }
        final OperandManager operandManager = new OperandManager(bcCaseCount, bcCaseValue, bcByte, bcShort, bcLocal,
            bcLabel, bcIntRef, bcFloatRef, bcLongRef, bcDoubleRef, bcStringRef, bcClassRef, bcFieldRef, bcMethodRef,
            bcIMethodRef, bcThisField, bcSuperField, bcThisMethod, bcSuperMethod, bcInitRef, wideByteCodeArray);
        operandManager.setSegment(segment);

        int i = 0;
        final ArrayList<List<Attribute>> orderedCodeAttributes = segment.getClassBands().getOrderedCodeAttributes();
        int codeAttributeIndex = 0;

        // Exception table fields
        final int[] handlerCount = segment.getClassBands().getCodeHandlerCount();
        final int[][] handlerStartPCs = segment.getClassBands().getCodeHandlerStartP();
        final int[][] handlerEndPCs = segment.getClassBands().getCodeHandlerEndPO();
        final int[][] handlerCatchPCs = segment.getClassBands().getCodeHandlerCatchPO();
        final int[][] handlerClassTypes = segment.getClassBands().getCodeHandlerClassRCN();

        final boolean allCodeHasFlags = segment.getSegmentHeader().getOptions().hasAllCodeFlags();
        final boolean[] codeHasFlags = segment.getClassBands().getCodeHasAttributes();

        for (int c = 0; c < classCount; c++) {
            final int numberOfMethods = methodFlags[c].length;
            for (int m = 0; m < numberOfMethods; m++) {
                final long methodFlag = methodFlags[c][m];
                if (!abstractModifier.matches(methodFlag) && !nativeModifier.matches(methodFlag)) {
                    final int maxStack = codeMaxStack[i];
                    int maxLocal = codeMaxNALocals[i];
                    if (!staticModifier.matches(methodFlag)) {
                        maxLocal++; // one for 'this' parameter
                    }
                    // I believe this has to take wide arguments into account
                    maxLocal += SegmentUtils.countInvokeInterfaceArgs(methodDescr[c][m]);
                    final String[] cpClass = segment.getCpBands().getCpClass();
                    operandManager.setCurrentClass(cpClass[segment.getClassBands().getClassThisInts()[c]]);
                    operandManager.setSuperClass(cpClass[segment.getClassBands().getClassSuperInts()[c]]);
                    final List<ExceptionTableEntry> exceptionTable = new ArrayList<>();
                    if (handlerCount != null) {
                        for (int j = 0; j < handlerCount[i]; j++) {
                            final int handlerClass = handlerClassTypes[i][j] - 1;
                            CPClass cpHandlerClass = null;
                            if (handlerClass != -1) {
                                // The handlerClass will be null if the
                                // catch is a finally (that is, the
                                // exception table catch_type should be 0
                                cpHandlerClass = segment.getCpBands().cpClassValue(handlerClass);
                            }
                            final ExceptionTableEntry entry = new ExceptionTableEntry(handlerStartPCs[i][j],
                                handlerEndPCs[i][j], handlerCatchPCs[i][j], cpHandlerClass);
                            exceptionTable.add(entry);
                        }
                    }
                    final CodeAttribute codeAttr = new CodeAttribute(maxStack, maxLocal, methodByteCodePacked[c][m],
                        segment, operandManager, exceptionTable);
                    final List<Attribute> methodAttributesList = methodAttributes[c][m];
                    // Make sure we add the code attribute in the right place
                    int indexForCodeAttr = 0;
                    for (final Attribute attribute : methodAttributesList) {
                        if (!(attribute instanceof NewAttribute)
                            || (((NewAttribute) attribute).getLayoutIndex() >= 15)) {
                            break;
                        }
                        indexForCodeAttr++;
                    }
                    methodAttributesList.add(indexForCodeAttr, codeAttr);
                    codeAttr.renumber(codeAttr.byteCodeOffsets);
                    List<Attribute> currentAttributes;
                    if (allCodeHasFlags) {
                        currentAttributes = orderedCodeAttributes.get(i);
                    } else if (codeHasFlags[i]) {
                        currentAttributes = orderedCodeAttributes.get(codeAttributeIndex);
                        codeAttributeIndex++;
                    } else {
                        currentAttributes = Collections.EMPTY_LIST;
                    }
                    for (final Attribute currentAttribute : currentAttributes) {
                        codeAttr.addAttribute(currentAttribute);
                        // Fix up the line numbers if needed
                        if (currentAttribute.hasBCIRenumbering()) {
                            ((BCIRenumberedAttribute) currentAttribute).renumber(codeAttr.byteCodeOffsets);
                        }
                    }
                    i++;
                }
            }
        }
    }
}
