/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.compress.harmony.unpack200.bytecode;

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;

/**
 * Tracks operands, provides methods to let other classes get next elements, and also knows about which classes have been used recently in super, this and new
 * references.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
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

    /**
     * Constructs a new OperandManager.
     *
     * @param bcCaseCount the case counts.
     * @param bcCaseValue the case values.
     * @param bcByte the byte values.
     * @param bcShort the short values.
     * @param bcLocal the local values.
     * @param bcLabel the label values.
     * @param bcIntRef the integer references.
     * @param bcFloatRef the float references.
     * @param bcLongRef the long references.
     * @param bcDoubleRef the double references.
     * @param bcStringRef the string references.
     * @param bcClassRef the class references.
     * @param bcFieldRef the field references.
     * @param bcMethodRef the method references.
     * @param bcIMethodRef the interface method references.
     * @param bcThisField the this field references.
     * @param bcSuperField the super field references.
     * @param bcThisMethod the this method references.
     * @param bcSuperMethod the super method references.
     * @param bcInitRef the init references.
     * @param wideByteCodes the wide bytecodes.
     */
    public OperandManager(final int[] bcCaseCount, final int[] bcCaseValue, final int[] bcByte, final int[] bcShort, final int[] bcLocal, final int[] bcLabel,
            final int[] bcIntRef, final int[] bcFloatRef, final int[] bcLongRef, final int[] bcDoubleRef, final int[] bcStringRef, final int[] bcClassRef,
            final int[] bcFieldRef, final int[] bcMethodRef, final int[] bcIMethodRef, final int[] bcThisField, final int[] bcSuperField,
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

    /**
     * Gets the current class.
     *
     * @return the current class.
     */
    public String getCurrentClass() {
        if (null == currentClass) {
            throw new IllegalStateException("Current class not set yet");
        }
        return currentClass;
    }

    /**
     * Gets the new class.
     *
     * @return the new class.
     */
    public String getNewClass() {
        if (null == newClass) {
            throw new IllegalStateException("New class not set yet");
        }
        return newClass;
    }

    /**
     * Gets the super class.
     *
     * @return the super class.
     */
    public String getSuperClass() {
        if (null == superClass) {
            throw new IllegalStateException("SuperClass not set yet");
        }
        return superClass;
    }

    /**
     * Gets the global constant pool.
     *
     * @return the constant pool.
     */
    public SegmentConstantPool globalConstantPool() {
        return segment.getConstantPool();
    }

    /**
     * Gets the next byte value.
     *
     * @return the next byte.
     */
    public int nextByte() {
        return bcByte[bcByteIndex++];
    }

    /**
     * Gets the next case count.
     *
     * @return the next case count.
     */
    public int nextCaseCount() {
        return bcCaseCount[bcCaseCountIndex++];
    }

    /**
     * Gets the next case value.
     *
     * @return the next case value.
     */
    public int nextCaseValues() {
        return bcCaseValue[bcCaseValueIndex++];
    }

    /**
     * Gets the next class reference.
     *
     * @return the next class reference.
     */
    public int nextClassRef() {
        return bcClassRef[bcClassRefIndex++];
    }

    /**
     * Gets the next double reference.
     *
     * @return the next double reference.
     */
    public int nextDoubleRef() {
        return bcDoubleRef[bcDoubleRefIndex++];
    }

    /**
     * Gets the next field reference.
     *
     * @return the next field reference.
     */
    public int nextFieldRef() {
        return bcFieldRef[bcFieldRefIndex++];
    }

    /**
     * Gets the next float reference.
     *
     * @return the next float reference.
     */
    public int nextFloatRef() {
        return bcFloatRef[bcFloatRefIndex++];
    }

    /**
     * Gets the next interface method reference.
     *
     * @return the next interface method reference.
     */
    public int nextIMethodRef() {
        return bcIMethodRef[bcIMethodRefIndex++];
    }

    /**
     * Gets the next init reference.
     *
     * @return the next init reference.
     */
    public int nextInitRef() {
        return bcInitRef[bcInitRefIndex++];
    }

    /**
     * Gets the next int reference.
     *
     * @return the next int reference.
     */
    public int nextIntRef() {
        return bcIntRef[bcIntRefIndex++];
    }

    /**
     * Gets the next label.
     *
     * @return the next label.
     */
    public int nextLabel() {
        return bcLabel[bcLabelIndex++];
    }

    /**
     * Gets the next local.
     *
     * @return the next local.
     */
    public int nextLocal() {
        return bcLocal[bcLocalIndex++];
    }

    /**
     * Gets the next long reference.
     *
     * @return the next long reference.
     */
    public int nextLongRef() {
        return bcLongRef[bcLongRefIndex++];
    }

    /**
     * Gets the next method reference.
     *
     * @return the next method reference.
     */
    public int nextMethodRef() {
        return bcMethodRef[bcMethodRefIndex++];
    }

    /**
     * Gets the next short value.
     *
     * @return the next short.
     */
    public int nextShort() {
        return bcShort[bcShortIndex++];
    }

    /**
     * Gets the next string reference.
     *
     * @return the next string reference.
     */
    public int nextStringRef() {
        return bcStringRef[bcStringRefIndex++];
    }

    /**
     * Gets the next super field reference.
     *
     * @return the next super field reference.
     */
    public int nextSuperFieldRef() {
        return bcSuperField[bcSuperFieldIndex++];
    }

    /**
     * Gets the next super method reference.
     *
     * @return the next super method reference.
     */
    public int nextSuperMethodRef() {
        return bcSuperMethod[bcSuperMethodIndex++];
    }

    /**
     * Gets the next this field reference.
     *
     * @return the next this field reference.
     */
    public int nextThisFieldRef() {
        return bcThisField[bcThisFieldIndex++];
    }

    /**
     * Gets the next this method reference.
     *
     * @return the next this method reference.
     */
    public int nextThisMethodRef() {
        return bcThisMethod[bcThisMethodIndex++];
    }

    /**
     * Gets the next wide bytecode.
     *
     * @return the next wide bytecode.
     */
    public int nextWideByteCode() {
        return wideByteCodes[wideByteCodeIndex++];
    }

    /**
     * Sets the current class.
     *
     * @param string the current class name.
     */
    public void setCurrentClass(final String string) {
        currentClass = string;
    }

    /**
     * Sets the new class.
     *
     * @param newClass the new class name.
     */
    public void setNewClass(final String newClass) {
        this.newClass = newClass;
    }

    /**
     * Sets the segment.
     *
     * @param segment the segment.
     */
    public void setSegment(final Segment segment) {
        this.segment = segment;
    }

    /**
     * Sets the super class.
     *
     * @param superClass the super class name.
     */
    public void setSuperClass(final String superClass) {
        this.superClass = superClass;
    }
}
