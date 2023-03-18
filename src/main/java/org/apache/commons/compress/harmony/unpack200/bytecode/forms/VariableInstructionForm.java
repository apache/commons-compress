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
package org.apache.commons.compress.harmony.unpack200.bytecode.forms;

/**
 * This abstract class implements the common code for instructions which have variable lengths. This is currently the
 * *switch instructions and some wide (_w) instructions.
 */
public abstract class VariableInstructionForm extends ByteCodeForm {

    public VariableInstructionForm(final int opcode, final String name) {
        super(opcode, name);
    }

    /**
     * This method writes operand directly into the rewrite array at index position specified.
     *
     * @param operand value to write
     * @param absPosition position in array to write. Note that this is absolute position in the array, so one can
     *        overwrite the bytecode if one isn't careful.
     * @param rewrite array to write into
     */
    public void setRewrite2Bytes(final int operand, final int absPosition, final int[] rewrite) {
        if (absPosition < 0) {
            throw new Error("Trying to rewrite " + this + " but there is no room for 4 bytes");
        }

        final int byteCodeRewriteLength = rewrite.length;

        if (absPosition + 1 > byteCodeRewriteLength) {
            throw new Error("Trying to rewrite " + this + " with an int at position " + absPosition
                + " but this won't fit in the rewrite array");
        }

        rewrite[absPosition] = ((0xFF00) & operand) >> 8;
        rewrite[absPosition + 1] = ((0x00FF) & operand);
    }

    /**
     * This method writes operand directly into the rewrite array at index position specified.
     *
     * @param operand value to write
     * @param absPosition position in array to write. Note that this is absolute position in the array, so one can
     *        overwrite the bytecode if one isn't careful.
     * @param rewrite array to write into
     */
    public void setRewrite4Bytes(final int operand, final int absPosition, final int[] rewrite) {
        if (absPosition < 0) {
            throw new Error("Trying to rewrite " + this + " but there is no room for 4 bytes");
        }

        final int byteCodeRewriteLength = rewrite.length;

        if (absPosition + 3 > byteCodeRewriteLength) {
            throw new Error("Trying to rewrite " + this + " with an int at position " + absPosition
                + " but this won't fit in the rewrite array");
        }

        rewrite[absPosition] = ((0xFF000000) & operand) >> 24;
        rewrite[absPosition + 1] = ((0x00FF0000) & operand) >> 16;
        rewrite[absPosition + 2] = ((0x0000FF00) & operand) >> 8;
        rewrite[absPosition + 3] = ((0x000000FF) & operand);
    }

    /**
     * Given an int operand, set the rewrite bytes for the next available operand position and the three immediately
     * following it to a highest-byte, mid-high, mid-low, low-byte encoding of the operand.
     *
     * Note that unlike the ByteCode setOperand* operations, this starts with an actual bytecode rewrite array (rather
     * than a ByteCodeForm prototype rewrite array). Also, this method overwrites -1 values in the rewrite array - so if
     * you start with an array that looks like: {100, -1, -1, -1, -1, 200, -1, -1, -1, -1} then calling
     * setRewrite4Bytes(0, rewrite) the first time will convert it to: {100, 0, 0, 0, 0, 200, -1, -1, -1, -1} Calling
     * setRewrite4Bytes(0, rewrite) a second time will convert it to: {100, 0, 0, 0, 0, 200, 0, 0, 0, 0}
     *
     * @param operand int to set the rewrite bytes to
     * @param rewrite int[] bytes to rewrite
     */
    public void setRewrite4Bytes(final int operand, final int[] rewrite) {
        int firstOperandPosition = -1;

        // Find the first -1 in the rewrite array
        for (int index = 0; index < rewrite.length - 3; index++) {
            if ((rewrite[index] == -1) && (rewrite[index + 1] == -1) && (rewrite[index + 2] == -1)
                && (rewrite[index + 3] == -1)) {
                firstOperandPosition = index;
                break;
            }
        }
        setRewrite4Bytes(operand, firstOperandPosition, rewrite);
    }
}
