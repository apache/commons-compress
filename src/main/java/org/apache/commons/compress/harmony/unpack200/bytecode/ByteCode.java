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

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.commons.compress.harmony.unpack200.bytecode.forms.ByteCodeForm;

/**
 * A bytecode class file entry.
 */
public class ByteCode extends ClassFileEntry {

    private static ByteCode[] noArgByteCodes = new ByteCode[255];

    public static ByteCode getByteCode(final int opcode) {
        final int byteOpcode = 0xFF & opcode;
        if (ByteCodeForm.get(byteOpcode).hasNoOperand()) {
            if (null == noArgByteCodes[byteOpcode]) {
                noArgByteCodes[byteOpcode] = new ByteCode(byteOpcode);
            }
            return noArgByteCodes[byteOpcode];
        }
        return new ByteCode(byteOpcode);
    }

    private final ByteCodeForm byteCodeForm;

    private ClassFileEntry[] nested;
    private int[][] nestedPositions;
    private int[] rewrite;

    private int byteCodeOffset = -1;
    private int[] byteCodeTargets;

    protected ByteCode(final int opcode) {
        this(opcode, ClassFileEntry.NONE);
    }

    protected ByteCode(final int opcode, final ClassFileEntry[] nested) {
        this.byteCodeForm = ByteCodeForm.get(opcode);
        this.rewrite = byteCodeForm.getRewriteCopy();
        this.nested = nested;
    }

    /**
     * Some ByteCodes (in particular, those with labels need to be fixed up after all the bytecodes in the CodeAttribute
     * have been added. (This can't be done beforehand because the CodeAttribute needs to be complete before targets can
     * be assigned.)
     *
     * @param codeAttribute the code attribute
     */
    public void applyByteCodeTargetFixup(final CodeAttribute codeAttribute) {
        getByteCodeForm().fixUpByteCodeTargets(this, codeAttribute);
    }

