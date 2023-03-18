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

import java.util.ArrayList;
import java.util.List;

/**
 * An IcTuple is the set of information that describes an inner class.
 *
 * C is the fully qualified class name<br>
 * F is the flags<br>
 * C2 is the outer class name, or null if it can be inferred from C<br>
 * N is the inner class name, or null if it can be inferred from C<br>
 */
public class IcTuple {

    private static final String[] EMPTY_STRING_ARRAY = {};
    public static final int NESTED_CLASS_FLAG = 0x00010000;
    static final IcTuple[] EMPTY_ARRAY = {};
    private final int cIndex;
    private final int c2Index;

    private final int nIndex;

    private final int tIndex;
    protected String C; // this class

    protected int F; // flags
    protected String C2; // outer class
    protected String N; // name
    private boolean predictSimple;

    private boolean predictOuter;
    private String cachedOuterClassString;
    private String cachedSimpleClassName;
    private boolean initialized;
    private boolean anonymous;
    private boolean outerIsAnonymous;
    private boolean member = true;
    private int cachedOuterClassIndex = -1;
    private int cachedSimpleClassNameIndex = -1;
    private boolean hashCodeComputed;

    private int cachedHashCode;

    /**
     *
     * @param C TODO
     * @param F TODO
     * @param C2 TODO
     * @param N TODO
     * @param cIndex the index of C in cpClass
     * @param c2Index the index of C2 in cpClass, or -1 if C2 is null
     * @param nIndex the index of N in cpUTF8, or -1 if N is null
     * @param tIndex TODO
     */
    public IcTuple(final String C, final int F, final String C2, final String N, final int cIndex, final int c2Index,
        final int nIndex, final int tIndex) {
        this.C = C;
        this.F = F;
        this.C2 = C2;
        this.N = N;
        this.cIndex = cIndex;
        this.c2Index = c2Index;
        this.nIndex = nIndex;
        this.tIndex = tIndex;
        if (null == N) {
            predictSimple = true;
        }
        if (null == C2) {
            predictOuter = true;
        }
        initializeClassStrings();
    }

