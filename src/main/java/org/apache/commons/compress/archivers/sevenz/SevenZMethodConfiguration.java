/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.sevenz;

/**
 * Combines a SevenZMethod with configuration options for the method.
 * @Immutable
 * @since 1.8
 */
public class SevenZMethodConfiguration {
    private final SevenZMethod method;
    private final Object options;

    /**
     * Doesn't configure any additional options.
     * @param method the method to use
     */
    public SevenZMethodConfiguration(SevenZMethod method) {
        this(method, null);
    }

    /**
     * Specifies and method plus configuration options.
     * @param method the method to use
     * @param options the options to use
     * @throws IllegalArgumentException if the method doesn't understand the options specified.
     */
    public SevenZMethodConfiguration(SevenZMethod method, Object options) {
        this.method = method;
        this.options = options;
        if (options != null && !Coders.findByMethod(method).canAcceptOptions(options)) {
            throw new IllegalArgumentException("The " + method + " method doesn't support options of type "
                                               + options.getClass());
        }
    }

    /**
     * The specified method.
     */
    public SevenZMethod getMethod() {
        return method;
    }

    /**
     * The specified options.
     */
    public Object getOptions() {
        return options;
    }

}
