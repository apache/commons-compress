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
 * Abstract superclass for Annotations attributes
 */
public abstract class AnnotationsAttribute extends Attribute {

    /**
     * Class to represent the annotation structure for class file attributes
     */
    public static class Annotation {

        private final int num_pairs;
        private final CPUTF8[] element_names;
        private final ElementValue[] element_values;
        private final CPUTF8 type;

        // Resolved values
        private int type_index;
        private int[] name_indexes;

        public Annotation(final int num_pairs, final CPUTF8 type, final CPUTF8[] element_names,
            final ElementValue[] element_values) {
            this.num_pairs = num_pairs;
            this.type = type;
            this.element_names = element_names;
            this.element_values = element_values;
        }

        public int getLength() {
            int length = 4;
            for (int i = 0; i < num_pairs; i++) {
                length += 2;
                length += element_values[i].getLength();
            }
            return length;
        }

        public void resolve(final ClassConstantPool pool) {
            type.resolve(pool);
            type_index = pool.indexOf(type);
            name_indexes = new int[num_pairs];
            for (int i = 0; i < element_names.length; i++) {
                element_names[i].resolve(pool);
                name_indexes[i] = pool.indexOf(element_names[i]);
                element_values[i].resolve(pool);
            }
        }

        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeShort(type_index);
            dos.writeShort(num_pairs);
            for (int i = 0; i < num_pairs; i++) {
                dos.writeShort(name_indexes[i]);
                element_values[i].writeBody(dos);
            }
        }

        public List getClassFileEntries() {
            final List entries = new ArrayList();
            for (int i = 0; i < element_names.length; i++) {
                entries.add(element_names[i]);
                entries.addAll(element_values[i].getClassFileEntries());
            }
            entries.add(type);
            return entries;
        }
    }

    public static class ElementValue {

        private final Object value;
        private final int tag;

        // resolved value index if it's a constant
        private int constant_value_index = -1;

        public ElementValue(final int tag, final Object value) {
            this.tag = tag;
            this.value = value;
        }

        public List getClassFileEntries() {
            final List entries = new ArrayList(1);
            if (value instanceof CPNameAndType) {
                // used to represent enum, so don't include the actual CPNameAndType
                entries.add(((CPNameAndType) value).name);
                entries.add(((CPNameAndType) value).descriptor);
            } else if (value instanceof ClassFileEntry) {
                entries.add(value);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] values = (ElementValue[]) value;
                for (ElementValue value2 : values) {
                    entries.addAll(value2.getClassFileEntries());
                }
            } else if (value instanceof Annotation) {
                entries.addAll(((Annotation) value).getClassFileEntries());
            }
            return entries;
        }

        public void resolve(final ClassConstantPool pool) {
            if (value instanceof CPConstant) {
                ((CPConstant) value).resolve(pool);
                constant_value_index = pool.indexOf((CPConstant) value);
            } else if (value instanceof CPClass) {
                ((CPClass) value).resolve(pool);
                constant_value_index = pool.indexOf((CPClass) value);
            } else if (value instanceof CPUTF8) {
                ((CPUTF8) value).resolve(pool);
                constant_value_index = pool.indexOf((CPUTF8) value);
            } else if (value instanceof CPNameAndType) {
                ((CPNameAndType) value).resolve(pool);
            } else if (value instanceof Annotation) {
                ((Annotation) value).resolve(pool);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] nestedValues = (ElementValue[]) value;
                for (ElementValue nestedValue : nestedValues) {
                    nestedValue.resolve(pool);
                }
            }
        }

        public void writeBody(final DataOutputStream dos) throws IOException {
            dos.writeByte(tag);
            if (constant_value_index != -1) {
                dos.writeShort(constant_value_index);
            } else if (value instanceof CPNameAndType) {
                ((CPNameAndType) value).writeBody(dos);
            } else if (value instanceof Annotation) {
                ((Annotation) value).writeBody(dos);
            } else if (value instanceof ElementValue[]) {
                final ElementValue[] nestedValues = (ElementValue[]) value;
                dos.writeShort(nestedValues.length);
                for (ElementValue nestedValue : nestedValues) {
                    nestedValue.writeBody(dos);
                }
            } else {
                throw new Error("");
            }
        }

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
                for (ElementValue nestedValue : nestedValues) {
                    length += nestedValue.getLength();
                }
                return length;
            case '@':
                return (1 + ((Annotation) value).getLength());
            }
            return 0;
        }
    }

    public AnnotationsAttribute(final CPUTF8 attributeName) {
        super(attributeName);
    }

}
