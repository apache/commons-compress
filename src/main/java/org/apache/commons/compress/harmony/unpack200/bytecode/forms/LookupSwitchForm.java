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
 * Lookup switch instruction form.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class LookupSwitchForm extends SwitchForm {

    /**
     * Constructs a new instance with the specified opcode, name, operandType and rewrite.
     *
     * @param opcode  index corresponding to the opcode's value.
     * @param name    String printable name of the opcode.
     */
    public LookupSwitchForm(final int opcode, final String name) {
        super(opcode, name);
    }

    @Override
    public void setByteCodeOperands(final ByteCode byteCode, final OperandManager operandManager, final int codeLength) throws Pack200Exception {
        final int caseCount = operandManager.nextCaseCount();
        final int defaultPc = operandManager.nextLabel();
        // Check all at once here for all arrays in this method to account for failures seen in GH CI.
        Pack200Exception.checkIntArray(caseCount * 8); // yeah, might overflow.
        final int[] caseValues = new int[Pack200Exception.checkIntArray(caseCount)];
        Arrays.setAll(caseValues, i -> operandManager.nextCaseValues());
        final int[] casePcs = new int[Pack200Exception.checkIntArray(caseCount)];
        Arrays.setAll(casePcs, i -> operandManager.nextLabel());
        final int[] labelsArray = new int[Pack200Exception.checkIntArray(caseCount + 1)];
        labelsArray[0] = defaultPc;
        System.arraycopy(casePcs, 0, labelsArray, 1, caseCount + 1 - 1);
        byteCode.setByteCodeTargets(labelsArray);
        // All this gets dumped into the rewrite bytes of the
        // poor bytecode.
        //
        // Unlike most byte codes, the LookupSwitch is a
        // variable-sized bytecode. Because of this, the
        // rewrite array has to be defined here individually
        // for each bytecode, rather than in the ByteCodeForm
        // class.
        //
        // First, there's the bytecode. Then there are 0-3
        // bytes of padding so that the first (default)
        // label is on a 4-byte offset.
        final int padLength = 3 - codeLength % 4;
        final int rewriteSize = 1 + padLength + 4 // defaultbytes
                + 4 // npairs
                + 4 * caseValues.length + 4 * casePcs.length;
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
        // npairs
        final int npairsIndex = rewriteIndex;
        setRewrite4Bytes(caseValues.length, npairsIndex, newRewrite);
        rewriteIndex += 4;
        // match-offset pairs
        // The caseValues aren't overwritten, but the
        // casePcs will get overwritten by fixUpByteCodeTargets
        for (final int caseValue : caseValues) {
            // match
            setRewrite4Bytes(caseValue, rewriteIndex, newRewrite);
            rewriteIndex += 4;
            // offset
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
            newRewrite[rewriteIndex++] = -1;
        }
        byteCode.setRewrite(newRewrite);
    }
}
