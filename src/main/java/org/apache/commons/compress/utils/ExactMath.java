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

package org.apache.commons.compress.utils;

/**
 * PRIVATE.
 *
 * Performs exact math through {@link Math} "exact" APIs.
 */
public class ExactMath {

    private ExactMath() {
        // no instances
    }

    /**
     * Adds two values and throws an exception on overflow.
     *
     * @param intValue the first value.
     * @param longValue the second value.
     * @return addition of both values.
     * @throws ArithmeticException when there is an overflow.
     */
    public static int add(final int intValue, final long longValue) {
        return Math.addExact(intValue, Math.toIntExact(longValue));
    }
}
