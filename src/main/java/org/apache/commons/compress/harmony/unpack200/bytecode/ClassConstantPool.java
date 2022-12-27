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
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.compress.harmony.unpack200.Segment;

/**
 * The Class constant pool
 */
public class ClassConstantPool {

    protected HashSet<ClassFileEntry> entriesContainsSet = new HashSet<>();
    protected HashSet<ClassFileEntry> othersContainsSet = new HashSet<>();

    private final HashSet<ClassFileEntry> mustStartClassPool = new HashSet<>();

    protected Map<ClassFileEntry, Integer> indexCache;

    private final List<ClassFileEntry> others = new ArrayList<>(500);
    private final List<ClassFileEntry> entries = new ArrayList<>(500);

    private boolean resolved;

    public ClassFileEntry add(final ClassFileEntry entry) {
        if (entry instanceof ByteCode) {
            return null;
        }
        if (entry instanceof ConstantPoolEntry) {
            if (entriesContainsSet.add(entry)) {
                entries.add(entry);
            }
        } else if (othersContainsSet.add(entry)) {
            others.add(entry);
        }

        return entry;
    }

    public void addNestedEntries() {
        boolean added = true;

        // initial assignment
        final List<ClassFileEntry> parents = new ArrayList<>(512);
        final List<ClassFileEntry> children = new ArrayList<>(512);

        // adding old entries
        parents.addAll(entries);
        parents.addAll(others);

        // while there any parents to traverse and at least one change in target
        // storage was made
        while (added || parents.size() > 0) {

            children.clear();

            final int entriesOriginalSize = entries.size();
            final int othersOriginalSize = others.size();

            // get the parents' children and add them to buffer
            // concurrently add parents to target storage
            for (int indexParents = 0; indexParents < parents.size(); indexParents++) {
                final ClassFileEntry entry = parents.get(indexParents);

                // traverse children
                final ClassFileEntry[] entryChildren = entry.getNestedClassFileEntries();
                children.addAll(Arrays.asList(entryChildren));

                final boolean isAtStart = (entry instanceof ByteCode) && ((ByteCode) entry).nestedMustStartClassPool();

                if (isAtStart) {
                    mustStartClassPool.addAll(Arrays.asList(entryChildren));
                }

                // add parent
                add(entry);
            }

            added = !(entries.size() == entriesOriginalSize && others.size() == othersOriginalSize);

            // parents are not needed anymore
            // children now become parents
            parents.clear();
            parents.addAll(children);
        }
    }

    public ClassFileEntry addWithNestedEntries(final ClassFileEntry entry) {
        add(entry);
        for (final ClassFileEntry nestedEntry : entry.getNestedClassFileEntries()) {
            addWithNestedEntries(nestedEntry);
        }
        return entry;
    }

    public List<ClassFileEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public ClassFileEntry get(int i) {
        if (!resolved) {
            throw new IllegalStateException("Constant pool is not yet resolved; this does not make any sense");
        }
        return entries.get(--i);
    }

    public int indexOf(final ClassFileEntry entry) {
        if (!resolved) {
            throw new IllegalStateException("Constant pool is not yet resolved; this does not make any sense");
        }
        if (null == indexCache) {
            throw new IllegalStateException("Index cache is not initialized!");
        }
        final Integer entryIndex = (indexCache.get(entry));
        // If the entry isn't found, answer -1. Otherwise answer the entry.
        if (entryIndex != null) {
            return entryIndex.intValue() + 1;
        }
        return -1;
    }

    private void initialSort() {
        final TreeSet<ClassFileEntry> inCpAll = new TreeSet<>(
                Comparator.comparingInt(arg0 -> ((ConstantPoolEntry) arg0).getGlobalIndex()));
        final TreeSet<ClassFileEntry> cpUtf8sNotInCpAll = new TreeSet<>(
                Comparator.comparing(arg0 -> ((CPUTF8) arg0).underlyingString()));
        final TreeSet<ClassFileEntry> cpClassesNotInCpAll = new TreeSet<>(
                Comparator.comparing(arg0 -> ((CPClass) arg0).getName()));

        for (final ClassFileEntry entry2 : entries) {
            final ConstantPoolEntry entry = (ConstantPoolEntry) entry2;
            if (entry.getGlobalIndex() == -1) {
                if (entry instanceof CPUTF8) {
                    cpUtf8sNotInCpAll.add(entry);
                } else if (entry instanceof CPClass) {
                    cpClassesNotInCpAll.add(entry);
                } else {
                    throw new Error("error");
                }
            } else {
                inCpAll.add(entry);
            }
        }
        entries.clear();
        entries.addAll(inCpAll);
        entries.addAll(cpUtf8sNotInCpAll);
        entries.addAll(cpClassesNotInCpAll);
    }

    public void resolve(final Segment segment) {
        initialSort();
        sortClassPool();

        resolved = true;

        entries.forEach(entry -> entry.resolve(this));
        others.forEach(entry -> entry.resolve(this));
    }

    public int size() {
        return entries.size();
    }

    protected void sortClassPool() {
        // Now that everything has been resolved, do one
        // final sort of the class pool. This fixes up
        // references to objects which need to be at the
        // start of the class pool

        final List<ClassFileEntry> startOfPool = new ArrayList<>(entries.size());
        final List<ClassFileEntry> finalSort = new ArrayList<>(entries.size());

        for (final ClassFileEntry entry : entries) {
            if (mustStartClassPool.contains(entry)) {
                startOfPool.add(entry);
            } else {
                finalSort.add(entry);
            }
        }

        // copy over and rebuild the cache
        //
        indexCache = new HashMap<>(entries.size());
        int index = 0;

        entries.clear();

        for (final ClassFileEntry entry : startOfPool) {
            indexCache.put(entry, Integer.valueOf(index));

            if (entry instanceof CPLong || entry instanceof CPDouble) {
                entries.add(entry); // these get 2 slots because of their size
                entries.add(entry);
                index += 2;
            } else {
                entries.add(entry);
                index += 1;
            }
        }

        for (final ClassFileEntry entry : finalSort) {
            indexCache.put(entry, Integer.valueOf(index));

            if (entry instanceof CPLong || entry instanceof CPDouble) {
                entries.add(entry); // these get 2 slots because of their size
                entries.add(entry);
                index += 2;
            } else {
                entries.add(entry);
                index += 1;
            }
        }

    }
}
