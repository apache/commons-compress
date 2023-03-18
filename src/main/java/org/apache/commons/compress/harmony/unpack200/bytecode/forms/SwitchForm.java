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

public abstract class SwitchForm extends VariableInstructionForm {

    public SwitchForm(final int opcode, final String name) {
        super(opcode, name);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.forms.ByteCodeForm#fixUpByteCodeTargets(org.apache.commons
     * .compress.harmony.unpack200.bytecode.ByteCode,
     * org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute)
     */
    @Override
    public void fixUpByteCodeTargets(final ByteCode byteCode, final CodeAttribute codeAttribute) {
        // SwitchForms need to fix up the target of label operations
        final int[] originalTargets = byteCode.getByteCodeTargets();
        final int numberOfLabels = originalTargets.length;
        final int[] replacementTargets = new int[numberOfLabels];

        final int sourceIndex = byteCode.getByteCodeIndex();
        final int sourceValue = codeAttribute.byteCodeOffsets.get(sourceIndex).intValue();
        for (int index = 0; index < numberOfLabels; index++) {
            final int absoluteInstructionTargetIndex = sourceIndex + originalTargets[index];
            final int targetValue = codeAttribute.byteCodeOffsets.get(absoluteInstructionTargetIndex)
                .intValue();
            replacementTargets[index] = targetValue - sourceValue;
        }
        final int[] rewriteArray = byteCode.getRewrite();
        for (int index = 0; index < numberOfLabels; index++) {
            setRewrite4Bytes(replacementTargets[index], rewriteArray);
        }
    }

}
