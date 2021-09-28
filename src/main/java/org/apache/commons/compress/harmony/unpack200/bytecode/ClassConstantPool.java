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

    protected HashSet entriesContainsSet = new HashSet();
    protected HashSet othersContainsSet = new HashSet();

    private final HashSet mustStartClassPool = new HashSet();

    protected Map indexCache;

    private final List others = new ArrayList(500);
    private final List entries = new ArrayList(500);

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
        final ArrayList parents = new ArrayList(512);
        final ArrayList children = new ArrayList(512);

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
                final ClassFileEntry entry = (ClassFileEntry) parents.get(indexParents);

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

    public int indexOf(final ClassFileEntry entry) {
        if (!resolved) {
            throw new IllegalStateException("Constant pool is not yet resolved; this does not make any sense");
        }
        if (null == indexCache) {
            throw new IllegalStateException("Index cache is not initialized!");
        }
        final Integer entryIndex = ((Integer) indexCache.get(entry));
        // If the entry isn't found, answer -1. Otherwise answer the entry.
        if (entryIndex != null) {
            return entryIndex.intValue() + 1;
        }
        return -1;
    }

    public int size() {
        return entries.size();
    }

    public ClassFileEntry get(int i) {
        if (!resolved) {
            throw new IllegalStateException("Constant pool is not yet resolved; this does not make any sense");
        }
        return (ClassFileEntry) entries.get(--i);
    }

    public void resolve(final Segment segment) {
        initialSort();
        sortClassPool();

        resolved = true;

        for (Object entry2 : entries) {
            final ClassFileEntry entry = (ClassFileEntry) entry2;
            entry.resolve(this);
        }

        for (Object other : others) {
            final ClassFileEntry entry = (ClassFileEntry) other;
            entry.resolve(this);
        }

    }

    private void initialSort() {
        final TreeSet inCpAll = new TreeSet(
            (arg0, arg1) -> ((ConstantPoolEntry) arg0).getGlobalIndex() - ((ConstantPoolEntry) arg1).getGlobalIndex());
        final TreeSet cpUtf8sNotInCpAll = new TreeSet(
            (arg0, arg1) -> ((CPUTF8) arg0).underlyingString().compareTo(((CPUTF8) arg1).underlyingString()));
        final TreeSet cpClassesNotInCpAll = new TreeSet(
            (arg0, arg1) -> ((CPClass) arg0).getName().compareTo(((CPClass) arg1).getName()));

        for (Object entry2 : entries) {
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

    public List entries() {
        return Collections.unmodifiableList(entries);
    }

    protected void sortClassPool() {
        // Now that everything has been resolved, do one
        // final sort of the class pool. This fixes up
        // references to objects which need to be at the
        // start of the class pool

        final ArrayList startOfPool = new ArrayList(entries.size());
        final ArrayList finalSort = new ArrayList(entries.size());

        for (Object entry : entries) {
            final ClassFileEntry nextEntry = (ClassFileEntry) entry;
            if (mustStartClassPool.contains(nextEntry)) {
                startOfPool.add(nextEntry);
            } else {
                finalSort.add(nextEntry);
            }
        }

        // copy over and rebuild the cache
        //
        indexCache = new HashMap(entries.size());
        int index = 0;

        entries.clear();

        for (Object element : startOfPool) {
            final ClassFileEntry entry = (ClassFileEntry) element;
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

        for (Object element : finalSort) {
            final ClassFileEntry entry = (ClassFileEntry) element;
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

    public ClassFileEntry addWithNestedEntries(final ClassFileEntry entry) {
        add(entry);
        final ClassFileEntry[] nestedEntries = entry.getNestedClassFileEntries();
        for (ClassFileEntry nestedEntry : nestedEntries) {
            addWithNestedEntries(nestedEntry);
        }
        return entry;
    }
}
