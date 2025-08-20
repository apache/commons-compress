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
package org.apache.commons.compress.harmony.unpack200.bytecode.forms;

import java.util.Arrays;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Table switch instruction form.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class TableSwitchForm extends SwitchForm {

    /**
     * Constructs a new instance with the specified opcode, name, operandType and rewrite.
     *
     * @param opcode  index corresponding to the opcode's value.
     * @param name    String printable name of the opcode.
     */
    public TableSwitchForm(final int opcode, final String name) {
        super(opcode, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.forms.SwitchForm#setByteCodeOperands(org.apache.commons.
     * compress.harmony.unpack200.bytecode.ByteCode, org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager, int)
     */
    @Override
    public void setByteCodeOperands(final ByteCode byteCode, final OperandManager operandManager, final int codeLength) throws Pack200Exception {
        final int caseCount = operandManager.nextCaseCount();
        final int defaultPc = operandManager.nextLabel();
        final int caseValue = operandManager.nextCaseValues();
        // Check all at once here for all arrays in this method to account for failures seen in GH CI.
        Pack200Exception.checkIntArray(caseCount * 6); // yeah, might overflow.
        final int[] casePcs = new int[Pack200Exception.checkIntArray(caseCount)];
        Arrays.setAll(casePcs, i -> operandManager.nextLabel());
        final int[] labelsArray = new int[Pack200Exception.checkIntArray(caseCount + 1)];
        labelsArray[0] = defaultPc;
        System.arraycopy(casePcs, 0, labelsArray, 1, caseCount + 1 - 1);
        byteCode.setByteCodeTargets(labelsArray);
        final int lowValue = caseValue;
        final int highValue = lowValue + caseCount - 1;
        // All this gets dumped into the rewrite bytes of the
        // poor bytecode.
        // Unlike most byte codes, the TableSwitch is a
        // variable-sized bytecode. Because of this, the
        // rewrite array has to be defined here individually
        // for each bytecode, rather than in the ByteCodeForm
        // class.
        // First, there's the bytecode. Then there are 0-3
        // bytes of padding so that the first (default)
        // label is on a 4-byte offset.
        final int padLength = 3 - codeLength % 4;
        final int rewriteSize = 1 + padLength + 4 // defaultbytes
                + 4 // lowbyte
                + 4 // highbyte
                + 4 * casePcs.length;
        final int[] newRewrite = new int[Pack200Exception.checkIntArray(rewriteSize)];
        int rewriteIndex = 0;
        // Fill in what we can now
        // opcode
        newRewrite[rewriteIndex++] = byteCode.getOpcode();
        // padding
        Arrays.fill(newRewrite, rewriteIndex, rewriteIndex + padLength, 0);
        rewriteIndex += padLength;
        // defaultbyte
        // This gets overwritten by fixUpByteCodeTargets
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;
        newRewrite[rewriteIndex++] = -1;
        // lowbyte
        final int lowbyteIndex = rewriteIndex;
        setRewrite4Bytes(lowValue, lowbyteIndex, newRewrite);
        rewriteIndex += 4;
        // highbyte
        final int highbyteIndex = rewriteIndex;
        setRewrite4Bytes(highValue, highbyteIndex, newRewrite);
        rewriteIndex += 4;
        // jump offsets
        // The case_pcs will get overwritten by fixUpByteCodeTargets
        Arrays.fill(newRewrite, rewriteIndex, rewriteIndex + caseCount * 4, -1);
        byteCode.setRewrite(newRewrite);
    }
}
