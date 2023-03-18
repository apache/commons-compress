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
 * ClassFile is used to represent and write out Java class files.
 */
public class ClassFile {

    private static final int MAGIC = 0xCAFEBABE;

    public int major;
    public int minor;
    public ClassConstantPool pool = new ClassConstantPool();
    public int accessFlags;
    public int thisClass;
    public int superClass;
    public int[] interfaces;
    public ClassFileEntry[] fields;
    public ClassFileEntry[] methods;
    public Attribute[] attributes;

    public void write(final DataOutputStream dos) throws IOException {
        dos.writeInt(MAGIC);
        dos.writeShort(minor);
        dos.writeShort(major);
        dos.writeShort(pool.size() + 1);
        for (int i = 1; i <= pool.size(); i++) {
            ConstantPoolEntry entry;
            (entry = (ConstantPoolEntry) pool.get(i)).doWrite(dos);
            // Doubles and longs take up two spaces in the pool, but only one
            // gets written
            if (entry.getTag() == ConstantPoolEntry.CP_Double || entry.getTag() == ConstantPoolEntry.CP_Long) {
                i++;
            }
        }
        dos.writeShort(accessFlags);
        dos.writeShort(thisClass);
        dos.writeShort(superClass);
        dos.writeShort(interfaces.length);
        for (final int element : interfaces) {
            dos.writeShort(element);
        }
        dos.writeShort(fields.length);
        for (final ClassFileEntry field : fields) {
            field.write(dos);
        }
        dos.writeShort(methods.length);
        for (final ClassFileEntry method : methods) {
            method.write(dos);
        }
        dos.writeShort(attributes.length);
        for (final Attribute attribute : attributes) {
            attribute.write(dos);
        }
    }
}
