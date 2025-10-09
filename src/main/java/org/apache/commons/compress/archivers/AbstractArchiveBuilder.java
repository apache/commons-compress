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
package org.apache.commons.compress.archivers;

import org.apache.commons.io.build.AbstractStreamBuilder;

/**
 * Base class for builder pattern implementations of all archive readers.
 *
 * <p>Ensures that all {@code ArchiveInputStream} implementations and other
 * archive handlers expose a consistent set of configuration options.</p>
 *
 * @param <T> The type of archive stream or file to build.
 * @param <B> The type of the concrete builder subclass.
 * @since 1.29.0
 */
public abstract class AbstractArchiveBuilder<T, B extends AbstractArchiveBuilder<T, B>>
        extends AbstractStreamBuilder<T, B> {

    /**
     * Constructs a new instance.
     */
    protected AbstractArchiveBuilder() {
        // empty
    }
}
