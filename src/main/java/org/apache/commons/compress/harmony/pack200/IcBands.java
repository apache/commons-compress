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
package org.apache.commons.compress.harmony.pack200;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Inner class bands (corresponds to the {@code ic_bands} set of bands in the pack200 specification)
 */
public class IcBands extends BandSet {

    class IcTuple implements Comparable<IcTuple> {

        protected CPClass C; // this class
        protected int F; // flags
        protected CPClass C2; // outer class
        protected CPUTF8 N; // name

        public IcTuple(final CPClass C, final int F, final CPClass C2, final CPUTF8 N) {
            this.C = C;
            this.F = F;
            this.C2 = C2;
            this.N = N;
        }

        @Override
        public int compareTo(final IcTuple arg0) {
            return C.compareTo(arg0.C);
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof IcTuple) {
                final IcTuple icT = (IcTuple) o;
                return C.equals(icT.C) && F == icT.F && (Objects.equals(C2, icT.C2))
                    && (Objects.equals(N, icT.N));
            }
            return false;
        }

        public boolean isAnonymous() {
            final String className = C.toString();
            final String innerName = className.substring(className.lastIndexOf('$') + 1);
            return Character.isDigit(innerName.charAt(0));
        }

        @Override
        public String toString() {
            return C.toString();
        }

    }
    private final Set<IcTuple> innerClasses = new TreeSet<>();
    private final CpBands cpBands;
    private int bit16Count = 0;

    private final Map<String, List<IcTuple>> outerToInner = new HashMap<>();

    public IcBands(final SegmentHeader segmentHeader, final CpBands cpBands, final int effort) {
        super(effort, segmentHeader);
        this.cpBands = cpBands;
    }

    public void addInnerClass(final String name, final String outerName, final String innerName, int flags) {
        if (outerName != null || innerName != null) {
            if (namesArePredictable(name, outerName, innerName)) {
                final IcTuple innerClass = new IcTuple(cpBands.getCPClass(name), flags, null, null);
                addToMap(outerName, innerClass);
                innerClasses.add(innerClass);
            } else {
                flags |= (1 << 16);
                final IcTuple icTuple = new IcTuple(cpBands.getCPClass(name), flags, cpBands.getCPClass(outerName),
                    cpBands.getCPUtf8(innerName));
                final boolean added = innerClasses.add(icTuple);
                if (added) {
                    bit16Count++;
                    addToMap(outerName, icTuple);
                }
            }
        } else {
            final IcTuple innerClass = new IcTuple(cpBands.getCPClass(name), flags, null, null);
            addToMap(getOuter(name), innerClass);
            innerClasses.add(innerClass);
        }
    }

    private void addToMap(final String outerName, final IcTuple icTuple) {
        List<IcTuple> tuples = outerToInner.get(outerName);
        if (tuples == null) {
            tuples = new ArrayList<>();
            outerToInner.put(outerName, tuples);
            tuples.add(icTuple);
        } else {
            for (final IcTuple tuple : tuples) {
                if (icTuple.equals(tuple)) {
                    return;
                }
            }
            tuples.add(icTuple);
        }
    }

    /**
     * All input classes for the segment have now been read in, so this method is called so that this class can
     * calculate/complete anything it could not do while classes were being read.
     */
    public void finaliseBands() {
        segmentHeader.setIc_count(innerClasses.size());
    }

    public IcTuple getIcTuple(final CPClass inner) {
        for (final IcTuple icTuple : innerClasses) {
            if (icTuple.C.equals(inner)) {
                return icTuple;
            }
        }
        return null;
    }

    public List<IcTuple> getInnerClassesForOuter(final String outerClassName) {
        return outerToInner.get(outerClassName);
    }

    private String getOuter(final String name) {
        return name.substring(0, name.lastIndexOf('$'));
    }

    private boolean namesArePredictable(final String name, final String outerName, final String innerName) {
        // TODO: Could be multiple characters, not just $
        return name.equals(outerName + '$' + innerName) && innerName.indexOf('$') == -1;
    }

    @Override
    public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
        PackingUtils.log("Writing internal class bands...");
        final int[] ic_this_class = new int[innerClasses.size()];
        final int[] ic_flags = new int[innerClasses.size()];
        final int[] ic_outer_class = new int[bit16Count];
        final int[] ic_name = new int[bit16Count];

        int index2 = 0;
        final List<IcTuple> innerClassesList = new ArrayList<>(innerClasses);
        for (int i = 0; i < ic_this_class.length; i++) {
            final IcTuple icTuple = innerClassesList.get(i);
            ic_this_class[i] = icTuple.C.getIndex();
            ic_flags[i] = icTuple.F;
            if ((icTuple.F & (1 << 16)) != 0) {
                ic_outer_class[index2] = icTuple.C2 == null ? 0 : icTuple.C2.getIndex() + 1;
                ic_name[index2] = icTuple.N == null ? 0 : icTuple.N.getIndex() + 1;
                index2++;
            }
        }
        byte[] encodedBand = encodeBandInt("ic_this_class", ic_this_class, Codec.UDELTA5);
        outputStream.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from ic_this_class[" + ic_this_class.length + "]");

        encodedBand = encodeBandInt("ic_flags", ic_flags, Codec.UNSIGNED5);
        outputStream.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from ic_flags[" + ic_flags.length + "]");

        encodedBand = encodeBandInt("ic_outer_class", ic_outer_class, Codec.DELTA5);
        outputStream.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from ic_outer_class[" + ic_outer_class.length + "]");

        encodedBand = encodeBandInt("ic_name", ic_name, Codec.DELTA5);
        outputStream.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from ic_name[" + ic_name.length + "]");
    }

}
