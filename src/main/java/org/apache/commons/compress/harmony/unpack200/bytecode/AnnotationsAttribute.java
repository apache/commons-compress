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

import org.apache.commons.compress.harmony.pack200.Pack200Exception;

/**
 * Abstracts Annotations attributes.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public abstract class AnnotationsAttribute extends Attribute {

    /**
     * Represents the annotation structure for class file attributes.
     */
    public static class Annotation {

        private final int numPairs;
        private final CPUTF8[] elementNames;
        private final ElementValue[] elementValues;
        private final CPUTF8 type;

        // Resolved values
        private int typeIndex;
        private int[] nameIndexes;

        /**
         * Constructs a new instance.
         *
         * @param numPairs      Number of pairs, matches the lengths of {@code elementNames} and {@code elementValues}.
         * @param type          Type.
         * @param elementNames  Element names.
         * @param elementValues Element values.
         */
        public Annotation(final int numPairs, final CPUTF8 type, final CPUTF8[] elementNames, final ElementValue[] elementValues) {
            this.numPairs = numPairs;
            this.type = type;
            this.elementNames = elementNames;
            this.elementValues = elementValues;
        }

        /**
         * Gets a list of class file entries.
         *
         * @return a list of class file entries.
         */
        public List<Object> getClassFileEntries() {
            final List<Object> entries = new ArrayList<>();
            for (int i = 0; i < elementNames.length; i++) {
                entries.add(elementNames[i]);
                entries.addAll(elementValues[i].getClassFileEntries());
            }
            entries.add(type);
            return entries;
        }

        /**
         * Gets the cumulative length of all element values.
         *
         * @return the cumulative length of all element values.
         */
        public int getLength() {
            int length = 4;
            for (int i = 0; i < numPairs; i++) {
                length += 2;
                length += elementValues[i].getLength();
            }
            return length;
        }

        /**
         * Resolves this instance against a given pool.
         *
         * @param pool a class constant pool.
         */
        public void resolve(final ClassConstantPool pool) {
            type.resolve(pool);
            typeIndex = pool.indexOf(type);
            nameIndexes = new int[numPairs];
            for (int i = 0; i < elementNames.length; i++) {
                elementNames[i].resolve(pool);
                nameIndexes[i] = pool.indexOf(elementNames[i]);
                elementValues[i].resolve(pool);
            }
        }

        /**
         * Writes this instance to the given output stream.
         *
         * @param dos the output stream.
         * @throws IOException if an I/O error occurs.
         */
        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeShort(typeIndex);
            dos.writeShort(numPairs);
            for (int i = 0; i < numPairs; i++) {
                dos.writeShort(nameIndexes[i]);
                elementValues[i].writeBody(dos);
            }
        }
    }

    /**
     * Pairs a tag and value.
     */
    public static class ElementValue {

        private final Object value;
        private final int tag;

        // resolved value index if it's a constant
        private int constantValueIndex = -1;

        /**
         * Constructs a new instance.
         *
         * @param tag a tag.
         * @param value a value.
         */
        public ElementValue(final int tag, final Object value) {
            this.tag = tag;
            this.value = value;
        }

        /**
         * Gets a list of class file entries.
         *
         * @return a list of class file entries.
         */
        public List<Object> getClassFileEntries() {
            final List<Object> entries = new ArrayList<>(1);
            if (value instanceof CPNameAndType) {
                // used to represent enum, so don't include the actual CPNameAndType
                entries.add(((CPNameAndType) value).name);
                entries.add(((CPNameAndType) value).descriptor);
            } else if (value instanceof ClassFileEntry) {
                // TODO? ClassFileEntry is an Object
                entries.add(value);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] values = (ElementValue[]) value;
                for (final ElementValue value2 : values) {
                    entries.addAll(value2.getClassFileEntries());
                }
            } else if (value instanceof Annotation) {
                entries.addAll(((Annotation) value).getClassFileEntries());
            }
            return entries;
        }

        /**
         * Gets the length.
         *
         * @return the length.
         */
        public int getLength() {
            switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'c':
            case 's':
                return 3;
            case 'e':
                return 5;
            case '[':
                int length = 3;
                final ElementValue[] nestedValues = (ElementValue[]) value;
                for (final ElementValue nestedValue : nestedValues) {
                    length += nestedValue.getLength();
                }
                return length;
            case '@':
                return 1 + ((Annotation) value).getLength();
            }
            return 0;
        }

        /**
         * Resolves this instance against a given pool.
         *
         * @param pool a class constant pool.
         */
        public void resolve(final ClassConstantPool pool) {
            if (value instanceof CPConstant) {
                ((CPConstant) value).resolve(pool);
                constantValueIndex = pool.indexOf((CPConstant) value);
            } else if (value instanceof CPClass) {
                ((CPClass) value).resolve(pool);
                constantValueIndex = pool.indexOf((CPClass) value);
            } else if (value instanceof CPUTF8) {
                ((CPUTF8) value).resolve(pool);
                constantValueIndex = pool.indexOf((CPUTF8) value);
            } else if (value instanceof CPNameAndType) {
                ((CPNameAndType) value).resolve(pool);
            } else if (value instanceof Annotation) {
                ((Annotation) value).resolve(pool);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] nestedValues = (ElementValue[]) value;
                for (final ElementValue nestedValue : nestedValues) {
                    nestedValue.resolve(pool);
                }
            }
        }

        /**
         * Writes this instance to the given output stream.
         *
         * @param dos the output stream.
         * @throws IOException if an I/O error occurs.
         */
        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeByte(tag);
            if (constantValueIndex != -1) {
                dos.writeShort(constantValueIndex);
            } else if (value instanceof CPNameAndType) {
                ((CPNameAndType) value).writeBody(dos);
            } else if (value instanceof Annotation) {
                ((Annotation) value).writeBody(dos);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] nestedValues = (ElementValue[]) value;
                dos.writeShort(nestedValues.length);
                for (final ElementValue nestedValue : nestedValues) {
                    nestedValue.writeBody(dos);
                }
            } else {
                throw new Pack200Exception("Value is not a CPNameAndType, Annotation, or ElementValue.");
            }
        }
    }

    /**
     * Constructs a new instance for an attribute name.
     *
     * @param attributeName an attribute name.
     */
    public AnnotationsAttribute(final CPUTF8 attributeName) {
        super(attributeName);
    }

}
