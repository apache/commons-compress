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
 * This class is used for representations of cldc and cldc_w. In these cases, a narrow class ref has one byte and a wide
 * class ref has two bytes.
 */
public class NarrowClassRefForm extends ClassRefForm {

    public NarrowClassRefForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    public NarrowClassRefForm(final int opcode, final String name, final int[] rewrite, final boolean widened) {
        super(opcode, name, rewrite, widened);
    }

    @Override
    public boolean nestedMustStartClassPool() {
        return !widened;
    }

    @Override
    protected void setNestedEntries(final ByteCode byteCode, final OperandManager operandManager, final int offset)
        throws Pack200Exception {
        super.setNestedEntries(byteCode, operandManager, offset);
        if (!widened) {
            byteCode.setNestedPositions(new int[][] {{0, 1}});
        }
    }
}
