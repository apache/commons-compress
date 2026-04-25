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
package org.apache.commons.compress.harmony.pack200;

/**
 * Constant pool entry for a method or field.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class CPMethodOrField extends ConstantPoolEntry implements Comparable {

    private final CPClass className;
    private final CPNameAndType nameAndType;
    private int indexInClass = -1;
    private int indexInClassForConstructor = -1;

    /**
     * Constructs a new instance.
     *
     * @param className the class name.
     * @param nameAndType the name and type.
     */
    public CPMethodOrField(final CPClass className, final CPNameAndType nameAndType) {
        this.className = className;
        this.nameAndType = nameAndType;
    }

    @Override
    public int compareTo(final Object obj) {
        if (obj instanceof CPMethodOrField) {
            final CPMethodOrField mof = (CPMethodOrField) obj;
            final int compareName = className.compareTo(mof.className);
            if (compareName == 0) {
                return nameAndType.compareTo(mof.nameAndType);
            }
            return compareName;
        }
        return 0;
    }

    /**
     * Gets the class index.
     *
     * @return the class index.
     */
    public int getClassIndex() {
        return className.getIndex();
    }

    /**
     * Gets the class name.
     *
     * @return the class name.
     */
    public CPClass getClassName() {
        return className;
    }

    /**
     * Gets the descriptor (name and type).
     *
     * @return the descriptor.
     */
    public CPNameAndType getDesc() {
        return nameAndType;
    }

    /**
     * Gets the descriptor index.
     *
     * @return the descriptor index.
     */
    public int getDescIndex() {
        return nameAndType.getIndex();
    }

    /**
     * Gets the index in class.
     *
     * @return the index in class.
     */
    public int getIndexInClass() {
        return indexInClass;
    }

    /**
     * Gets the index in class for constructor.
     *
     * @return the index in class for constructor.
     */
    public int getIndexInClassForConstructor() {
        return indexInClassForConstructor;
    }

    /**
     * Sets the index in class.
     *
     * @param index the index.
     */
    public void setIndexInClass(final int index) {
        indexInClass = index;
    }

    /**
     * Sets the index in class for constructor.
     *
     * @param index the index.
     */
    public void setIndexInClassForConstructor(final int index) {
        indexInClassForConstructor = index;
    }

    @Override
    public String toString() {
        return className + ": " + nameAndType;
    }

}
