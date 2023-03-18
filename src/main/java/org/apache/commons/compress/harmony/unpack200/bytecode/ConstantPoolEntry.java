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
 * Abstract superclass for constant pool entries
 */
public abstract class ConstantPoolEntry extends ClassFileEntry {

    public static final byte CP_Class = 7;

    public static final byte CP_Double = 6;

    public static final byte CP_Fieldref = 9;

    public static final byte CP_Float = 4;

    public static final byte CP_Integer = 3;

    /*
     * class MemberRef extends ConstantPoolEntry { private int index; Class(String name) { super(CP_Class); index =
     * pool.indexOf(name); } void writeBody(DataOutputStream dos) throws IOException { dos.writeShort(index); } }
     */

    public static final byte CP_InterfaceMethodref = 11;

    public static final byte CP_Long = 5;

    public static final byte CP_Methodref = 10;

    public static final byte CP_NameAndType = 12;

    public static final byte CP_String = 8;

    public static final byte CP_UTF8 = 1;

    byte tag;

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

    public int getGlobalIndex() {
        return globalIndex;
    }

    public byte getTag() {
        return tag;
    }

    @Override
    public abstract int hashCode();

    protected abstract void writeBody(DataOutputStream dos) throws IOException;
}
