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

/**
 * Field reference constant pool entry.
 */
public class CPFieldRef extends ConstantPoolEntry {

    CPClass className;
    transient int classNameIndex;
    private final CPNameAndType nameAndType;
    transient int nameAndTypeIndex;

    public CPFieldRef(final CPClass className, final CPNameAndType descriptor, final int globalIndex) {
        super(ConstantPoolEntry.CP_Fieldref, globalIndex);
        this.className = className;
        this.nameAndType = descriptor;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] {className, nameAndType};
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        nameAndTypeIndex = pool.indexOf(nameAndType);
        classNameIndex = pool.indexOf(className);
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(classNameIndex);
        dos.writeShort(nameAndTypeIndex);
    }

    @Override
    public String toString() {
        return "FieldRef: " + className + "#" + nameAndType;
    }

    private boolean hashcodeComputed;
    private int cachedHashCode;

    private void generateHashCode() {
        hashcodeComputed = true;
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((className == null) ? 0 : className.hashCode());
        result = PRIME * result + ((nameAndType == null) ? 0 : nameAndType.hashCode());
        cachedHashCode = result;
    }

    @Override
    public int hashCode() {
        if (!hashcodeComputed) {
            generateHashCode();
        }
        return cachedHashCode;
    }

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
        final CPFieldRef other = (CPFieldRef) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (nameAndType == null) {
            if (other.nameAndType != null) {
                return false;
            }
        } else if (!nameAndType.equals(other.nameAndType)) {
            return false;
        }
        return true;
    }

}