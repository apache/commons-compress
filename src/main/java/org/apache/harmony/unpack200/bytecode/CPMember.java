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
package org.apache.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Superclass for member constant pool entries, such as fields or methods.
 */
public class CPMember extends ClassFileEntry {

    List attributes;
    short flags;
    CPUTF8 name;
    transient int nameIndex;
    protected final CPUTF8 descriptor;
    transient int descriptorIndex;

    /**
     * Create a new CPMember
     *
     * @param name
     * @param descriptor
     * @param flags
     * @param attributes
     * @throws NullPointerException
     *             if name or descriptor is null
     */
    public CPMember(CPUTF8 name, CPUTF8 descriptor, long flags, List attributes) {
        this.name = name;
        this.descriptor = descriptor;
        this.flags = (short) flags;
        this.attributes = (attributes == null ? Collections.EMPTY_LIST
                : attributes);
        if (name == null || descriptor == null) {
            throw new NullPointerException("Null arguments are not allowed");
        }
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        int attributeCount = attributes.size();
        ClassFileEntry[] entries = new ClassFileEntry[attributeCount + 2];
        entries[0] = name;
        entries[1] = descriptor;
        for (int i = 0; i < attributeCount; i++) {
            entries[i + 2] = (Attribute) attributes.get(i);
        }
        return entries;
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        nameIndex = pool.indexOf(name);
        descriptorIndex = pool.indexOf(descriptor);
        for(int it = 0; it < attributes.size(); it++) {
            Attribute attribute = (Attribute) attributes.get(it);
            attribute.resolve(pool);
        }
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + attributes.hashCode();
        result = PRIME * result + descriptor.hashCode();
        result = PRIME * result + flags;
        result = PRIME * result + name.hashCode();
        return result;
    }

    public String toString() {
        return "CPMember: " + name + "(" + descriptor + ")";
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CPMember other = (CPMember) obj;
        if (!attributes.equals(other.attributes))
            return false;
        if (!descriptor.equals(other.descriptor))
            return false;
        if (flags != other.flags)
            return false;
        if (!name.equals(other.name))
            return false;
        return true;
    }

    protected void doWrite(DataOutputStream dos) throws IOException {
        dos.writeShort(flags);
        dos.writeShort(nameIndex);
        dos.writeShort(descriptorIndex);
        int attributeCount = attributes.size();
        dos.writeShort(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            Attribute attribute = (Attribute) attributes.get(i);
            attribute.doWrite(dos);
        }
    }

}