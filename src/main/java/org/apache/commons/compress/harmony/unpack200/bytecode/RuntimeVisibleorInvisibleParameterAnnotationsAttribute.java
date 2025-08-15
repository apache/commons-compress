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
import java.util.ArrayList;
import java.util.List;

/**
 * Parameter annotations class file attribute, either a RuntimeVisibleParameterAnnotations attribute or a RuntimeInvisibleParameterAnnotations attribute.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class RuntimeVisibleorInvisibleParameterAnnotationsAttribute extends AnnotationsAttribute {

    /**
     * ParameterAnnotation represents the annotations on a single parameter.
     */
    public static class ParameterAnnotation {

        private final Annotation[] annotations;

        /**
         * Constructs a new instance.
         *
         * @param annotations Annotation.
         */
        public ParameterAnnotation(final Annotation[] annotations) {
            this.annotations = annotations;
        }

        /**
         * Gets all annotation class file entries.
         *
         * @return all annotation class file entries.
         */
        public List<Object> getClassFileEntries() {
            final List<Object> nested = new ArrayList<>();
            for (final Annotation annotation : annotations) {
                nested.addAll(annotation.getClassFileEntries());
            }
            return nested;
        }

        /**
         * Gets the cumulative length of all annotations.
         *
         * @return the cumulative length of all annotations.
         */
        public int getLength() {
            int length = 2;
            for (final Annotation annotation : annotations) {
                length += annotation.getLength();
            }
            return length;
        }

        /**
         * Resolves all annotations in this instance against the given pool.
         *
         * @param pool A class constant pool.
         */
        public void resolve(final ClassConstantPool pool) {
            for (final Annotation annotation : annotations) {
                annotation.resolve(pool);
            }
        }

        /**
         * Writes this body to the given output stream.
         *
         * @param dos the output stream.
         * @throws IOException if an I/O error occurs.
         */
        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeShort(annotations.length);
            for (final Annotation annotation : annotations) {
                annotation.writeBody(dos);
            }
        }

    }

    private final ParameterAnnotation[] parameterAnnotations;

    /**
     * Constructs a new instance for an attribute name.
     *
     * @param name an attribute name.
     * @param parameterAnnotations Annotations.
     */
    public RuntimeVisibleorInvisibleParameterAnnotationsAttribute(final CPUTF8 name, final ParameterAnnotation[] parameterAnnotations) {
        super(name);
        this.parameterAnnotations = parameterAnnotations;
    }

    @Override
    protected int getLength() {
        int length = 1;
        for (final ParameterAnnotation parameterAnnotation : parameterAnnotations) {
            length += parameterAnnotation.getLength();
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
        return nested.toArray(NONE);
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
        return attributeName.underlyingString() + ": " + parameterAnnotations.length + " parameter annotations";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeByte(parameterAnnotations.length);
        for (final ParameterAnnotation parameterAnnotation : parameterAnnotations) {
            parameterAnnotation.writeBody(dos);
        }
    }

}
