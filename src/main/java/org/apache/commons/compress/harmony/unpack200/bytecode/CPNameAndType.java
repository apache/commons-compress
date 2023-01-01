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
import java.util.Objects;

import org.apache.commons.compress.harmony.unpack200.SegmentUtils;

/**
 * Name and Type pair constant pool entry.
 */
public class CPNameAndType extends ConstantPoolEntry {

    CPUTF8 descriptor;

    transient int descriptorIndex;

    CPUTF8 name;

    transient int nameIndex;

    private boolean hashCodeComputed;

    private int cachedHashCode;

    /**
     * Create a new CPNameAndType
     *
     * @param name TODO
     * @param descriptor TODO
     * @param globalIndex - index in CpBands
     * @throws NullPointerException if name or descriptor is null
     */
    public CPNameAndType(final CPUTF8 name, final CPUTF8 descriptor, final int globalIndex) {
        super(ConstantPoolEntry.CP_NameAndType, globalIndex);
        this.name = Objects.requireNonNull(name, "name");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    /*
     * field_info { u2 access_flags; u2 name_index; u2 descriptor_index; u2 attributes_count; attribute_info
     * attributes[attributes_count]; }
     */

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CPNameAndType other = (CPNameAndType) obj;
        if (!descriptor.equals(other.descriptor)) {
            return false;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    private void generateHashCode() {
        hashCodeComputed = true;
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + descriptor.hashCode();
        result = PRIME * result + name.hashCode();
        cachedHashCode = result;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] {name, descriptor};
    }
    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            generateHashCode();
        }
        return cachedHashCode;
    }

    /**
     * Answers the invokeinterface count argument when the receiver is treated as an invokeinterface target. This value
     * is not meaningful if the receiver is not an invokeinterface target.
     *
     * @return count
     */
    public int invokeInterfaceCount() {
        return 1 + SegmentUtils.countInvokeInterfaceArgs(descriptor.underlyingString());
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        descriptorIndex = pool.indexOf(descriptor);
        nameIndex = pool.indexOf(name);
    }

    @Override
    public String toString() {
        return "NameAndType: " + name + "(" + descriptor + ")";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(nameIndex);
        dos.writeShort(descriptorIndex);
    }
}