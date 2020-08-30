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
import java.util.Iterator;
import java.util.List;

/**
 * A compressor-defined class file attribute.
 */
public class NewAttribute extends BCIRenumberedAttribute {

    private final List lengths = new ArrayList(); // List of Integers
    private final List body = new ArrayList();
    private ClassConstantPool pool;
    private final int layoutIndex;

    public NewAttribute(CPUTF8 attributeName, int layoutIndex) {
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
    protected int getLength() {
        int length = 0;
        for(int iter = 0; iter < lengths.size(); iter++) {
            length += ((Integer)lengths.get(iter)).intValue();
        }
        return length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.bytecode.Attribute#writeBody(java.io.DataOutputStream)
     */
    protected void writeBody(DataOutputStream dos) throws IOException {
        for (int i = 0; i < lengths.size(); i++) {
            int length = ((Integer) lengths.get(i)).intValue();
            Object obj = body.get(i);
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
    public String toString() {
        return attributeName.underlyingString();
    }

    public void addInteger(int length, long value) {
        lengths.add(new Integer(length));
        body.add(new Long(value));
    }

    public void addBCOffset(int length, int value) {
        lengths.add(new Integer(length));
        body.add(new BCOffset(value));
    }

    public void addBCIndex(int length, int value) {
        lengths.add(new Integer(length));
        body.add(new BCIndex(value));
    }

    public void addBCLength(int length, int value) {
        lengths.add(new Integer(length));
        body.add(new BCLength(value));
    }

    public void addToBody(int length, Object value) {
        lengths.add(new Integer(length));
        body.add(value);
    }

    protected void resolve(ClassConstantPool pool) {
        super.resolve(pool);
        for(int iter = 0; iter < body.size(); iter++) {
            Object element = body.get(iter);
            if (element instanceof ClassFileEntry) {
                ((ClassFileEntry) element).resolve(pool);
            }
        }
        this.pool = pool;
    }

    protected ClassFileEntry[] getNestedClassFileEntries() {
    	int total = 1;
    	for(int iter = 0; iter < body.size(); iter++) {
            Object element = body.get(iter);
            if (element instanceof ClassFileEntry) {
                total++;
            }
        }
    	ClassFileEntry[] nested = new ClassFileEntry[total];
    	nested[0] = getAttributeName();
    	int i = 1;
    	for(int iter = 0; iter < body.size(); iter++) {
            Object element = body.get(iter);
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

        public BCOffset(int offset) {
            this.offset = offset;
        }

        public void setIndex(int index) {
            this.index = index;
        }

    }

    private static class BCIndex extends BCValue {

        private final int index;

        public BCIndex(int index) {
            this.index = index;
        }
    }

    private static class BCLength extends BCValue {

        private final int length;

        public BCLength(int length) {
            this.length = length;
        }
    }

    // Bytecode-related value (either a bytecode index or a length)
    private static abstract class BCValue {

        int actualValue;

        public void setActualValue(int value) {
            this.actualValue = value;
        }

    }

    protected int[] getStartPCs() {
        // Don't need to return anything here as we've overridden renumber
        return null;
    }

    public void renumber(List byteCodeOffsets) {
        if (!renumbered) {
            Object previous = null;
            for (Iterator iter = body.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                if (obj instanceof BCIndex) {
                    BCIndex bcIndex = (BCIndex) obj;
                    bcIndex.setActualValue(((Integer) byteCodeOffsets
                            .get(bcIndex.index)).intValue());
                } else if (obj instanceof BCOffset) {
                    BCOffset bcOffset = (BCOffset) obj;
                    if (previous instanceof BCIndex) {
                        int index = ((BCIndex) previous).index
                                + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(((Integer) byteCodeOffsets
                                .get(index)).intValue());
                    } else if (previous instanceof BCOffset) {
                        int index = ((BCOffset) previous).index
                                + bcOffset.offset;
                        bcOffset.setIndex(index);
                        bcOffset.setActualValue(((Integer) byteCodeOffsets
                                .get(index)).intValue());
                    } else {
                        // Not sure if this should be able to happen
                        bcOffset.setActualValue(((Integer) byteCodeOffsets
                                .get(bcOffset.offset)).intValue());
                    }
                } else if (obj instanceof BCLength) {
                    // previous must be a BCIndex
                    BCLength bcLength = (BCLength) obj;
                    BCIndex prevIndex = (BCIndex) previous;
                    int index = prevIndex.index + bcLength.length;
                    int actualLength = ((Integer) byteCodeOffsets.get(index))
                            .intValue()
                            - prevIndex.actualValue;
                    bcLength.setActualValue(actualLength);
                }
                previous = obj;
            }
            renumbered = true;
        }
    }

}
