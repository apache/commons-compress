/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.lz4;

/**
 * Implementation of the xxhash32 hash algorithm.
 *
 * @see <a href="https://cyan4973.github.io/xxHash/">xxHash</a>
 * @NotThreadSafe
 * @since 1.14
 * @deprecated Use {@link org.apache.commons.codec.digest.XXHash32}.
 */
@Deprecated
public class XXHash32 extends org.apache.commons.codec.digest.XXHash32 {

    /**
     * Creates an XXHash32 instance with a seed of 0.
     */
    public XXHash32() {
        super(0);
    }

    /**
     * Creates an XXHash32 instance.
     *
     * @param seed the seed to use
     */
    public XXHash32(final int seed) {
        super(seed);
    }
}
