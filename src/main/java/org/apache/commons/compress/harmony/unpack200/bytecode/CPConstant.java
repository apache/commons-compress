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

import java.util.Objects;

/**
 * Abstract superclass for constant pool constant entries such as numbers or Strings
 */
public abstract class CPConstant extends ConstantPoolEntry {

    private final Object value;

    /**
     * Create a new CPConstant
     *
     * @param tag TODO
     * @param value TODO
     * @param globalIndex index in CpBands
     * @throws NullPointerException if value is null
     */
    public CPConstant(final byte tag, final Object value, final int globalIndex) {
        super(tag, globalIndex);
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final CPConstant other = (CPConstant) obj;
        if (!Objects.equals(value, other.value)) {
            return false;
        }
        return true;
    }

    protected Object getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}