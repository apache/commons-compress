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
 * This class implements the byte code form for those bytecodes which have string references (and only string references).
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class StringRefForm extends SingleByteReferenceForm {

    /**
     * Constructs a new instance with the specified opcode, name, operandType and rewrite.
     *
     * @param opcode  index corresponding to the opcode's value.
     * @param name    String printable name of the opcode.
     * @param rewrite Operand positions (which will later be rewritten in ByteCodes) are indicated by -1.
     */
    public StringRefForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    /**
     * Constructs a new instance with the specified opcode, name, operandType and rewrite.
     *
     * @param opcode  index corresponding to the opcode's value.
     * @param name    String printable name of the opcode.
     * @param rewrite Operand positions (which will later be rewritten in ByteCodes) are indicated by -1.
     * @param widened TODO.
     */
    public StringRefForm(final int opcode, final String name, final int[] rewrite, final boolean widened) {
        this(opcode, name, rewrite);
        this.widened = widened;
    }

    @Override
    protected int getOffset(final OperandManager operandManager) {
        return operandManager.nextStringRef();
    }

    @Override
    protected int getPoolID() {
        return SegmentConstantPool.CP_STRING;
    }

    @Override
    protected void setNestedEntries(final ByteCode byteCode, final OperandManager operandManager, final int offset) throws Pack200Exception {
        final SegmentConstantPool globalPool = operandManager.globalConstantPool();
        final ClassFileEntry[] nested = { globalPool.getValue(getPoolID(), offset) };
        byteCode.setNested(nested);
        if (widened) {
            byteCode.setNestedPositions(new int[][] { { 0, 2 } });
        } else {
            byteCode.setNestedPositions(new int[][] { { 0, 1 } });
        }
    }
}
