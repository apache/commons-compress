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
 * A compressor-defined class file attribute.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class NewAttribute extends BCIRenumberedAttribute {

    // Bytecode-related value (either a bytecode index or a length)
    private abstract static class AbstractBcValue {

        int actualValue;

        public void setActualValue(final int value) {
            this.actualValue = value;
        }

    }

    private static final class BCIndex extends AbstractBcValue {

        private final int index;

        BCIndex(final int index) {
            this.index = index;
        }
    }

    private static final class BCLength extends AbstractBcValue {

        private final int length;

        BCLength(final int length) {
            this.length = length;
        }
    }

    private static final class BCOffset extends AbstractBcValue {

        private final int offset;
        private int index;

        BCOffset(final int offset) {
            this.offset = offset;
        }

        public void setIndex(final int index) {
            this.index = index;
        }

    }

    private final List<Integer> lengths = new ArrayList<>();

    private final List<Object> body = new ArrayList<>();

    private ClassConstantPool pool;

    private final int layoutIndex;

    /**
     * Constructs a new NewAttribute.
     *
     * @param attributeName the attribute name.
     * @param layoutIndex the layout index.
     */
    public NewAttribute(final CPUTF8 attributeName, final int layoutIndex) {
        super(attributeName);
        this.layoutIndex = layoutIndex;
    }

    /**
     * Adds a bytecode index.
     *
     * @param length the length.
     * @param value the value.
     */
    public void addBCIndex(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCIndex(value));
    }

    /**
     * Adds a bytecode length.
     *
     * @param length the length.
     * @param value the value.
     */
    public void addBCLength(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCLength(value));
    }

    /**
     * Adds a bytecode offset.
     *
     * @param length the length.
     * @param value the value.
     */
    public void addBCOffset(final int length, final int value) {
        lengths.add(Integer.valueOf(length));
        body.add(new BCOffset(value));
    }

    /**
     * Adds an integer value.
     *
     * @param length the length.
     * @param value the value.
     */
    public void addInteger(final int length, final long value) {
        lengths.add(Integer.valueOf(length));
        body.add(Long.valueOf(value));
    }

    /**
     * Adds a value to the body.
     *
     * @param length the length.
     * @param value the value.
     */
    public void addToBody(final int length, final Object value) {
        lengths.add(Integer.valueOf(length));
        body.add(value);
    }

    /**
     * Gets the layout index.
     *
     * @return the layout index.
     */
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
        for (final Integer len : lengths) {
            length += len.intValue();
        }
        return length;
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

    @Override
    protected int[] getStartPCs() {
        // Don't need to return anything here as we've overridden renumber
        return null;
    }

    @Override
    public void renumber(final List<Integer> byteCodeOffsets) {
        if (!renumbered) {
            Object previous = null;
            for (final Object obj : body) {
                if (obj instanceof BCIndex) {
                    final BCIndex bcIndex = (BCIndex) obj;
                    bcIndex.setActualValue(byteCodeOffsets.get(bcIndex.index).intValue());
                } else if (obj instanceof BCOffset) {
                    final BCOffset bcOffset = (BCOffset) obj;
                    if (previous instanceof BCIndex) {
                        final int index = ((BCIndex) previous).index + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(byteCodeOffsets.get(index).intValue());
                    } else if (previous instanceof BCOffset) {
                        final int index = ((BCOffset) previous).index + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(byteCodeOffsets.get(index).intValue());
                    } else {
                        // Not sure if this should be able to happen
                        bcOffset.setActualValue(byteCodeOffsets.get(bcOffset.offset).intValue());
                    }
                } else if (obj instanceof BCLength) {
                    // previous must be a BCIndex
                    final BCLength bcLength = (BCLength) obj;
                    final BCIndex prevIndex = (BCIndex) previous;
                    final int index = prevIndex.index + bcLength.length;
                    final int actualLength = byteCodeOffsets.get(index).intValue() - prevIndex.actualValue;
                    bcLength.setActualValue(actualLength);
                }
                previous = obj;
            }
            renumbered = true;
        }
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

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry#toString()
     */
    @Override
    public String toString() {
        return attributeName.underlyingString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#writeBody(java.io.DataOutputStream)
     */
    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        for (int i = 0; i < lengths.size(); i++) {
            final int length = lengths.get(i).intValue();
            final Object obj = body.get(i);
            long value = 0;
            if (obj instanceof Long) {
                value = ((Long) obj).longValue();
            } else if (obj instanceof ClassFileEntry) {
                value = pool.indexOf((ClassFileEntry) obj);
            } else if (obj instanceof AbstractBcValue) {
                value = ((AbstractBcValue) obj).actualValue;
            }
            // Write
            switch (length) {
            case 1:
                dos.writeByte((int) value);
                break;
            case 2:
                dos.writeShort((int) value);
                break;
            case 4:
                dos.writeInt((int) value);
                break;
            case 8:
                dos.writeLong(value);
                break;
            default:
                break;
            }
        }
    }

}
