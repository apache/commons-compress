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
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * Some bytecodes (such as (a)ldc, fldc and ildc) have single- byte references to the class pool. This class is the
 * abstract superclass of those classes.
 */
public abstract class SingleByteReferenceForm extends ReferenceForm {

    protected boolean widened;

    public SingleByteReferenceForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    @Override
    protected abstract int getOffset(OperandManager operandManager);

    @Override
    protected abstract int getPoolID();

    @Override
    public boolean nestedMustStartClassPool() {
        return !widened;
    }

    @Override
    protected void setNestedEntries(final ByteCode byteCode, final OperandManager operandManager, final int offset)
        throws Pack200Exception {
        super.setNestedEntries(byteCode, operandManager, offset);
        if (widened) {
            byteCode.setNestedPositions(new int[][] {{0, 2}});
        } else {
            byteCode.setNestedPositions(new int[][] {{0, 1}});
        }
    }
}
