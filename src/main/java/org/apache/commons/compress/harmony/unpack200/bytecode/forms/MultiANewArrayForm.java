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
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * This class implements the byte code form for the multianewarray instruction. It has a class reference and a byte
 * operand.
 *
 * MultiANewArrayForms (like other anewarray forms) do not track the last new().
 */
public class MultiANewArrayForm extends ClassRefForm {

    public MultiANewArrayForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
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
        // multianewarray has a class ref and a dimension.
        // The superclass handles the class ref.
        super.setByteCodeOperands(byteCode, operandManager, codeLength);

        // We have to handle the dimension.
        final int dimension = operandManager.nextByte();
        byteCode.setOperandByte(dimension, 2);
    }
}
