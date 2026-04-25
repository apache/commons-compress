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
import java.util.Objects;

/**
 * Abstract superclass for reference constant pool entries, such as a method or field reference.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public abstract class CPRef extends ConstantPoolEntry {

    /**
     * The class name.
     */
    CPClass className;
    transient int classNameIndex;

    /**
     * The name and type descriptor.
     */
    protected CPNameAndType nameAndType;
    transient int nameAndTypeIndex;

    /**
     * Cached toString value.
     */
    protected String cachedToString;

    /**
     * Constructs a new CPRef.
     *
     * @param type        the constant pool entry type.
     * @param className   the class name.
     * @param descriptor  the name and type descriptor.
     * @param globalIndex index in CpBands
     * @throws NullPointerException if descriptor or className is null
     */
    public CPRef(final byte type, final CPClass className, final CPNameAndType descriptor, final int globalIndex) {
        super(type, globalIndex);
        this.className = Objects.requireNonNull(className, "className");
        this.nameAndType = Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() || hashCode() != obj.hashCode()) {
            return false;
        }
        final CPRef other = (CPRef) obj;
        return Objects.equals(className, other.className)
                && Objects.equals(nameAndType, other.nameAndType);
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final ClassFileEntry[] entries = new ClassFileEntry[2];
        entries[0] = className;
        entries[1] = nameAndType;
        return entries;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        nameAndTypeIndex = pool.indexOf(nameAndType);
        classNameIndex = pool.indexOf(className);
    }

    @Override
    public String toString() {
        if (cachedToString == null) {
            final String type;
            if (getTag() == CP_Fieldref) {
                type = "FieldRef"; //$NON-NLS-1$
            } else if (getTag() == CP_Methodref) {
                type = "MethoddRef"; //$NON-NLS-1$
            } else if (getTag() == CP_InterfaceMethodref) {
                type = "InterfaceMethodRef"; //$NON-NLS-1$
            } else {
                type = "unknown"; //$NON-NLS-1$
            }
            cachedToString = type + ": " + className + "#" + nameAndType; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return cachedToString;
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(classNameIndex);
        dos.writeShort(nameAndTypeIndex);
    }

}
