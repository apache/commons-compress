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

/**
 * Constant pool entry for a class
 */
public class CPClass extends ConstantPoolEntry {

    private int index;

    public String name;

    private final CPUTF8 utf8;

    private boolean hashCodeComputed;

    private int cachedHashCode;

    /**
     * Creates a new CPClass
     *
     * @param name TODO
     * @param globalIndex index in CpBands
     * @throws NullPointerException if name is null
     */
    public CPClass(final CPUTF8 name, final int globalIndex) {
        super(ConstantPoolEntry.CP_Class, globalIndex);
        this.name = Objects.requireNonNull(name, "name").underlyingString();
        this.utf8 = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final CPClass other = (CPClass) obj;
        return utf8.equals(other.utf8);
    }
    private void generateHashCode() {
        hashCodeComputed = true;
        cachedHashCode = utf8.hashCode();
    }

    public String getName() {
        return name;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] {utf8,};
    }

    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            generateHashCode();
        }
        return cachedHashCode;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        index = pool.indexOf(utf8);
    }

    @Override
    public String toString() {
        return "Class: " + getName();
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(index);
    }
}