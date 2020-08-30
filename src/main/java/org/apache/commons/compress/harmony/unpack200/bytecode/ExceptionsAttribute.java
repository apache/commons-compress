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
import java.util.Arrays;

/**
 * Exceptions class file attribute
 */
public class ExceptionsAttribute extends Attribute {

    private static CPUTF8 attributeName;

    private static int hashCode(Object[] array) {
        final int prime = 31;
        if (array == null)
            return 0;
        int result = 1;
        for (int index = 0; index < array.length; index++) {
            result = prime * result
                    + (array[index] == null ? 0 : array[index].hashCode());
        }
        return result;
    }

    private transient int[] exceptionIndexes;

    private final CPClass[] exceptions;

    public ExceptionsAttribute(CPClass[] exceptions) {
        super(attributeName);
        this.exceptions = exceptions;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ExceptionsAttribute other = (ExceptionsAttribute) obj;
        if (!Arrays.equals(exceptions, other.exceptions))
            return false;
        return true;
    }

    protected int getLength() {
        return 2 + 2 * exceptions.length;
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        ClassFileEntry[] result = new ClassFileEntry[exceptions.length + 1];
        for (int i = 0; i < exceptions.length; i++) {
            result[i] = exceptions[i];
        }
        result[exceptions.length] = getAttributeName();
        return result;
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ExceptionsAttribute.hashCode(exceptions);
        return result;
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        exceptionIndexes = new int[exceptions.length];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i].resolve(pool);
            exceptionIndexes[i] = pool.indexOf(exceptions[i]);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Exceptions: ");
        for (int i = 0; i < exceptions.length; i++) {
            sb.append(exceptions[i]);
            sb.append(' ');
        }
        return sb.toString();
    }

    protected void writeBody(DataOutputStream dos) throws IOException {
        dos.writeShort(exceptionIndexes.length);
        for (int i = 0; i < exceptionIndexes.length; i++) {
            dos.writeShort(exceptionIndexes[i]);
        }
    }

    public static void setAttributeName(CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

}