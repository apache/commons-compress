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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

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
        return filter(WRITE_PREDICATE);
    }

    /**
     * Iterates over all known formats that can write archives to channels.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithWriteSupportForChannels() {
        return filter(WRITE_TO_CHANNEL_PREDICATE);
    }

    /**
     * Iterates over all known formats that can read archives from channels.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithReadSupportForChannels() {
        return filter(READ_FROM_CHANNEL_PREDICATE);
    }

    /**
     * Iterates over all known formats that provide random access input.
     */
    public Iterable<ArchiveFormat<? extends ArchiveEntry>> getFormatsWithRandomAccessInput() {
        return filter(RANDOM_ACCESS_PREDICATE);
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
        Set<ArchiveFormat> ts = new TreeSet<ArchiveFormat>(SORT_FOR_AUTO_DETECTION);
        ts.addAll(asList(formatLoader));
        Map<String, ArchiveFormat<? extends ArchiveEntry>> a =
            new LinkedHashMap<String, ArchiveFormat<? extends ArchiveEntry>>();
        for (ArchiveFormat<? extends ArchiveEntry> f : ts) {
            a.put(f.getName(), f);
        }
        archivers = Collections.unmodifiableMap(a);
    }

    private interface Predicate<T> { boolean matches(T t); }

    private static final Predicate<ArchiveFormat<? extends ArchiveEntry>> WRITE_PREDICATE =
        new Predicate<ArchiveFormat<? extends ArchiveEntry>>() {
            public boolean matches(ArchiveFormat<? extends ArchiveEntry> a) {
                return a.supportsWriting();
            }
        };

    private static final Predicate<ArchiveFormat<? extends ArchiveEntry>> WRITE_TO_CHANNEL_PREDICATE =
        new Predicate<ArchiveFormat<? extends ArchiveEntry>>() {
            public boolean matches(ArchiveFormat<? extends ArchiveEntry> a) {
                return a.supportsWritingToNonSeekableChannels();
            }
        };

    private static final Predicate<ArchiveFormat<? extends ArchiveEntry>> READ_FROM_CHANNEL_PREDICATE =
        new Predicate<ArchiveFormat<? extends ArchiveEntry>>() {
            public boolean matches(ArchiveFormat<? extends ArchiveEntry> a) {
                return a.supportsReadingFromNonSeekableChannels();
            }
        };

    private static final Predicate<ArchiveFormat<? extends ArchiveEntry>> RANDOM_ACCESS_PREDICATE =
        new Predicate<ArchiveFormat<? extends ArchiveEntry>>() {
            public boolean matches(ArchiveFormat<? extends ArchiveEntry> a) {
                return a.supportsRandomAccessInput();
            }
        };

    private static final Predicate<ArchiveFormat<? extends ArchiveEntry>> AUTO_DETECTION_PREDICATE =
        new Predicate<ArchiveFormat<? extends ArchiveEntry>>() {
            public boolean matches(ArchiveFormat<? extends ArchiveEntry> a) {
                return a.supportsAutoDetection();
            }
        };

    private Iterable<ArchiveFormat<? extends ArchiveEntry>>
        filter(final Predicate<ArchiveFormat<? extends ArchiveEntry>> p) {
        return new Iterable<ArchiveFormat<? extends ArchiveEntry>>() {
            public Iterator<ArchiveFormat<? extends ArchiveEntry>> iterator() {
                return new FilteringIterator(Archivers.this.iterator(), p);
            }
        };
    }

    private static <T> List<T> asList(Iterable<T> i) {
        List<T> l = new ArrayList<T>();
        for (T t : i) {
            l.add(t);
        }
        return l;
    }

    private Comparator<ArchiveFormat> SORT_FOR_AUTO_DETECTION = new Comparator<ArchiveFormat>() {
        public int compare(ArchiveFormat a1, ArchiveFormat a2) {
            if (a1.supportsAutoDetection() && a2.supportsAutoDetection()) {
                return a1.getNumberOfBytesRequiredForAutodetection() - a2.getNumberOfBytesRequiredForAutodetection();
            }
            if (!a1.supportsAutoDetection() && !a2.supportsAutoDetection()) {
                return 0;
            }
            if (a1.supportsAutoDetection()) {
                return -1;
            }
            return 1;
        }
    };

    private static class FilteringIterator<T> implements Iterator<T> {
        private final Iterator<T> i;
        private final Predicate<? super T> filter;
        private T lookAhead = null;
        private FilteringIterator(Iterator<T> i, Predicate<? super T> filter) {
            this.i = i;
            this.filter = filter;
        }
        public void remove() {
            i.remove();
        }
        public T next() {
            if (lookAhead == null) {
                throw new NoSuchElementException();
            }
            T next = lookAhead;
            lookAhead = null;
            return next;
        }
        public boolean hasNext() {
            while (lookAhead == null && i.hasNext()) {
                T next = i.next();
                if (filter.matches(next)) {
                    lookAhead = next;
                }
            }
            return lookAhead != null;
        }
    }
}