    private boolean computeOuterIsAnonymous() {
        final String[] result = innerBreakAtDollar(cachedOuterClassString);
        if (result.length == 0) {
            throw new Error("Should have an outer before checking if it's anonymous");
        }

        for (final String element : result) {
            if (isAllDigits(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(final Object object) {
        if ((object == null) || (object.getClass() != this.getClass())) {
            return false;
        }
        final IcTuple compareTuple = (IcTuple) object;

        if (!nullSafeEquals(this.C, compareTuple.C)) {
            return false;
        }

        if (!nullSafeEquals(this.C2, compareTuple.C2)) {
            return false;
        }

        if (!nullSafeEquals(this.N, compareTuple.N)) {
            return false;
        }
        return true;
    }

    private void generateHashCode() {
        hashCodeComputed = true;
        cachedHashCode = 17;
        if (C != null) {
            cachedHashCode = +C.hashCode();
        }
        if (C2 != null) {
            cachedHashCode = +C2.hashCode();
        }
        if (N != null) {
            cachedHashCode = +N.hashCode();
        }
    }

    public String getC() {
        return C;
    }

    public String getC2() {
        return C2;
    }

    public int getF() {
        return F;
    }

    public String getN() {
        return N;
    }

    public int getTupleIndex() {
        return tIndex;
    }

    @Override
    public int hashCode() {
        if (!hashCodeComputed) {
            generateHashCode();
        }
        return cachedHashCode;
    }

    private void initializeClassStrings() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!predictSimple) {
            cachedSimpleClassName = N;
        }
        if (!predictOuter) {
            cachedOuterClassString = C2;
        }
        // Class names must be calculated from
        // this class name.
        final String[] nameComponents = innerBreakAtDollar(C);
        if (nameComponents.length == 0) {
            // Unable to predict outer class
            // throw new Error("Unable to predict outer class name: " + C);
        }
        if (nameComponents.length == 1) {
            // Unable to predict simple class name
            // throw new Error("Unable to predict inner class name: " + C);
        }
        if (nameComponents.length < 2) {
            // If we get here, we hope cachedSimpleClassName
            // and cachedOuterClassString were caught by the
            // predictSimple / predictOuter code above.
            return;
        }

        // If we get to this point, nameComponents.length must be >=2
        final int lastPosition = nameComponents.length - 1;
        cachedSimpleClassName = nameComponents[lastPosition];
        cachedOuterClassString = "";
        for (int index = 0; index < lastPosition; index++) {
            cachedOuterClassString += nameComponents[index];
            if (isAllDigits(nameComponents[index])) {
                member = false;
            }
            if (index + 1 != lastPosition) {
                // TODO: might need more logic to handle
                // classes with separators of non-$ characters
                // (ie Foo#Bar)
                cachedOuterClassString += '$';
            }
        }
        // TODO: these two blocks are the same as blocks
        // above. Can we eliminate some by reworking the logic?
        if (!predictSimple) {
            cachedSimpleClassName = N;
            cachedSimpleClassNameIndex = nIndex;
        }
        if (!predictOuter) {
            cachedOuterClassString = C2;
            cachedOuterClassIndex = c2Index;
        }
        if (isAllDigits(cachedSimpleClassName)) {
            anonymous = true;
            member = false;
            if (nestedExplicitFlagSet()) {
                // Predicted class - marking as member
                member = true;
            }
        }

        outerIsAnonymous = computeOuterIsAnonymous();
    }

    /**
     * Break the receiver into components at $ boundaries.
     *
     * @param className TODO
     * @return TODO
     */
    public String[] innerBreakAtDollar(final String className) {
        final List<String> resultList = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (index < className.length()) {
            if (className.charAt(index) <= '$') {
                resultList.add(className.substring(start, index));
                start = index + 1;
            }
            index++;
            if (index >= className.length()) {
                // Add the last element
                resultList.add(className.substring(start));
            }
        }
        return resultList.toArray(EMPTY_STRING_ARRAY);
    }

    private boolean isAllDigits(final String nameString) {
        // Answer true if the receiver is all digits; otherwise answer false.
        if (null == nameString) {
            return false;
        }
        for (int index = 0; index < nameString.length(); index++) {
            if (!Character.isDigit(nameString.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public boolean isMember() {
        return member;
    }
    /**
     * Answer true if the receiver's bit 16 is set (indicating that explicit outer class and name fields are set).
     *
     * @return boolean
     */
    public boolean nestedExplicitFlagSet() {
        return (F & NESTED_CLASS_FLAG) == NESTED_CLASS_FLAG;
    }

    public boolean nullSafeEquals(final String stringOne, final String stringTwo) {
        if (null == stringOne) {
            return null == stringTwo;
        }
        return stringOne.equals(stringTwo);
    }

    public int outerClassIndex() {
        return cachedOuterClassIndex;
    }

    /**
     * Answer the outer class name for the receiver. This may either be specified or inferred from inner class name.
     *
     * @return String name of outer class
     */
    public String outerClassString() {
        return cachedOuterClassString;
    }

    public boolean outerIsAnonymous() {
        return outerIsAnonymous;
    }

    /**
     * Answer true if the receiver is predicted; answer false if the receiver is specified explicitly in the outer and
     * name fields.
     *
     * @return true if the receiver is predicted; answer false if the receiver is specified explicitly in the outer and
     *         name fields.
     */
    public boolean predicted() {
        return predictOuter || predictSimple;
    }

    /**
     * Answer the inner class name for the receiver.
     *
     * @return String name of inner class
     */
    public String simpleClassName() {
        return cachedSimpleClassName;
    }

    public int simpleClassNameIndex() {
        return cachedSimpleClassNameIndex;
    }

    public int thisClassIndex() {
        if (predicted()) {
            return cIndex;
        }
        return -1;
    }

    /**
     * Answer the full name of the inner class represented by this tuple (including its outer component)
     *
     * @return String full name of inner class
     */
    public String thisClassString() {
        if (predicted()) {
            return C;
        }
        // TODO: this may not be right. What if I
        // get a class like Foo#Bar$Baz$Bug?
        return C2 + "$" + N;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("IcTuple ");
        result.append('(');
        result.append(simpleClassName());
        result.append(" in ");
        result.append(outerClassString());
        result.append(')');
        return result.toString();
    }
}
