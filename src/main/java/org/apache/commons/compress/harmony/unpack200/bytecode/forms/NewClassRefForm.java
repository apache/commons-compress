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
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * This class is an extension of the ClassRefForm. It has two purposes: 1. To keep track of the last type used in a
 * new() instruction in the current class. 2. To allow the sender to create instances of either a specified class (which
 * then becomes the new class) or the last used new class.
 */
public class NewClassRefForm extends ClassRefForm {

    public NewClassRefForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.forms.ReferenceForm#setByteCodeOperands(org.apache.commons
     * .compress.harmony.unpack200.bytecode.ByteCode,
     * org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager)
     */
    @Override
    public void setByteCodeOperands(final ByteCode byteCode, final OperandManager operandManager,
        final int codeLength) {
        ClassFileEntry[] nested = null;
        final int offset = getOffset(operandManager);
        if (offset == 0) {
            // Use current class
            final SegmentConstantPool globalPool = operandManager.globalConstantPool();
            nested = new ClassFileEntry[] {globalPool.getClassPoolEntry(operandManager.getCurrentClass())};
            byteCode.setNested(nested);
            byteCode.setNestedPositions(new int[][] {{0, 2}});
        } else {
            // Look up the class in the classpool
            try {
                // Parent takes care of subtracting one from offset
                // to adjust for 1-based global pool
                setNestedEntries(byteCode, operandManager, offset);
            } catch (final Pack200Exception ex) {
                throw new Error("Got a pack200 exception. What to do?");
            }
        }
        operandManager.setNewClass(((CPClass) byteCode.getNestedClassFileEntries()[0]).getName());
    }
}
