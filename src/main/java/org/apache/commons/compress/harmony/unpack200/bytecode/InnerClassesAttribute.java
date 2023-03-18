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

/**
 * Inner classes class file attribute
 */
public class InnerClassesAttribute extends Attribute {

    private static class InnerClassesEntry {

        CPClass innerClassInfo;
        CPClass outerClassInfo;
        CPUTF8 innerClassName;

        int innerClassInfoIndex = -1;
        int outerClassInfoIndex = -1;
        int innerNameIndex = -1;
        int innerClassAccessFlags = -1;

        public InnerClassesEntry(final CPClass innerClass, final CPClass outerClass, final CPUTF8 innerName,
            final int flags) {
            this.innerClassInfo = innerClass;
            this.outerClassInfo = outerClass;
            this.innerClassName = innerName;
            this.innerClassAccessFlags = flags;
        }

        /**
         * Determine the indices of the things in the receiver which point to elements of the ClassConstantPool
         *
         * @param pool ClassConstantPool which holds the CPClass and CPUTF8 objects.
         */
        public void resolve(final ClassConstantPool pool) {
            if (innerClassInfo != null) {
                innerClassInfo.resolve(pool);
                innerClassInfoIndex = pool.indexOf(innerClassInfo);
            } else {
                innerClassInfoIndex = 0;
            }

            if (innerClassName != null) {
                innerClassName.resolve(pool);
                innerNameIndex = pool.indexOf(innerClassName);
            } else {
                innerNameIndex = 0;
            }

            if (outerClassInfo != null) {
                outerClassInfo.resolve(pool);
                outerClassInfoIndex = pool.indexOf(outerClassInfo);
            } else {
                outerClassInfoIndex = 0;
            }
        }

        public void write(final DataOutputStream dos) throws IOException {
            dos.writeShort(innerClassInfoIndex);
            dos.writeShort(outerClassInfoIndex);
            dos.writeShort(innerNameIndex);
            dos.writeShort(innerClassAccessFlags);
        }

    }

    private static CPUTF8 attributeName;

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private final List<InnerClassesEntry> innerClasses = new ArrayList<>();
    private final List<ConstantPoolEntry> nestedClassFileEntries = new ArrayList<>();

    public InnerClassesAttribute(final String name) {
        super(attributeName);
        nestedClassFileEntries.add(getAttributeName());
    }

    public void addInnerClassesEntry(final CPClass innerClass, final CPClass outerClass, final CPUTF8 innerName,
        final int flags) {
        if (innerClass != null) {
            nestedClassFileEntries.add(innerClass);
        }
        if (outerClass != null) {
            nestedClassFileEntries.add(outerClass);
        }
        if (innerName != null) {
            nestedClassFileEntries.add(innerName);
        }
        addInnerClassesEntry(new InnerClassesEntry(innerClass, outerClass, innerName, flags));
    }

    private void addInnerClassesEntry(final InnerClassesEntry innerClassesEntry) {
        innerClasses.add(innerClassesEntry);
    }

    @Override
    protected void doWrite(final DataOutputStream dos) throws IOException {
        // Hack so I can see what's being written.
        super.doWrite(dos);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final InnerClassesAttribute other = (InnerClassesAttribute) obj;
        if (getAttributeName() == null) {
            if (other.getAttributeName() != null) {
                return false;
            }
        } else if (!getAttributeName().equals(other.getAttributeName())) {
            return false;
        }
        return true;
    }

    @Override
    protected int getLength() {
        return 2 + ((2 + 2 + 2 + 2) * innerClasses.size());
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return nestedClassFileEntries.toArray(ClassFileEntry.NONE);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + ((getAttributeName() == null) ? 0 : getAttributeName().hashCode());
        return result;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        for (final InnerClassesEntry entry : innerClasses) {
            entry.resolve(pool);
        }
    }

    @Override
    public String toString() {
        return "InnerClasses: " + getAttributeName();
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(innerClasses.size());

        for (final InnerClassesEntry entry : innerClasses) {
            entry.write(dos);
        }
    }
}
