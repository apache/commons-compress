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

package org.apache.commons.compress.harmony.unpack200;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;
import org.apache.commons.compress.harmony.unpack200.bytecode.forms.TableSwitchForm;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TableSwitchForm}.
 */
class TableSwitchFormTest {

    /**
     * Run with {@code -Xmx64m} to get a Pack200Exception(MemoryLimitException).
     *
     * @throws Pack200Exception
     */
    @Test
    void test() throws Pack200Exception {
        final int large = 1_000_000_000;
        final int[] caseCount = { large };
        // Provide a dummy label for the defaultPc branch taken before the allocation.
        final int[] labels = { 0 };
        final int[] caseValues = { 0 }; // TableSwitch needs at least one case value.
        final int[] empty = {};
        // The OperandManager requires all 21 band arrays.
        // Most can be empty for this test.
        final OperandManager om = new OperandManager(caseCount, caseValues, empty, empty, empty, labels, empty, empty, empty, empty, empty, empty, empty, empty,
                empty, empty, empty, empty, empty, empty, empty);
        final ByteCode bc = ByteCode.getByteCode(170); // tableswitch
        // Run with -Xmx64m to get a Pack200Exception(MemoryLimitException).
        assertThrows(Exception.class, () -> new TableSwitchForm(170, "tableswitch").setByteCodeOperands(bc, om, 0));
    }
}
