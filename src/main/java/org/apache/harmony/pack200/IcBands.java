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
package org.apache.harmony.pack200;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Inner class bands (corresponds to the <code>ic_bands</code> set of bands in
 * the pack200 specification)
 */
public class IcBands extends BandSet {

    private final Set innerClasses = new TreeSet();
    private final CpBands cpBands;
    private int bit16Count = 0;

    private final Map outerToInner = new HashMap();

    public IcBands(SegmentHeader segmentHeader, CpBands cpBands, int effort) {
        super(effort, segmentHeader);
        this.cpBands = cpBands;
    }

    /**
     * All input classes for the segment have now been read in, so this method
     * is called so that this class can calculate/complete anything it could not
     * do while classes were being read.
     */
    public void finaliseBands() {
        segmentHeader.setIc_count(innerClasses.size());
    }

    public void pack(OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing internal class bands...");
        int[] ic_this_class = new int[innerClasses.size()];
        int[] ic_flags = new int[innerClasses.size()];
        int[] ic_outer_class = new int[bit16Count];
        int[] ic_name = new int[bit16Count];

        int index2 = 0;
        List innerClassesList = new ArrayList(innerClasses);
        for (int i = 0; i < ic_this_class.length; i++) {
            IcTuple icTuple = (IcTuple) innerClassesList.get(i);
            ic_this_class[i] = icTuple.C.getIndex();
            ic_flags[i] = icTuple.F;
            if((icTuple.F & (1<<16)) != 0) {
                ic_outer_class[index2] = icTuple.C2 == null ? 0 : icTuple.C2.getIndex() + 1;
                ic_name[index2] = icTuple.N == null ? 0 : icTuple.N.getIndex() + 1;
                index2++;
            }
        }
        byte[] encodedBand = encodeBandInt("ic_this_class", ic_this_class,
                Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from ic_this_class[" + ic_this_class.length + "]");

        encodedBand = encodeBandInt("ic_flags", ic_flags, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from ic_flags[" + ic_flags.length + "]");

        encodedBand = encodeBandInt("ic_outer_class", ic_outer_class,
                Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from ic_outer_class[" + ic_outer_class.length + "]");

        encodedBand = encodeBandInt("ic_name", ic_name, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length
                + " bytes from ic_name[" + ic_name.length + "]");
    }

    public void addInnerClass(String name, String outerName, String innerName,
            int flags) {
        if(outerName != null || innerName != null) {
            if(namesArePredictable(name, outerName, innerName)) {
                IcTuple innerClass = new IcTuple(cpBands.getCPClass(name), flags, null, null);
                addToMap(outerName, innerClass);
                innerClasses.add(innerClass);
            } else {
                flags |= (1<<16);
                IcTuple icTuple = new IcTuple(cpBands.getCPClass(name), flags, cpBands.getCPClass(outerName), cpBands.getCPUtf8(innerName));
                boolean added = innerClasses.add(icTuple);
                if(added) {
                    bit16Count++;
                    addToMap(outerName, icTuple);
                }
            }
        } else {
            IcTuple innerClass = new IcTuple(cpBands.getCPClass(name), flags, null, null);
            addToMap(getOuter(name), innerClass);
            innerClasses.add(innerClass);
        }
    }

    public List getInnerClassesForOuter(String outerClassName) {
        return (List) outerToInner.get(outerClassName);
    }

    private String getOuter(String name) {
        return name.substring(0, name.lastIndexOf('$'));
    }

    private void addToMap(String outerName, IcTuple icTuple) {
        List tuples = (List) outerToInner.get(outerName);
        if(tuples == null) {
            tuples = new ArrayList();
            outerToInner.put(outerName, tuples);
            tuples.add(icTuple);
        } else {
            for (Iterator iterator = tuples.iterator(); iterator.hasNext();) {
                IcTuple icT = (IcTuple) iterator.next();
                if(icTuple.equals(icT)) {
                    return;
                }
            }
            tuples.add(icTuple);
        }
    }

    private boolean namesArePredictable(String name, String outerName,
            String innerName) {
        // TODO: Could be multiple characters, not just $
        return name.equals(outerName + '$' + innerName) && innerName.indexOf('$') == -1;
    }

    class IcTuple implements Comparable {

        protected CPClass C; // this class
        protected int F; // flags
        protected CPClass C2; // outer class
        protected CPUTF8 N; // name

        public IcTuple(CPClass C, int F, CPClass C2, CPUTF8 N) {
            this.C = C;
            this.F = F;
            this.C2 = C2;
            this.N = N;
        }

        public boolean equals(Object o) {
            if(o instanceof IcTuple) {
                IcTuple icT = (IcTuple)o;
                return C.equals(icT.C) && F == icT.F && (C2 != null ? C2.equals(icT.C2) : icT.C2 == null) && (N != null ? N.equals(icT.N) : icT.N == null);
            }
            return false;
        }

        public String toString() {
            return C.toString();
        }

        public int compareTo(Object arg0) {
            return C.compareTo(((IcTuple)arg0).C);
        }

        public boolean isAnonymous() {
            String className = C.toString();
            String innerName = className.substring(className.lastIndexOf('$') + 1);
            return Character.isDigit(innerName.charAt(0));
        }

    }

    public IcTuple getIcTuple(CPClass inner) {
        for (Iterator iterator = innerClasses.iterator(); iterator.hasNext();) {
            IcTuple icTuple = (IcTuple) iterator.next();
            if(icTuple.C.equals(inner)) {
                return icTuple;
            }
        }
        return null;
    }

}
