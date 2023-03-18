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
 * Parameter annotations class file attribute, either a RuntimeVisibleParameterAnnotations attribute or a
 * RuntimeInvisibleParameterAnnotations attribute.
 */
public class RuntimeVisibleorInvisibleParameterAnnotationsAttribute extends AnnotationsAttribute {

    /**
     * ParameterAnnotation represents the annotations on a single parameter.
     */
    public static class ParameterAnnotation {

        private final Annotation[] annotations;
        private final int numAnnotations;

        public ParameterAnnotation(final Annotation[] annotations) {
            this.numAnnotations = annotations.length;
            this.annotations = annotations;
        }

        public List<Object> getClassFileEntries() {
            final List<Object> nested = new ArrayList<>();
            for (final Annotation annotation : annotations) {
                nested.addAll(annotation.getClassFileEntries());
            }
            return nested;
        }

        public int getLength() {
            int length = 2;
            for (final Annotation annotation : annotations) {
                length += annotation.getLength();
            }
            return length;
        }

        public void resolve(final ClassConstantPool pool) {
            for (final Annotation annotation : annotations) {
                annotation.resolve(pool);
            }
        }

        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeShort(numAnnotations);
            for (final Annotation annotation : annotations) {
                annotation.writeBody(dos);
            }
        }

    }
    private final int numParameters;

    private final ParameterAnnotation[] parameterAnnotations;

    public RuntimeVisibleorInvisibleParameterAnnotationsAttribute(final CPUTF8 name,
        final ParameterAnnotation[] parameterAnnotations) {
        super(name);
        this.numParameters = parameterAnnotations.length;
        this.parameterAnnotations = parameterAnnotations;
    }

    @Override
    protected int getLength() {
        int length = 1;
        for (int i = 0; i < numParameters; i++) {
            length += parameterAnnotations[i].getLength();
        }
        return length;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<Object> nested = new ArrayList<>();
        nested.add(attributeName);
        for (final ParameterAnnotation parameterAnnotation : parameterAnnotations) {
            nested.addAll(parameterAnnotation.getClassFileEntries());
        }
        return nested.toArray(ClassFileEntry.NONE);
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        for (final ParameterAnnotation parameterAnnotation : parameterAnnotations) {
            parameterAnnotation.resolve(pool);
        }
    }

    @Override
    public String toString() {
        return attributeName.underlyingString() + ": " + numParameters + " parameter annotations";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeByte(numParameters);
        for (int i = 0; i < numParameters; i++) {
            parameterAnnotations[i].writeBody(dos);
        }
    }

}
