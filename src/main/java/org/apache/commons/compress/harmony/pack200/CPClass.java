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
 * Constant pool entry for a class.
 */
public class CPClass extends CPConstant<CPClass> {

    private final String className;
    private final CPUTF8 value;
    private final boolean isInnerClass;

    /**
     * Constructs a new instance.
     *
     * @param value The value.
     */
    public CPClass(final CPUTF8 value) {
        this.value = value;
        this.className = value.getUnderlyingString();
        final char[] chars = className.toCharArray();
        for (final char element : chars) {
            if (element <= 0x2D) {
                isInnerClass = true;
                return;
            }
        }
        isInnerClass = false;
    }

    @Override
    public int compareTo(final CPClass arg0) {
        return className.compareTo(arg0.className);
    }

    public int getIndexInCpUtf8() {
        return value.getIndex();
    }

    public boolean isInnerClass() {
        return isInnerClass;
    }

    @Override
    public String toString() {
        return className;
    }

}
