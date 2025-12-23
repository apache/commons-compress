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
 * Constant pool entry for a UTF8 entry, used for storing long Strings.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class CPUTF8 extends ConstantPoolEntry implements Comparable {

    private final String string;

    /**
     * Constructs a new CPUTF8.
     *
     * @param string the UTF-8 string.
     */
    public CPUTF8(final String string) {
        this.string = string;
    }

    @Override
    public int compareTo(final Object arg0) {
        return string.compareTo(((CPUTF8) arg0).string);
    }

    /**
     * Gets the underlying string.
     *
     * @return the underlying string.
     */
    public String getUnderlyingString() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

}
