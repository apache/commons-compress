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
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Abstract superclass for attributes that have some part encoded with Byte Code Injection (BCI) renumbering.
 */
public abstract class BCIRenumberedAttribute extends Attribute {

    /**
     * Whether renumbering has occurred.
     */
    protected boolean renumbered;

    /**
     * Constructs a new instance for an attribute name.
     *
     * @param attributeName an attribute name.
     */
    public BCIRenumberedAttribute(final CPUTF8 attributeName) {
        super(attributeName);
    }

    @Override
    protected abstract int getLength();

    /**
     * Gets the array of indices for the start of line numbers.
     *
     * @return the array of indices for the start of line numbers.
     */
    protected abstract int[] getStartPCs();

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#hasBCIRenumbering()
     */
    @Override
    public boolean hasBCIRenumbering() {
        return true;
    }

    /**
     * In Pack200, line number tables are BCI renumbered. This method takes the byteCodeOffsets (which is a List of Integers specifying the offset in the byte
     * code array of each instruction) and updates the start_pcs so that it points to the instruction index itself, not the BCI renumbering of the instruction.
     *
     * @param byteCodeOffsets List of Integer offsets of the byte code array.
     * @throws Pack200Exception Thrown from a subclass.
     */
    public void renumber(final List<Integer> byteCodeOffsets) throws Pack200Exception {
        if (renumbered) {
            throw new Pack200Exception("Trying to renumber a line number table that has already been renumbered");
        }
        renumbered = true;
        final int[] startPCs = getStartPCs();
        Arrays.setAll(startPCs, i -> byteCodeOffsets.get(startPCs[i]).intValue());
    }

    @Override
    public abstract String toString();

    @Override
    protected abstract void writeBody(DataOutputStream dos) throws IOException;

}
