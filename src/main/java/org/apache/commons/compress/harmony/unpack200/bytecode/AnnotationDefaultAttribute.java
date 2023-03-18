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
import java.util.ArrayList;
import java.util.List;

/**
 * AnnotationDefault class file attribute
 */
public class AnnotationDefaultAttribute extends AnnotationsAttribute {

    private static CPUTF8 attributeName;

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    private final ElementValue elementValue;

    public AnnotationDefaultAttribute(final ElementValue elementValue) {
        super(attributeName);
        this.elementValue = elementValue;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    protected int getLength() {
        return elementValue.getLength();
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<Object> nested = new ArrayList<>();
        nested.add(attributeName);
        nested.addAll(elementValue.getClassFileEntries());
        final ClassFileEntry[] nestedEntries = new ClassFileEntry[nested.size()];
        for (int i = 0; i < nestedEntries.length; i++) {
            nestedEntries[i] = (ClassFileEntry) nested.get(i);
        }
        return nestedEntries;
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        elementValue.resolve(pool);
    }

    @Override
    public String toString() {
        return "AnnotationDefault: " + elementValue;
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        elementValue.writeBody(dos);
    }

}
