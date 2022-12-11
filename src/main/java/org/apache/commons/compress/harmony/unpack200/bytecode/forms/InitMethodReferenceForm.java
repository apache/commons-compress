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

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.SegmentConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Abstract superclass of those classes which look up init methods (these are class specific methods). They use
 * getInitMethodPoolEntry to find the methods.
 */
public abstract class InitMethodReferenceForm extends ClassSpecificReferenceForm {

    public InitMethodReferenceForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    @Override
    protected abstract String context(OperandManager operandManager);

    @Override
    protected int getOffset(final OperandManager operandManager) {
        return operandManager.nextInitRef();
    }

    @Override
    protected int getPoolID() {
        return SegmentConstantPool.CP_METHOD;
    }

    @Override
    protected void setNestedEntries(final ByteCode byteCode, final OperandManager operandManager, final int offset)
        throws Pack200Exception {
        final SegmentConstantPool globalPool = operandManager.globalConstantPool();
        ClassFileEntry[] nested = null;
        nested = new ClassFileEntry[] {
            globalPool.getInitMethodPoolEntry(SegmentConstantPool.CP_METHOD, offset, context(operandManager))};
        byteCode.setNested(nested);
        byteCode.setNestedPositions(new int[][] {{0, 2}});
    }
}
