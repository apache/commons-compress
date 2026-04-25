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
 * Source file class file attribute
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class SourceFileAttribute extends Attribute {

    private static CPUTF8 attributeName;

    /**
     * Sets the attribute name.
     *
     * @param cpUTF8Value the attribute name.
     */
    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private final CPUTF8 name;

    private int nameIndex;

    /**
     * Constructs a new SourceFileAttribute.
     *
     * @param name the source file name.
     */
    public SourceFileAttribute(final CPUTF8 name) {
        super(attributeName);
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || this.getClass() != obj.getClass()) {
            return false;
        }
        final SourceFileAttribute other = (SourceFileAttribute) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    protected int getLength() {
        return 2;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] { getAttributeName(), name };
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#isSourceFileAttribute()
     */
    @Override
    public boolean isSourceFileAttribute() {
        return true;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        nameIndex = pool.indexOf(name);
    }

    @Override
    public String toString() {
        return "SourceFile: " + name;
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(nameIndex);
    }
}
