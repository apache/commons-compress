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

import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * This class implements the byte code form for those bytecodes which have label references (and only label references).
 */
public class LabelForm extends ByteCodeForm {

    protected boolean widened;

    public LabelForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    public LabelForm(final int opcode, final String name, final int[] rewrite, final boolean widened) {
        this(opcode, name, rewrite);
        this.widened = widened;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.forms.ByteCodeForm#fixUpByteCodeTarget(org.apache.commons.
     * compress.harmony.unpack200.bytecode.ByteCode,
     * org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute)
     */
    @Override
    public void fixUpByteCodeTargets(final ByteCode byteCode, final CodeAttribute codeAttribute) {
        // LabelForms need to fix up the target of label operations
        final int originalTarget = byteCode.getByteCodeTargets()[0];
        final int sourceIndex = byteCode.getByteCodeIndex();
        final int absoluteInstructionTargetIndex = sourceIndex + originalTarget;
        final int targetValue = codeAttribute.byteCodeOffsets.get(absoluteInstructionTargetIndex)
            .intValue();
        final int sourceValue = codeAttribute.byteCodeOffsets.get(sourceIndex).intValue();
        // The operand is the difference between the source instruction
        // and the destination instruction.
        byteCode.setOperandSigned2Bytes(targetValue - sourceValue, 0);
        if (widened) {
            byteCode.setNestedPositions(new int[][] {{0, 4}});
        } else {
            byteCode.setNestedPositions(new int[][] {{0, 2}});
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.forms.ByteCodeForm#setByteCodeOperands(org.apache.commons.
     * compress.harmony.unpack200.bytecode.ByteCode,
     * org.apache.commons.compress.harmony.unpack200.bytecode.OperandTable,
     * org.apache.commons.compress.harmony.unpack200.SegmentConstantPool)
     */
    @Override
    public void setByteCodeOperands(final ByteCode byteCode, final OperandManager operandManager,
        final int codeLength) {
        byteCode.setByteCodeTargets(new int[] {operandManager.nextLabel()});
        // The byte code operands actually get set later -
        // once we have all the bytecodes - in fixUpByteCodeTarget().
        return;
    }
}
