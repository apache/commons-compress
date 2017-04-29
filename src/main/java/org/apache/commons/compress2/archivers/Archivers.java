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
package org.apache.commons.compress2.archivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.List;
import java.util.Map;
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
 * Loads ArchiveFormats defined as "services" from {@code
 * META-INF/services/org.apache.commons.compress2.archivers.ArchiveFormat} and provides access to them.
 *
 * <p>Uses {@link java.util.ServiceLoader} under the covers but iterates over all formats found eagerly inside the
 * constructor so errors are reported early.</p>
 */
public class Archivers implements Iterable<ArchiveFormat<? extends ArchiveEntry>> {
    private final ServiceLoader<ArchiveFormat> formatLoader;
    private Map<String, ArchiveFormat<? extends ArchiveEntry>> archivers;

    /**
     * Loads services using the current thread's context class loader.
     * @throws ServiceConfigurationError if an error occurs reading a service file or instantiating a format
     */
    public Archivers() throws ServiceConfigurationError {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads services using the given class loader.
     * @throws ServiceConfigurationError if an error occurs reading a service file or instantiating a format
     */
    public Archivers(ClassLoader cl) throws ServiceConfigurationError {
        this(ServiceLoader.load(ArchiveFormat.class, cl));
    }

    private Archivers(ServiceLoader<ArchiveFormat> loader) {
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
    public Iterator<ArchiveFormat<? extends ArchiveEntry>> iterator() {
        return archivers.values().iterator();
    }

    /**
     * Iterates over all known formats that can write archives.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithWriteSupport() {
        return filter(ArchiveFormat::supportsWriting);
    }

    /**
     * Iterates over all known formats that can write archives to channels.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithWriteSupportForNonSeekableChannels() {
        return filter(ArchiveFormat::supportsWritingToNonSeekableChannels);
    }

    /**
     * Iterates over all known formats that can read archives from channels.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithReadSupportForNonSeekableChannels() {
        return filter(ArchiveFormat::supportsReadingFromNonSeekableChannels);
    }

    /**
     * Iterates over all known formats that provide random access input.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithRandomAccessInput() {
        return filter(ArchiveFormat::supportsRandomAccessInput);
    }

    /**
     * Gets a format by its name.
     * @param name the {@link ArchiveFormat#getName name} of the format.
     * @return the ArchiveFormat instance or null if not format is known by that name
     */
    public ArchiveFormat getArchiveFormatByName(String name) {
        return archivers.get(name);
    }

    private void fillMap() throws ServiceConfigurationError {
        Set<ArchiveFormat<? extends ArchiveEntry>> ts =
            new TreeSet<ArchiveFormat<? extends ArchiveEntry>>(Format.AUTO_DETECTION_ORDER);
        ts.addAll(asList(formatLoader));
        archivers = Collections.unmodifiableMap(ts.stream()
            .collect(Collectors.toMap(ArchiveFormat::getName, Function.identity())));
    }

    private Iterable<ArchiveFormat<? extends ArchiveEntry>>
        filter(final Predicate<ArchiveFormat<? extends ArchiveEntry>> p) {
        return () -> StreamSupport.stream(Spliterators.spliterator(archivers.values(),
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
