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
package org.apache.commons.compress2.compressors;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import org.apache.commons.compress2.FormatDiscoverer;

/**
 * Loads CompressionFormats defined as "services" from {@code
 * META-INF/services/org.apache.commons.compress2.compressors.CompressionFormat} and provides access to them.
 *
 * <p>Uses {@link java.util.ServiceLoader} under the covers but iterates over all formats found eagerly inside the
 * constructor so errors are reported early.</p>
 */
public class Compressors extends FormatDiscoverer<CompressionFormat> {

    /**
     * Loads services using the current thread's context class loader.
     * @throws ServiceConfigurationError if an error occurs reading a service file or instantiating a format
     */
    public Compressors() throws ServiceConfigurationError {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads services using the given class loader.
     * @throws ServiceConfigurationError if an error occurs reading a service file or instantiating a format
     */
    public Compressors(ClassLoader cl) throws ServiceConfigurationError {
        super(cl, CompressionFormat.class);
    }

    /**
     * Gets a format by its name.
     * @param name the {@link CompressionFormat#getName name} of the format.
     * @return the Format instance if one is known by that name
     */
    public Optional<CompressionFormat> getCompressionFormatByName(String name) {
        return getFormatByName(name);
    }
}
