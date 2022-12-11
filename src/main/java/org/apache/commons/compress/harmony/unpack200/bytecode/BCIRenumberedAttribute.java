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
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Abstract superclass for attributes that have some part encoded with a BCI renumbering
 */
public abstract class BCIRenumberedAttribute extends Attribute {

    protected boolean renumbered;

    public BCIRenumberedAttribute(final CPUTF8 attributeName) {
        super(attributeName);
    }

    @Override
    protected abstract int getLength();

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
     * In Pack200, line number tables are BCI renumbered. This method takes the byteCodeOffsets (which is a List of
     * Integers specifying the offset in the byte code array of each instruction) and updates the start_pcs so that it
     * points to the instruction index itself, not the BCI renumbering of the instruction.
     *
     * @param byteCodeOffsets List of Integer offsets of the bytecode array
     * @throws Pack200Exception TODO
     */
    public void renumber(final List<Integer> byteCodeOffsets) throws Pack200Exception {
        if (renumbered) {
            throw new Error("Trying to renumber a line number table that has already been renumbered");
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
