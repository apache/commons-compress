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
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

/**
 * This class implements references to methods defined in the superclass, which is set by this class in the OperandManager. Pack200 allows the superclass to be
 * inferred from context; this class tracks previous method reference superclasses to allow this.
 */
public class SuperMethodRefForm extends ClassSpecificReferenceForm {

    public SuperMethodRefForm(final int opcode, final String name, final int[] rewrite) {
        super(opcode, name, rewrite);
    }

    @Override
    protected String context(final OperandManager operandManager) {
        return operandManager.getSuperClass();
    }

    @Override
    protected int getOffset(final OperandManager operandManager) {
        return operandManager.nextSuperMethodRef();
    }

    @Override
    protected int getPoolID() {
        return SegmentConstantPool.CP_METHOD;
    }
}
