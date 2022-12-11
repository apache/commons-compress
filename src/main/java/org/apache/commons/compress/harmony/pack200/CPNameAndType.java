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
package org.apache.commons.compress.harmony.pack200;

/**
 * Constant pool entry for a name and type pair.
 */
public class CPNameAndType extends ConstantPoolEntry implements Comparable {

    private final CPUTF8 name;
    private final CPSignature signature;

    public CPNameAndType(final CPUTF8 name, final CPSignature signature) {
        this.name = name;
        this.signature = signature;
    }

    @Override
    public int compareTo(final Object obj) {
        if (obj instanceof CPNameAndType) {
            final CPNameAndType nat = (CPNameAndType) obj;
            final int compareSignature = signature.compareTo(nat.signature);
            if (compareSignature == 0) {
                return name.compareTo(nat.name);
            }
            return compareSignature;
        }
        return 0;
    }

    public String getName() {
        return name.getUnderlyingString();
    }

    public int getNameIndex() {
        return name.getIndex();
    }

    public int getTypeIndex() {
        return signature.getIndex();
    }

    @Override
    public String toString() {
        return name + ":" + signature;
    }

}