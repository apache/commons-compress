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
package org.apache.commons.compress.harmony.unpack200;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassConstantPool;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.ConstantPoolEntry;

/**
 * Inner Class Bands
 */
public class IcBands extends BandSet {

    private IcTuple[] icAll;

    private final String[] cpUTF8;

    private final String[] cpClass;

    private Map<String, IcTuple> thisClassToTuple;
    private Map<String, List<IcTuple>> outerClassToTuples;

    /**
     * @param segment TODO
     */
    public IcBands(final Segment segment) {
        super(segment);
        this.cpClass = segment.getCpBands().getCpClass();
        this.cpUTF8 = segment.getCpBands().getCpUTF8();
    }

    public IcTuple[] getIcTuples() {
        return icAll;
    }

    /**
     * Answer the relevant IcTuples for the specified className and class constant pool.
     *
     * @param className String name of the class X for ic_relevant(X)
     * @param cp ClassConstantPool used to generate ic_relevant(X)
     * @return array of IcTuple
     */
    public IcTuple[] getRelevantIcTuples(final String className, final ClassConstantPool cp) {
        final Set<IcTuple> relevantTuplesContains = new HashSet<>();
        final List<IcTuple> relevantTuples = new ArrayList<>();

        final List<IcTuple> relevantCandidates = outerClassToTuples.get(className);
        if (relevantCandidates != null) {
            for (int index = 0; index < relevantCandidates.size(); index++) {
                final IcTuple tuple = relevantCandidates.get(index);
                relevantTuplesContains.add(tuple);
                relevantTuples.add(tuple);
            }
        }

        final List<ClassFileEntry> entries = cp.entries();

        // For every class constant in both ic_this_class and cp,
        // add it to ic_relevant. Repeat until no more
        // changes to ic_relevant.

        for (int eIndex = 0; eIndex < entries.size(); eIndex++) {
            final ConstantPoolEntry entry = (ConstantPoolEntry) entries.get(eIndex);
            if (entry instanceof CPClass) {
                final CPClass clazz = (CPClass) entry;
                final IcTuple relevant = thisClassToTuple.get(clazz.name);
                if (relevant != null && relevantTuplesContains.add(relevant)) {
                    relevantTuples.add(relevant);
                }
            }
        }

        // Not part of spec: fix up by adding to relevantTuples the parents
        // of inner classes which are themselves inner classes.
        // i.e., I think that if Foo$Bar$Baz gets added, Foo$Bar needs to be
        // added
        // as well.

        final List<IcTuple> tuplesToScan = new ArrayList<>(relevantTuples);
        final List<IcTuple> tuplesToAdd = new ArrayList<>();

        while (tuplesToScan.size() > 0) {

            tuplesToAdd.clear();
            for (int index = 0; index < tuplesToScan.size(); index++) {
                final IcTuple aRelevantTuple = tuplesToScan.get(index);
                final IcTuple relevant = thisClassToTuple.get(aRelevantTuple.outerClassString());
                if (relevant != null && !aRelevantTuple.outerIsAnonymous()) {
                    tuplesToAdd.add(relevant);
                }
            }

            tuplesToScan.clear();
            for (int index = 0; index < tuplesToAdd.size(); index++) {
                final IcTuple tuple = tuplesToAdd.get(index);
                if (relevantTuplesContains.add(tuple)) {
                    relevantTuples.add(tuple);
                    tuplesToScan.add(tuple);
                }
            }

        }

        // End not part of the spec. Ugh.

        // Now order the result as a subsequence of ic_all
        relevantTuples.sort((arg0, arg1) -> {
            final Integer index1 = Integer.valueOf(arg0.getTupleIndex());
            final Integer index2 = Integer.valueOf(arg1.getTupleIndex());
            return index1.compareTo(index2);
        });

        return relevantTuples.toArray(IcTuple.EMPTY_ARRAY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        // Read IC bands
        final int innerClassCount = header.getInnerClassCount();
        final int[] icThisClassInts = decodeBandInt("ic_this_class", in, Codec.UDELTA5, innerClassCount);
        final String[] icThisClass = getReferences(icThisClassInts, cpClass);
        final int[] icFlags = decodeBandInt("ic_flags", in, Codec.UNSIGNED5, innerClassCount);
        final int outerClasses = SegmentUtils.countBit16(icFlags);
        final int[] icOuterClassInts = decodeBandInt("ic_outer_class", in, Codec.DELTA5, outerClasses);
        final String[] icOuterClass = new String[outerClasses];
        for (int i = 0; i < icOuterClass.length; i++) {
            if (icOuterClassInts[i] == 0) {
                icOuterClass[i] = null;
            } else {
                icOuterClass[i] = cpClass[icOuterClassInts[i] - 1];
            }
        }
        final int[] icNameInts = decodeBandInt("ic_name", in, Codec.DELTA5, outerClasses);
        final String[] icName = new String[outerClasses];
        for (int i = 0; i < icName.length; i++) {
            if (icNameInts[i] == 0) {
                icName[i] = null;
            } else {
                icName[i] = cpUTF8[icNameInts[i] - 1];
            }
        }

        // Construct IC tuples
        icAll = new IcTuple[icThisClass.length];
        int index = 0;
        for (int i = 0; i < icThisClass.length; i++) {
            final String icTupleC = icThisClass[i];
            final int icTupleF = icFlags[i];
            String icTupleC2 = null;
            String icTupleN = null;
            final int cIndex = icThisClassInts[i];
            int c2Index = -1;
            int nIndex = -1;
            if ((icFlags[i] & 1 << 16) != 0) {
                icTupleC2 = icOuterClass[index];
                icTupleN = icName[index];
                c2Index = icOuterClassInts[index] - 1;
                nIndex = icNameInts[index] - 1;
                index++;
            }
            icAll[i] = new IcTuple(icTupleC, icTupleF, icTupleC2, icTupleN, cIndex, c2Index, nIndex, i);
        }
    }

    @Override
    public void unpack() throws IOException, Pack200Exception {
        final IcTuple[] allTuples = getIcTuples();
        thisClassToTuple = new HashMap<>(allTuples.length);
        outerClassToTuples = new HashMap<>(allTuples.length);
        for (final IcTuple tuple : allTuples) {

            // generate mapping thisClassString -> IcTuple
            // presumably this relation is 1:1
            //
            final Object result = thisClassToTuple.put(tuple.thisClassString(), tuple);
            if (result != null) {
                throw new Error("Collision detected in <thisClassString, IcTuple> mapping. "
                    + "There are at least two inner clases with the same name.");
            }

            // generate mapping outerClassString -> IcTuple
            // this relation is 1:M

            // If it's not anon and the outer is not anon, it could be relevant
            if ((!tuple.isAnonymous() && !tuple.outerIsAnonymous()) || (tuple.nestedExplicitFlagSet())) {

                // add tuple to corresponding bucket
                final String key = tuple.outerClassString();
                List<IcTuple> bucket = outerClassToTuples.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    outerClassToTuples.put(key, bucket);
                }
                bucket.add(tuple);
            }
        }
    }

}