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
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Local variable table
 */
public class LocalVariableTableAttribute extends BCIRenumberedAttribute {

    private final int local_variable_table_length;
    private final int[] start_pcs;
    private final int[] lengths;
    private int[] name_indexes;
    private int[] descriptor_indexes;
    private final int[] indexes;
    private final CPUTF8[] names;
    private final CPUTF8[] descriptors;
    private int codeLength;
    private static CPUTF8 attributeName;

    public static void setAttributeName(CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    public LocalVariableTableAttribute(int local_variable_table_length,
            int[] start_pcs, int[] lengths, CPUTF8[] names,
            CPUTF8[] descriptors, int[] indexes) {
        super(attributeName);
        this.local_variable_table_length = local_variable_table_length;
        this.start_pcs = start_pcs;
        this.lengths = lengths;
        this.names = names;
        this.descriptors = descriptors;
        this.indexes = indexes;
    }


    public void setCodeLength(int length) {
        codeLength = length;
    }

    protected int getLength() {
        return 2 + (10 * local_variable_table_length);
    }

    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(local_variable_table_length);
        for (int i = 0; i < local_variable_table_length; i++) {
            dos.writeShort(start_pcs[i]);
            dos.writeShort(lengths[i]);
            dos.writeShort(name_indexes[i]);
            dos.writeShort(descriptor_indexes[i]);
            dos.writeShort(indexes[i]);
        }
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        ArrayList nestedEntries = new ArrayList();
        nestedEntries.add(getAttributeName());
        for (int i = 0; i < local_variable_table_length; i++) {
            nestedEntries.add(names[i]);
            nestedEntries.add(descriptors[i]);
        }
        ClassFileEntry[] nestedEntryArray = new ClassFileEntry[nestedEntries
                .size()];
        nestedEntries.toArray(nestedEntryArray);
        return nestedEntryArray;
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        name_indexes = new int[local_variable_table_length];
        descriptor_indexes = new int[local_variable_table_length];
        for (int i = 0; i < local_variable_table_length; i++) {
            names[i].resolve(pool);
            descriptors[i].resolve(pool);
            name_indexes[i] = pool.indexOf(names[i]);
            descriptor_indexes[i] = pool.indexOf(descriptors[i]);
        }
    }

    public String toString() {
        return "LocalVariableTable: " + +local_variable_table_length
                + " variables";
    }

    protected int[] getStartPCs() {
        return start_pcs;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.BCIRenumberedAttribute#renumber(java.util.List)
     */
    public void renumber(List byteCodeOffsets) throws Pack200Exception {
        // Remember the unrenumbered start_pcs, since that's used later
        // to calculate end position.
        int[] unrenumbered_start_pcs = new int[start_pcs.length];
        System.arraycopy(start_pcs, 0, unrenumbered_start_pcs, 0,
                start_pcs.length);

        // Next renumber start_pcs in place
        super.renumber(byteCodeOffsets);

        // lengths are BRANCH5 encoded, not BCI-encoded.
        // In other words:
        // start_pc is BCI5 start_pc
        // end_pc is byteCodeOffset[(index of start_pc in byteCodeOffset) +
        // (encoded length)]
        // real length = end_pc - start_pc
        // special case if end_pc is beyond end of bytecode array

        int maxSize = codeLength;

        // Iterate through the lengths and update each in turn.
        // This is done in place in the lengths array.
        for (int index = 0; index < lengths.length; index++) {
            int start_pc = start_pcs[index];
            int revisedLength = -1;
            int encodedLength = lengths[index];

            // First get the index of the start_pc in the byteCodeOffsets
            int indexOfStartPC = unrenumbered_start_pcs[index];
            // Given the index of the start_pc, we can now add
            // the encodedLength to it to get the stop index.
            int stopIndex = indexOfStartPC + encodedLength;
            if (stopIndex < 0) {
                throw new Pack200Exception("Error renumbering bytecode indexes");
            }
            // Length can either be an index into the byte code offsets, or one
            // beyond the
            // end of the byte code offsets. Need to determine which this is.
            if (stopIndex == byteCodeOffsets.size()) {
                // Pointing to one past the end of the byte code array
                revisedLength = maxSize - start_pc;
            } else {
                // We're indexed into the byte code array
                int stopValue = ((Integer) byteCodeOffsets.get(stopIndex))
                        .intValue();
                revisedLength = stopValue - start_pc;
            }
            lengths[index] = revisedLength;
        }
    }
}
