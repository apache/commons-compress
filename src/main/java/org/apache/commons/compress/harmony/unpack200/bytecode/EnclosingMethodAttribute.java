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
 * Enclosing method class file attribute.
 */
public class EnclosingMethodAttribute extends Attribute {

    private int class_index;
    private int method_index;
    private final CPClass cpClass;
    private final CPNameAndType method;
    private static CPUTF8 attributeName;

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    public EnclosingMethodAttribute(final CPClass cpClass, final CPNameAndType method) {
        super(attributeName);
        this.cpClass = cpClass;
        this.method = method;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        if (method != null) {
            return new ClassFileEntry[] {attributeName, cpClass, method};
        }
        return new ClassFileEntry[] {attributeName, cpClass};
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#getLength()
     */
    @Override
    protected int getLength() {
        return 4;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        cpClass.resolve(pool);
        class_index = pool.indexOf(cpClass);
        if (method != null) {
            method.resolve(pool);
            method_index = pool.indexOf(method);
        } else {
            method_index = 0;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#writeBody(java.io.DataOutputStream)
     */
    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(class_index);
        dos.writeShort(method_index);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry#toString()
     */
    @Override
    public String toString() {
        return "EnclosingMethod";
    }

}
