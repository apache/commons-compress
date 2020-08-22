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
import java.util.ArrayList;
import java.util.List;

/**
 * Inner classes class file attribute
 */
public class InnerClassesAttribute extends Attribute {

    private static CPUTF8 attributeName;

    public static void setAttributeName(CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private static class InnerClassesEntry {

        CPClass inner_class_info;
        CPClass outer_class_info;
        CPUTF8 inner_class_name;

        int inner_class_info_index = -1;
        int outer_class_info_index = -1;
        int inner_name_index = -1;
        int inner_class_access_flags = -1;

        public InnerClassesEntry(CPClass innerClass, CPClass outerClass,
                CPUTF8 innerName, int flags) {
            this.inner_class_info = innerClass;
            this.outer_class_info = outerClass;
            this.inner_class_name = innerName;
            this.inner_class_access_flags = flags;
        }

        /**
         * Determine the indices of the things in the receiver which point to
         * elements of the ClassConstantPool
         *
         * @param pool
         *            ClassConstantPool which holds the CPClass and CPUTF8
         *            objects.
         */
        public void resolve(ClassConstantPool pool) {
            if (inner_class_info != null) {
                inner_class_info.resolve(pool);
                inner_class_info_index = pool.indexOf(inner_class_info);
            } else {
                inner_class_info_index = 0;
            }

            if (inner_class_name != null) {
                inner_class_name.resolve(pool);
                inner_name_index = pool.indexOf(inner_class_name);
            } else {
                inner_name_index = 0;
            }

            if (outer_class_info != null) {
                outer_class_info.resolve(pool);
                outer_class_info_index = pool.indexOf(outer_class_info);
            } else {
                outer_class_info_index = 0;
            }
        }

        public void write(DataOutputStream dos) throws IOException {
            dos.writeShort(inner_class_info_index);
            dos.writeShort(outer_class_info_index);
            dos.writeShort(inner_name_index);
            dos.writeShort(inner_class_access_flags);
        }

    }

    private final List innerClasses = new ArrayList();
    private final List nestedClassFileEntries = new ArrayList();

    public InnerClassesAttribute(String name) {
        super(attributeName);
        nestedClassFileEntries.add(getAttributeName());
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (this.getClass() != obj.getClass())
            return false;
        final InnerClassesAttribute other = (InnerClassesAttribute) obj;
        if (getAttributeName() == null) {
            if (other.getAttributeName() != null)
                return false;
        } else if (!getAttributeName().equals(other.getAttributeName()))
            return false;
        return true;
    }

    protected int getLength() {
        return 2 + ((2 + 2 + 2 + 2) * innerClasses.size());
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        ClassFileEntry[] result = new ClassFileEntry[nestedClassFileEntries
                .size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = (ClassFileEntry) nestedClassFileEntries.get(index);
        }
        return result;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME
                * result
                + ((getAttributeName() == null) ? 0 : getAttributeName()
                        .hashCode());
        return result;
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        for(int it = 0; it < innerClasses.size(); it++) {
            InnerClassesEntry entry = (InnerClassesEntry) innerClasses.get(it);
            entry.resolve(pool);
        }
    }

    public String toString() {
        return "InnerClasses: " + getAttributeName();
    }

    protected void doWrite(DataOutputStream dos) throws IOException {
        // Hack so I can see what's being written.
        super.doWrite(dos);
    }

    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(innerClasses.size());

        for(int it = 0; it < innerClasses.size(); it++) {
            InnerClassesEntry entry = (InnerClassesEntry) innerClasses.get(it);
            entry.write(dos);
        }
    }

    public void addInnerClassesEntry(CPClass innerClass, CPClass outerClass,
            CPUTF8 innerName, int flags) {
        if (innerClass != null) {
            nestedClassFileEntries.add(innerClass);
        }
        if (outerClass != null) {
            nestedClassFileEntries.add(outerClass);
        }
        if (innerName != null) {
            nestedClassFileEntries.add(innerName);
        }
        addInnerClassesEntry(new InnerClassesEntry(innerClass, outerClass,
                innerName, flags));
    }

    private void addInnerClassesEntry(InnerClassesEntry innerClassesEntry) {
        innerClasses.add(innerClassesEntry);
    }
}
