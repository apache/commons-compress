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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Local variable table
 */
public class LocalVariableTableAttribute extends BCIRenumberedAttribute {

    private static CPUTF8 attributeName;
    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }
    private final int localVariableTableLength;
    private final int[] startPcs;
    private final int[] lengths;
    private int[] nameIndexes;
    private int[] descriptorIndexes;
    private final int[] indexes;
    private final CPUTF8[] names;
    private final CPUTF8[] descriptors;

    private int codeLength;

    public LocalVariableTableAttribute(final int localVariableTableLength, final int[] startPcs,
        final int[] lengths, final CPUTF8[] names, final CPUTF8[] descriptors, final int[] indexes) {
        super(attributeName);
        this.localVariableTableLength = localVariableTableLength;
        this.startPcs = startPcs;
        this.lengths = lengths;
        this.names = names;
        this.descriptors = descriptors;
        this.indexes = indexes;
    }

    @Override
    protected int getLength() {
        return 2 + (10 * localVariableTableLength);
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<CPUTF8> nestedEntries = new ArrayList<>();
        nestedEntries.add(getAttributeName());
        for (int i = 0; i < localVariableTableLength; i++) {
            nestedEntries.add(names[i]);
            nestedEntries.add(descriptors[i]);
        }
        return nestedEntries.toArray(ClassFileEntry.NONE);
    }

    @Override
    protected int[] getStartPCs() {
        return startPcs;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.BCIRenumberedAttribute#renumber(java.util.List)
     */
    @Override
    public void renumber(final List<Integer> byteCodeOffsets) throws Pack200Exception {
        // Remember the unrenumbered startPcs, since that's used later
        // to calculate end position.
        final int[] unrenumberedStartPcs = Arrays.copyOf(startPcs, startPcs.length);

        // Next renumber startPcs in place
        super.renumber(byteCodeOffsets);

        // lengths are BRANCH5 encoded, not BCI-encoded.
        // In other words:
        // startPc is BCI5 startPc
        // endPc is byteCodeOffset[(index of startPc in byteCodeOffset) +
        // (encoded length)]
        // real length = endPc - startPc
        // special case if endPc is beyond end of bytecode array

        final int maxSize = codeLength;

        // Iterate through the lengths and update each in turn.
        // This is done in place in the lengths array.
        for (int index = 0; index < lengths.length; index++) {
            final int startPc = startPcs[index];
            int revisedLength = -1;
            final int encodedLength = lengths[index];

            // First get the index of the startPc in the byteCodeOffsets
            final int indexOfStartPC = unrenumberedStartPcs[index];
            // Given the index of the startPc, we can now add
            // the encodedLength to it to get the stop index.
            final int stopIndex = indexOfStartPC + encodedLength;
            if (stopIndex < 0) {
                throw new Pack200Exception("Error renumbering bytecode indexes");
            }
            // Length can either be an index into the byte code offsets, or one
            // beyond the
            // end of the byte code offsets. Need to determine which this is.
            if (stopIndex == byteCodeOffsets.size()) {
                // Pointing to one past the end of the byte code array
                revisedLength = maxSize - startPc;
            } else {
                // We're indexed into the byte code array
                final int stopValue = byteCodeOffsets.get(stopIndex).intValue();
                revisedLength = stopValue - startPc;
            }
            lengths[index] = revisedLength;
        }
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        nameIndexes = new int[localVariableTableLength];
        descriptorIndexes = new int[localVariableTableLength];
        for (int i = 0; i < localVariableTableLength; i++) {
            names[i].resolve(pool);
            descriptors[i].resolve(pool);
            nameIndexes[i] = pool.indexOf(names[i]);
            descriptorIndexes[i] = pool.indexOf(descriptors[i]);
        }
    }

    public void setCodeLength(final int length) {
        codeLength = length;
    }

    @Override
    public String toString() {
        return "LocalVariableTable: " + +localVariableTableLength + " variables";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(localVariableTableLength);
        for (int i = 0; i < localVariableTableLength; i++) {
            dos.writeShort(startPcs[i]);
            dos.writeShort(lengths[i]);
            dos.writeShort(nameIndexes[i]);
            dos.writeShort(descriptorIndexes[i]);
            dos.writeShort(indexes[i]);
        }
    }
}
