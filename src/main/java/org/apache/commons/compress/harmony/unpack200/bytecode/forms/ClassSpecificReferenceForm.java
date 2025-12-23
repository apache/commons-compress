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

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Abstract superclass of all classes that have class-specific references to constant pool information. These classes have a context (a string representing a
 * pack200 class) i.e., they send getClassSpecificPoolEntry instead of getConstantPoolEntry.
 */
public abstract class ClassSpecificReferenceForm extends ReferenceForm {

    /**
     * Constructs a new instance with the specified opcode, name, operandType and rewrite.
     *
     * @param opcode  index corresponding to the opcode's value.
     * @param name    String printable name of the opcode.
     * @param rewrite Operand positions (which will later be rewritten in ByteCodes) are indicated by -1.
     */
    public ClassSpecificReferenceForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    /**
     * Gets the context from the operand manager.
     *
     * @param operandManager the operand manager.
     * @return the context string.
     */
    protected abstract String context(OperandManager operandManager);

    @Override
    protected abstract int getOffset(OperandManager operandManager);

    @Override
    protected abstract int getPoolID();

    @Override
    protected void setNestedEntries(final ByteCode byteCode, final OperandManager operandManager, final int offset) throws Pack200Exception {
        final SegmentConstantPool globalPool = operandManager.globalConstantPool();
        final ClassFileEntry[] nested = { globalPool.getClassSpecificPoolEntry(getPoolID(), offset, context(operandManager)) };
        byteCode.setNested(nested);
        byteCode.setNestedPositions(new int[][] { { 0, 2 } });
    }

}
