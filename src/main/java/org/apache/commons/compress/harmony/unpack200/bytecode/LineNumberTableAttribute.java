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
 * Line number table
 */
public class LineNumberTableAttribute extends BCIRenumberedAttribute {

    private static CPUTF8 attributeName;
    
    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private final int lineNumberTableLength;
    private final int[] startPcs;
    private final int[] lineNumbers;

    public LineNumberTableAttribute(final int lineNumberTableLength, final int[] startPcs,
        final int[] lineNumbers) {
        super(attributeName);
        this.lineNumberTableLength = lineNumberTableLength;
        this.startPcs = startPcs;
        this.lineNumbers = lineNumbers;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    protected int getLength() {
        return 2 + (4 * lineNumberTableLength);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#getNestedClassFileEntries()
     */
    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        return new ClassFileEntry[] {getAttributeName()};
    }

    @Override
    protected int[] getStartPCs() {
        return startPcs;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#resolve(org.apache.commons.compress.harmony.
     * unpack200.bytecode.ClassConstantPool)
     */
    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry#toString()
     */
    @Override
    public String toString() {
        return "LineNumberTable: " + lineNumberTableLength + " lines";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(lineNumberTableLength);
        for (int i = 0; i < lineNumberTableLength; i++) {
            dos.writeShort(startPcs[i]);
            dos.writeShort(lineNumbers[i]);
        }
    }
}
