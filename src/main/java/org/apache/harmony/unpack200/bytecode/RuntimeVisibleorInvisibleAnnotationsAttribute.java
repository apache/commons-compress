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
package org.apache.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Annotations class file attribute, either a RuntimeVisibleAnnotations
 * attribute or a RuntimeInvisibleAnnotations attribute.
 */
public class RuntimeVisibleorInvisibleAnnotationsAttribute extends
        AnnotationsAttribute {

    private final int num_annotations;
    private final Annotation[] annotations;

    public RuntimeVisibleorInvisibleAnnotationsAttribute(CPUTF8 name,
            Annotation[] annotations) {
        super(name);
        this.num_annotations = annotations.length;
        this.annotations = annotations;
    }

    protected int getLength() {
        int length = 2;
        for (int i = 0; i < num_annotations; i++) {
            length += annotations[i].getLength();
        }
        return length;
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        for (int i = 0; i < annotations.length; i++) {
            annotations[i].resolve(pool);
        }
    }

    protected void writeBody(DataOutputStream dos) throws IOException {
        int size = dos.size();
        dos.writeShort(num_annotations);
        for (int i = 0; i < num_annotations; i++) {
            annotations[i].writeBody(dos);
        }
        if (dos.size() - size != getLength()) {
            throw new Error();
        }
    }

    public String toString() {
        return attributeName.underlyingString() + ": " + num_annotations
                + " annotations";
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
        List nested = new ArrayList();
        nested.add(attributeName);
        for (int i = 0; i < annotations.length; i++) {
            nested.addAll(annotations[i].getClassFileEntries());
        }
        ClassFileEntry[] nestedEntries = new ClassFileEntry[nested.size()];
        for (int i = 0; i < nestedEntries.length; i++) {
            nestedEntries[i] = (ClassFileEntry) nested.get(i);
        }
        return nestedEntries;
    }
}
