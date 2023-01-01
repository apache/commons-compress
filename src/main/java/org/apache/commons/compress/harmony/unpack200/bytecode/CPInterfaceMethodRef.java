/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.commons.compress.harmony.unpack200.bytecode;

/**
 * Interface method reference constant pool entry.
 */
public class CPInterfaceMethodRef extends CPRef {

    private boolean hashCodeComputed;

    private int cachedHashCode;

    public CPInterfaceMethodRef(final CPClass className, final CPNameAndType descriptor, final int globalIndex) {
        super(ConstantPoolEntry.CP_InterfaceMethodref, className, descriptor, globalIndex);
    }
    private void generateHashCode() {
        hashCodeComputed = true;
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + className.hashCode();
        result = PRIME * result + nameAndType.hashCode();
        cachedHashCode = result;
    }

    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            generateHashCode();
        }
        return cachedHashCode;
    }

    /**
     * This method answers the value this method will use for an invokeinterface call. This is equal to 1 + the count of
     * all the args, where longs and doubles count for 2 and all others count for 1.
     *
     * @return integer count
     */
    public int invokeInterfaceCount() {
        return nameAndType.invokeInterfaceCount();
    }

}
