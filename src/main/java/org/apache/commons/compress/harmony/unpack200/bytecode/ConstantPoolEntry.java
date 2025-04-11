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

/**
 * Abstracts constant pool entries.
 */
public abstract class ConstantPoolEntry extends ClassFileEntry {

    /**
     * The constant {@value} for a constant pool class.
     */
    public static final byte CP_Class = 7;

    /**
     * The constant {@value} for a constant pool double.
     */
    public static final byte CP_Double = 6;

    /**
     * The constant {@value} for a constant pool field reference.
     */
    public static final byte CP_Fieldref = 9;

    /**
     * The constant {@value} for a constant pool float.
     */
    public static final byte CP_Float = 4;

    /**
     * The constant {@value} for a constant pool int.
     */
    public static final byte CP_Integer = 3;

    /*
     * class MemberRef extends ConstantPoolEntry { private int index; Class(String name) { super(CP_Class); index = pool.indexOf(name); } void
     * writeBody(DataOutputStream dos) throws IOException { dos.writeShort(index); } }
     */

    /**
     * The constant {@value} for a constant pool interface method reference.
     */
    public static final byte CP_InterfaceMethodref = 11;

    /**
     * The constant {@value} for a constant pool long.
     */
    public static final byte CP_Long = 5;

    /**
     * The constant {@value} for a constant pool method reference.
     */
    public static final byte CP_Methodref = 10;

    /**
     * The constant {@value} for a constant pool name and type.
     */
    public static final byte CP_NameAndType = 12;

    /**
     * The constant {@value} for a constant pool string.
     */
    public static final byte CP_String = 8;

    /**
     * The constant {@value} for a constant pool UTF8.
     */
    public static final byte CP_UTF8 = 1;

    byte tag;

    /**
     * Global index.
     */
    protected int globalIndex;

    ConstantPoolEntry(final byte tag, final int globalIndex) {
        this.tag = tag;
        this.globalIndex = globalIndex;
    }

    @Override
    public void doWrite(final DataOutputStream dos) throws IOException {
        dos.writeByte(tag);
        writeBody(dos);
    }

    @Override
    public abstract boolean equals(Object obj);

    /**
     * Gets the global index.
     *
     * @return the global index.
     */
    public int getGlobalIndex() {
        return globalIndex;
    }

    /**
     * Gets the tag.

     * @return the tag.
     */
    public byte getTag() {
        return tag;
    }

    @Override
    public abstract int hashCode();

    /**
     * Writes this instance to the given output stream.
     *
     * @param dos the output stream.
     * @throws IOException if an I/O error occurs.
     */
    protected abstract void writeBody(DataOutputStream dos) throws IOException;
}