    @Override
    protected void doWrite(final DataOutputStream dos) throws IOException {
        for (final int element : rewrite) {
            dos.writeByte(element);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    public void extractOperands(final OperandManager operandManager, final Segment segment, final int codeLength) {
        // Given an OperandTable, figure out which operands
        // the receiver needs and stuff them in operands.
        // Later on the operands can be rewritten (But that's
        // later, not now).
        final ByteCodeForm currentByteCodeForm = getByteCodeForm();
        currentByteCodeForm.setByteCodeOperands(this, operandManager, codeLength);
    }

    protected ByteCodeForm getByteCodeForm() {
        return byteCodeForm;
    }

    public int getByteCodeIndex() {
        return byteCodeOffset;
    }

    public int[] getByteCodeTargets() {
        return byteCodeTargets;
    }

    public int getLength() {
        return rewrite.length;
    }

    public String getName() {
        return getByteCodeForm().getName();
    }

    @Override
    public ClassFileEntry[] getNestedClassFileEntries() {
        return nested;
    }

    public int[] getNestedPosition(final int index) {
        return getNestedPositions()[index];
    }

    public int[][] getNestedPositions() {
        return nestedPositions;
    }

    public int getOpcode() {
        return getByteCodeForm().getOpcode();
    }

    /**
     * Some bytecodes (the ones with variable lengths) can't have a static rewrite array - they need the ability to
     * update the array. This method permits their associated bytecode formst to query their rewrite array.
     *
     * Note that this should not be called from bytecodes which have a static rewrite; use the table in ByteCodeForm
     * instead to specify those rewrites.
     *
     * @return Some bytecodes.
     */
    public int[] getRewrite() {
        return rewrite;
    }

    @Override
    public int hashCode() {
        return objectHashCode();
    }

    /**
     * This method will answer true if the receiver is a multi-bytecode instruction (such as aload0_putfield_super);
     * otherwise, it will answer false.
     *
     * @return boolean true if multibytecode, false otherwise
     */
    public boolean hasMultipleByteCodes() {
        return getByteCodeForm().hasMultipleByteCodes();
    }

    public boolean nestedMustStartClassPool() {
        return byteCodeForm.nestedMustStartClassPool();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry#resolve(org.apache.commons.compress.harmony
     * .unpack200.bytecode.ClassConstantPool)
     */
    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        if (nested.length > 0) {
            // Update the bytecode rewrite array so that it points
            // to the elements of the nested array.
            for (int index = 0; index < nested.length; index++) {
                final int argLength = getNestedPosition(index)[1];
                switch (argLength) {

                case 1:
                    setOperandByte(pool.indexOf(nested[index]), getNestedPosition(index)[0]);
                    break;

                case 2:
                    setOperand2Bytes(pool.indexOf(nested[index]), getNestedPosition(index)[0]);
                    break;

                default:
                    throw new Error("Unhandled resolve " + this);
                }
            }
        }
    }

    /**
     * ByteCodes may need to know their position in the code array (in particular, label byte codes need to know where
     * they are in order to calculate their targets). This method lets the CodeAttribute specify where the byte code is.
     *
     * Since there are no aload0+label instructions, this method doesn't worry about multioperation bytecodes.
     *
     * @param byteCodeOffset int position in code array.
     */
    public void setByteCodeIndex(final int byteCodeOffset) {
        this.byteCodeOffset = byteCodeOffset;
    }

    /**
     * Some ByteCodes (in particular, those with labels) have to keep track of byteCodeTargets. These are initially
     * offsets in the CodeAttribute array relative to the byteCodeOffset, but later get fixed up to point to the
     * absolute position in the CodeAttribute array. This method sets the targets.
     *
     * @param byteCodeTargets int index in array
     */
    public void setByteCodeTargets(final int[] byteCodeTargets) {
        this.byteCodeTargets = byteCodeTargets;
    }

    public void setNested(final ClassFileEntry[] nested) {
        this.nested = nested;
    }

    /**
     * nestedPositions is an array of arrays of ints. Each subarray specifies a position of a nested element (from the
     * nested[] array) and the length of that element.
     *
     * For instance, one might have a nested of: {CPClass java/lang/Foo, CPFloat 3.14} The nestedPositions would then
     * be: {{0,2},{2,2}} In other words, when the bytecode is resolved, the CPClass will be resolved to an int and
     * inserted at position 0 and 1 of the rewrite arguments (the first occurrences of -1). The CPFloat will be resolved
     * to an int position and inserted at positions 2 and 3 of the rewrite arguments.
     *
     * @param nestedPositions Each subarray specifies a position of a nested element (from the nested[] array) and the
     *        length of that element.
     */
    public void setNestedPositions(final int[][] nestedPositions) {
        this.nestedPositions = nestedPositions;
    }

    /**
     * Given an int operand, set the rewrite bytes for that position and the one immediately following it to a
     * high-byte, low-byte encoding of the operand.
     *
     * @param operand int to set the rewrite bytes to
     * @param position int position in the operands of the rewrite bytes. For a rewrite array of {100, -1, -1, -1}
     *        position 0 is the first -1, position 1 is the second -1, etc.
     */
    public void setOperand2Bytes(final int operand, final int position) {
        final int firstOperandIndex = getByteCodeForm().firstOperandIndex();
        final int byteCodeFormLength = getByteCodeForm().getRewrite().length;
        if (firstOperandIndex < 1) {
            // No operand rewriting permitted for this bytecode
            throw new Error("Trying to rewrite " + this + " that has no rewrite");
        }

        if (firstOperandIndex + position + 1 > byteCodeFormLength) {
            throw new Error("Trying to rewrite " + this + " with an int at position " + position
                + " but this won't fit in the rewrite array");
        }

        rewrite[firstOperandIndex + position] = (operand & 0xFF00) >> 8;
        rewrite[firstOperandIndex + position + 1] = operand & 0xFF;
    }

    /**
     * Given an int operand, treat it as a byte and set the rewrite byte for that position to that value. Mask of
     * anything beyond 0xFF.
     *
     * @param operand int to set the rewrite byte to (unsigned)
     * @param position int position in the operands of the rewrite bytes. For a rewrite array of {100, -1, -1, -1}
     *        position 0 is the first -1, position 1 is the second -1, etc.
     */
    public void setOperandByte(final int operand, final int position) {
        final int firstOperandIndex = getByteCodeForm().firstOperandIndex();
        final int byteCodeFormLength = getByteCodeForm().operandLength();
        if (firstOperandIndex < 1) {
            // No operand rewriting permitted for this bytecode
            throw new Error("Trying to rewrite " + this + " that has no rewrite");
        }

        if (firstOperandIndex + position > byteCodeFormLength) {
            throw new Error("Trying to rewrite " + this + " with an byte at position " + position
                + " but this won't fit in the rewrite array");
        }

        rewrite[firstOperandIndex + position] = operand & 0xFF;
    }

    /**
     * Given an array of ints which correspond to bytes in the operand of the bytecode, set the rewrite bytes of the
     * operand to be the appropriate values. All values in operands[] will be masked with 0xFF so they fit into a byte.
     *
     * @param operands int[] rewrite operand bytes
     */
    public void setOperandBytes(final int[] operands) {
        final int firstOperandIndex = getByteCodeForm().firstOperandIndex();
        final int byteCodeFormLength = getByteCodeForm().operandLength();
        if (firstOperandIndex < 1) {
            // No operand rewriting permitted for this bytecode
            throw new Error("Trying to rewrite " + this + " that has no rewrite");
        }

        if (byteCodeFormLength != operands.length) {
            throw new Error("Trying to rewrite " + this + " with " + operands.length + " but bytecode has length "
                + byteCodeForm.operandLength());
        }

        for (int index = 0; index < byteCodeFormLength; index++) {
            rewrite[index + firstOperandIndex] = operands[index] & 0xFF;
        }
    }

    /**
     * This is just like setOperandInt, but takes special care when the operand is less than 0 to make sure it's written
     * correctly.
     *
     * @param operand int to set the rewrite bytes to
     * @param position int position of the operands in the rewrite bytes
     */
    public void setOperandSigned2Bytes(final int operand, final int position) {
        if (operand >= 0) {
            setOperand2Bytes(operand, position);
        } else {
            final int twosComplementOperand = 0x10000 + operand;
            setOperand2Bytes(twosComplementOperand, position);
        }
    }

    /**
     * Some bytecodes (the ones with variable lengths) can't have a static rewrite array - they need the ability to
     * update the array. This method permits that.
     *
     * Note that this should not be called from bytecodes which have a static rewrite; use the table in ByteCodeForm
     * instead to specify those rewrites.
     *
     * @param rewrite Some bytecodes.
     */
    public void setRewrite(final int[] rewrite) {
        this.rewrite = rewrite;
    }

    @Override
    public String toString() {
        return getByteCodeForm().getName();
    }
}
