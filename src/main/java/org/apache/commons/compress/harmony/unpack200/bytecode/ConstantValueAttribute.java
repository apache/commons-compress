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
 * An {@link Attribute} representing a constant.
 */
public class ConstantValueAttribute extends Attribute {

    private static CPUTF8 attributeName;

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private int constantIndex;

    private final ClassFileEntry entry;

    public ConstantValueAttribute(final ClassFileEntry entry) {
        super(attributeName);
        this.entry = Objects.requireNonNull(entry, "entry");
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
        final ConstantValueAttribute other = (ConstantValueAttribute) obj;
        if (!Objects.equals(entry, other.entry)) {
            return false;
        }
        return true;
    }

    @Override
    protected int getLength() {
        return 2;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] {getAttributeName(), entry};
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + ((entry == null) ? 0 : entry.hashCode());
        return result;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        entry.resolve(pool);
        this.constantIndex = pool.indexOf(entry);
    }

    @Override
    public String toString() {
        return "Constant:" + entry;
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(constantIndex);
    }

}