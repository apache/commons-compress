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
 * A compressor-defined class file attribute.
 */
public class NewAttribute extends BCIRenumberedAttribute {

    private final List lengths = new ArrayList(); // List of Integers
    private final List body = new ArrayList();
    private ClassConstantPool pool;
    private final int layoutIndex;

    public NewAttribute(final CPUTF8 attributeName, final int layoutIndex) {
        super(attributeName);
        this.layoutIndex = layoutIndex;
    }

    public int getLayoutIndex() {
        return layoutIndex;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#getLength()
     */
    @Override
    protected int getLength() {
        int length = 0;
        for (Object length2 : lengths) {
            length += ((Integer) length2).intValue();
        }
        return length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#writeBody(java.io.DataOutputStream)
     */
    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        for (int i = 0; i < lengths.size(); i++) {
            final int length = ((Integer) lengths.get(i)).intValue();
            final Object obj = body.get(i);
            long value = 0;
            if (obj instanceof Long) {
                value = ((Long) obj).longValue();
            } else if (obj instanceof ClassFileEntry) {
                value = pool.indexOf(((ClassFileEntry) obj));
            } else if (obj instanceof BCValue) {
                value = ((BCValue) obj).actualValue;
            }
            // Write
            if (length == 1) {
                dos.writeByte((int) value);
            } else if (length == 2) {
                dos.writeShort((int) value);
            } else if (length == 4) {
                dos.writeInt((int) value);
            } else if (length == 8) {
                dos.writeLong(value);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry#toString()
     */
    @Override
    public String toString() {
        return attributeName.underlyingString();
    }

    public void addInteger(final int length, final long value) {
        lengths.add(Integer.valueOf(length));
        body.add(Long.valueOf(value));
    }

    public void addBCOffset(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCOffset(value));
    }

    public void addBCIndex(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCIndex(value));
    }

    public void addBCLength(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCLength(value));
    }

    public void addToBody(final int length, final Object value) {
        lengths.add(Integer.valueOf(length));
        body.add(value);
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        for (final Object element : body) {
            if (element instanceof ClassFileEntry) {
                ((ClassFileEntry) element).resolve(pool);
            }
        }
        this.pool = pool;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        int total = 1;
        for (final Object element : body) {
            if (element instanceof ClassFileEntry) {
                total++;
            }
        }
        final ClassFileEntry[] nested = new ClassFileEntry[total];
        nested[0] = getAttributeName();
        int i = 1;
        for (final Object element : body) {
            if (element instanceof ClassFileEntry) {
                nested[i] = (ClassFileEntry) element;
                i++;
            }
        }
        return nested;
    }

    private static class BCOffset extends BCValue {

        private final int offset;
        private int index;

        public BCOffset(final int offset) {
            this.offset = offset;
        }

        public void setIndex(final int index) {
            this.index = index;
        }

    }

    private static class BCIndex extends BCValue {

        private final int index;

        public BCIndex(final int index) {
            this.index = index;
        }
    }

    private static class BCLength extends BCValue {

        private final int length;

        public BCLength(final int length) {
            this.length = length;
        }
    }

    // Bytecode-related value (either a bytecode index or a length)
    private static abstract class BCValue {

        int actualValue;

        public void setActualValue(final int value) {
            this.actualValue = value;
        }

    }

    @Override
    protected int[] getStartPCs() {
        // Don't need to return anything here as we've overridden renumber
        return null;
    }

    @Override
    public void renumber(final List byteCodeOffsets) {
        if (!renumbered) {
            Object previous = null;
            for (Object obj : body) {
                if (obj instanceof BCIndex) {
                    final BCIndex bcIndex = (BCIndex) obj;
                    bcIndex.setActualValue(((Integer) byteCodeOffsets.get(bcIndex.index)).intValue());
                } else if (obj instanceof BCOffset) {
                    final BCOffset bcOffset = (BCOffset) obj;
                    if (previous instanceof BCIndex) {
                        final int index = ((BCIndex) previous).index + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(((Integer) byteCodeOffsets.get(index)).intValue());
                    } else if (previous instanceof BCOffset) {
                        final int index = ((BCOffset) previous).index + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(((Integer) byteCodeOffsets.get(index)).intValue());
                    } else {
                        // Not sure if this should be able to happen
                        bcOffset.setActualValue(((Integer) byteCodeOffsets.get(bcOffset.offset)).intValue());
                    }
                } else if (obj instanceof BCLength) {
                    // previous must be a BCIndex
                    final BCLength bcLength = (BCLength) obj;
                    final BCIndex prevIndex = (BCIndex) previous;
                    final int index = prevIndex.index + bcLength.length;
                    final int actualLength = ((Integer) byteCodeOffsets.get(index)).intValue() - prevIndex.actualValue;
                    bcLength.setActualValue(actualLength);
                }
                previous = obj;
            }
            renumbered = true;
        }
    }

}
