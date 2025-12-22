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
 * ClassFile is used to represent and write out Java class files.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class ClassFile {

    private static final int MAGIC = 0xCAFEBABE;

    /**
     * The major version number.
     */
    public int major;

    /**
     * The minor version number.
     */
    public int minor;

    /**
     * The constant pool.
     */
    public ClassConstantPool pool = new ClassConstantPool();

    /**
     * The access flags.
     */
    public int accessFlags;

    /**
     * The index of this class in the constant pool.
     */
    public int thisClass;

    /**
     * The index of the super class in the constant pool.
     */
    public int superClass;

    /**
     * The interfaces.
     */
    public int[] interfaces;

    /**
     * The fields.
     */
    public ClassFileEntry[] fields;

    /**
     * The methods.
     */
    public ClassFileEntry[] methods;

    /**
     * The attributes.
     */
    public Attribute[] attributes;

    /**
     * Writes this class file to the specified output stream.
     *
     * @param dos the data output stream.
     * @throws IOException if an I/O error occurs.
     */
    public void write(final DataOutputStream dos) throws IOException {
        dos.writeInt(MAGIC);
        dos.writeShort(minor);
        dos.writeShort(major);
        dos.writeShort(pool.size() + 1);
        for (int i = 1; i <= pool.size(); i++) {
            final ConstantPoolEntry entry;
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
