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

package org.apache.commons.compress.harmony.unpack200.bytecode;

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;

/**
 * This class keeps track of operands used. It provides API to let other classes get next elements, and also knows about
 * which classes have been used recently in super, this and new references.
 */
public class OperandManager {

    int[] bcCaseCount;
    int[] bcCaseValue;
    int[] bcByte;
    int[] bcShort;
    int[] bcLocal;
    int[] bcLabel;
    int[] bcIntRef;
    int[] bcFloatRef;
    int[] bcLongRef;
    int[] bcDoubleRef;
    int[] bcStringRef;
    int[] bcClassRef;
    int[] bcFieldRef;
    int[] bcMethodRef;
    int[] bcIMethodRef;
    int[] bcThisField;
    int[] bcSuperField;
    int[] bcThisMethod;
    int[] bcSuperMethod;
    int[] bcInitRef;
    int[] wideByteCodes;

    int bcCaseCountIndex;
    int bcCaseValueIndex;
    int bcByteIndex;
    int bcShortIndex;
    int bcLocalIndex;
    int bcLabelIndex;
    int bcIntRefIndex;
    int bcFloatRefIndex;
    int bcLongRefIndex;
    int bcDoubleRefIndex;
    int bcStringRefIndex;
    int bcClassRefIndex;
    int bcFieldRefIndex;
    int bcMethodRefIndex;
    int bcIMethodRefIndex;
    int bcThisFieldIndex;
    int bcSuperFieldIndex;
    int bcThisMethodIndex;
    int bcSuperMethodIndex;
    int bcInitRefIndex;
    int wideByteCodeIndex;

    Segment segment;

    String currentClass;
    String superClass;
    String newClass;

    public OperandManager(final int[] bcCaseCount, final int[] bcCaseValue, final int[] bcByte, final int[] bcShort,
        final int[] bcLocal, final int[] bcLabel, final int[] bcIntRef, final int[] bcFloatRef, final int[] bcLongRef,
        final int[] bcDoubleRef, final int[] bcStringRef, final int[] bcClassRef, final int[] bcFieldRef,
        final int[] bcMethodRef, final int[] bcIMethodRef, final int[] bcThisField, final int[] bcSuperField,
        final int[] bcThisMethod, final int[] bcSuperMethod, final int[] bcInitRef, final int[] wideByteCodes) {
        this.bcCaseCount = bcCaseCount;
        this.bcCaseValue = bcCaseValue;
        this.bcByte = bcByte;
        this.bcShort = bcShort;
        this.bcLocal = bcLocal;
        this.bcLabel = bcLabel;
        this.bcIntRef = bcIntRef;
        this.bcFloatRef = bcFloatRef;
        this.bcLongRef = bcLongRef;
        this.bcDoubleRef = bcDoubleRef;
        this.bcStringRef = bcStringRef;
        this.bcClassRef = bcClassRef;
        this.bcFieldRef = bcFieldRef;
        this.bcMethodRef = bcMethodRef;
        this.bcIMethodRef = bcIMethodRef;

        this.bcThisField = bcThisField;
        this.bcSuperField = bcSuperField;
        this.bcThisMethod = bcThisMethod;
        this.bcSuperMethod = bcSuperMethod;
        this.bcInitRef = bcInitRef;
        this.wideByteCodes = wideByteCodes;
    }

    public String getCurrentClass() {
        if (null == currentClass) {
            throw new Error("Current class not set yet");
        }
        return currentClass;
    }

    public String getNewClass() {
        if (null == newClass) {
            throw new Error("New class not set yet");
        }
        return newClass;
    }

    public String getSuperClass() {
        if (null == superClass) {
            throw new Error("SuperClass not set yet");
        }
        return superClass;
    }

    public SegmentConstantPool globalConstantPool() {
        return segment.getConstantPool();
    }

    public int nextByte() {
        return bcByte[bcByteIndex++];
    }

    public int nextCaseCount() {
        return bcCaseCount[bcCaseCountIndex++];
    }

    public int nextCaseValues() {
        return bcCaseValue[bcCaseValueIndex++];
    }

    public int nextClassRef() {
        return bcClassRef[bcClassRefIndex++];
    }

    public int nextDoubleRef() {
        return bcDoubleRef[bcDoubleRefIndex++];
    }

    public int nextFieldRef() {
        return bcFieldRef[bcFieldRefIndex++];
    }

    public int nextFloatRef() {
        return bcFloatRef[bcFloatRefIndex++];
    }

    public int nextIMethodRef() {
        return bcIMethodRef[bcIMethodRefIndex++];
    }

    public int nextInitRef() {
        return bcInitRef[bcInitRefIndex++];
    }

    public int nextIntRef() {
        return bcIntRef[bcIntRefIndex++];
    }

    public int nextLabel() {
        return bcLabel[bcLabelIndex++];
    }

    public int nextLocal() {
        return bcLocal[bcLocalIndex++];
    }

    public int nextLongRef() {
        return bcLongRef[bcLongRefIndex++];
    }

    public int nextMethodRef() {
        return bcMethodRef[bcMethodRefIndex++];
    }

    public int nextShort() {
        return bcShort[bcShortIndex++];
    }

    public int nextStringRef() {
        return bcStringRef[bcStringRefIndex++];
    }

    public int nextSuperFieldRef() {
        return bcSuperField[bcSuperFieldIndex++];
    }

    public int nextSuperMethodRef() {
        return bcSuperMethod[bcSuperMethodIndex++];
    }

    public int nextThisFieldRef() {
        return bcThisField[bcThisFieldIndex++];
    }

    public int nextThisMethodRef() {
        return bcThisMethod[bcThisMethodIndex++];
    }

    public int nextWideByteCode() {
        return wideByteCodes[wideByteCodeIndex++];
    }

    public void setCurrentClass(final String string) {
        currentClass = string;
    }

    public void setNewClass(final String string) {
        newClass = string;
    }

    public void setSegment(final Segment segment) {
        this.segment = segment;
    }

    public void setSuperClass(final String string) {
        superClass = string;
    }
}
