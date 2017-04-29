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
package org.apache.commons.compress2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.compress2.Format;

/**
 * Loads formats defined as "services" and provides access to them.
 *
 * <p>Uses {@link java.util.ServiceLoader} under the covers but iterates over all formats found eagerly inside the
 * constructor so errors are reported early.</p>
 */
public abstract class FormatDiscoverer<F extends Format> implements Iterable<F> {
    private final ServiceLoader<F> formatLoader;
    private Map<String, F> formats;

    /**
     * Loads services using the given class loader.
     * @throws ServiceConfigurationError if an error occurs reading a service file or instantiating a format
     */
    protected FormatDiscoverer(ClassLoader cl, Class<F> clazz) throws ServiceConfigurationError {
        this(ServiceLoader.load(clazz, cl));
    }

    private FormatDiscoverer(ServiceLoader<F> loader) {
        formatLoader = loader;
        fillMap();
    }

    /**
     * Clears the cached formats and rebuilds it.
     *
     * @see ServiceLoader#reload
     */
    public void reload() {
        formatLoader.reload();
        fillMap();
    }

    /**
     * Iterator over all known formats.
     */
    public Iterator<F> iterator() {
        return formats.values().iterator();
    }

    /**
     * Iterates over all known formats that support writing.
     */
    public Iterable<F> getFormatsWithWriteSupport() {
        return filter(Format::supportsWriting);
    }

    /**
     * Gets a format by its name.
     * @param name the {@link Format#getName name} of the format.
     * @return the Format instance if one is known by that name
     */
    public Optional<F> getFormatByName(String name) {
        return Optional.ofNullable(formats.get(name));
    }

    private void fillMap() throws ServiceConfigurationError {
        Set<F> ts = new TreeSet<F>(Format.AUTO_DETECTION_ORDER);
        ts.addAll(asList(formatLoader));
        formats = Collections.unmodifiableMap(ts.stream()
            .collect(Collectors.toMap(Format::getName, Function.identity())));
    }

    protected Iterable<F> filter(final Predicate<F> p) {
        return () -> StreamSupport.stream(Spliterators.spliterator(formats.values(),
                                                                   Spliterator.NONNULL),
                                          false)
            .filter(p)
            .iterator();
    }

    private static <T> List<T> asList(Iterable<T> i) {
        List<T> l = new ArrayList<T>();
        for (T t : i) {
            l.add(t);
        }
        return l;
    }
}
