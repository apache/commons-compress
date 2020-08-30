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

import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInterfaceMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * This class implements the byte code form for those bytecodes which have
 * IMethod references (and only IMethod references).
 */
public class IMethodRefForm extends ReferenceForm {

    public IMethodRefForm(int opcode, String name, int[] rewrite) {
        super(opcode, name, rewrite);
    }

    protected int getOffset(OperandManager operandManager) {
        return operandManager.nextIMethodRef();
    }

    protected int getPoolID() {
        return SegmentConstantPool.CP_IMETHOD;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.forms.ByteCodeForm#setByteCodeOperands(org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode,
     *      org.apache.commons.compress.harmony.unpack200.bytecode.OperandTable,
     *      org.apache.commons.compress.harmony.unpack200.Segment)
     */
    public void setByteCodeOperands(ByteCode byteCode,
            OperandManager operandManager, int codeLength) {
        super.setByteCodeOperands(byteCode, operandManager, codeLength);
        final int count = ((CPInterfaceMethodRef) byteCode
                .getNestedClassFileEntries()[0]).invokeInterfaceCount();
        byteCode.getRewrite()[3] = count;
    }
}
