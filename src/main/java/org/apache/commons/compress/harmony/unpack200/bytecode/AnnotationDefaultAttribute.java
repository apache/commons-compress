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

    private final ElementValue element_value;

    private static CPUTF8 attributeName;

    public static void setAttributeName(final CPUTF8 cpUTF8Value) {
        attributeName = cpUTF8Value;
    }

    public AnnotationDefaultAttribute(final ElementValue element_value) {
        super(attributeName);
        this.element_value = element_value;
    }

    @Override
    protected int getLength() {
        return element_value.getLength();
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        element_value.writeBody(dos);
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        element_value.resolve(pool);
    }

    @Override
    public String toString() {
        return "AnnotationDefault: " + element_value;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List nested = new ArrayList();
        nested.add(attributeName);
        nested.addAll(element_value.getClassFileEntries());
        final ClassFileEntry[] nestedEntries = new ClassFileEntry[nested.size()];
        for (int i = 0; i < nestedEntries.length; i++) {
            nestedEntries[i] = (ClassFileEntry) nested.get(i);
        }
        return nestedEntries;
    }

}
