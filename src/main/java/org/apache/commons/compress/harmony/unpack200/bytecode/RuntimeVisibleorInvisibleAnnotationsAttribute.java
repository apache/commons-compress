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
 * Annotations class file attribute, either a RuntimeVisibleAnnotations attribute or a RuntimeInvisibleAnnotations
 * attribute.
 */
public class RuntimeVisibleorInvisibleAnnotationsAttribute extends AnnotationsAttribute {

    private final int numAnnotations;
    private final Annotation[] annotations;

    public RuntimeVisibleorInvisibleAnnotationsAttribute(final CPUTF8 name, final Annotation[] annotations) {
        super(name);
        this.numAnnotations = annotations.length;
        this.annotations = annotations;
    }

    @Override
    protected int getLength() {
        int length = 2;
        for (int i = 0; i < numAnnotations; i++) {
            length += annotations[i].getLength();
        }
        return length;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<Object> nested = new ArrayList<>();
        nested.add(attributeName);
        for (final Annotation annotation : annotations) {
            nested.addAll(annotation.getClassFileEntries());
        }
        return nested.toArray(ClassFileEntry.NONE);
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        for (final Annotation annotation : annotations) {
            annotation.resolve(pool);
        }
    }

    @Override
    public String toString() {
        return attributeName.underlyingString() + ": " + numAnnotations + " annotations";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        final int size = dos.size();
        dos.writeShort(numAnnotations);
        for (int i = 0; i < numAnnotations; i++) {
            annotations[i].writeBody(dos);
        }
        if (dos.size() - size != getLength()) {
            throw new Error();
        }
    }
}
