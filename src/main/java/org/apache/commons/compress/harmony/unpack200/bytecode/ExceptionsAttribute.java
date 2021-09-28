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

    private static int hashCode(final Object[] array) {
        final int prime = 31;
        if (array == null) {
            return 0;
        }
        int result = 1;
        for (Object element : array) {
            result = prime * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    private transient int[] exceptionIndexes;

    private final CPClass[] exceptions;

    public ExceptionsAttribute(final CPClass[] exceptions) {
        super(attributeName);
        this.exceptions = exceptions;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ExceptionsAttribute other = (ExceptionsAttribute) obj;
        if (!Arrays.equals(exceptions, other.exceptions)) {
            return false;
        }
        return true;
    }

    @Override
    protected int getLength() {
        return 2 + 2 * exceptions.length;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final ClassFileEntry[] result = new ClassFileEntry[exceptions.length + 1];
        for (int i = 0; i < exceptions.length; i++) {
            result[i] = exceptions[i];
        }
        result[exceptions.length] = getAttributeName();
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ExceptionsAttribute.hashCode(exceptions);
        return result;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        exceptionIndexes = new int[exceptions.length];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i].resolve(pool);
            exceptionIndexes[i] = pool.indexOf(exceptions[i]);
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Exceptions: ");
        for (CPClass exception : exceptions) {
            sb.append(exception);
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(exceptionIndexes.length);
        for (int element : exceptionIndexes) {
            dos.writeShort(element);
        }
    }

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

}