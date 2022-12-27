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

import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.ConstantPoolEntry;

/**
 * SegmentConstantPool manages the constant pool used for re-creating class files.
 */
public class SegmentConstantPool {

    public static final int ALL = 0;
    public static final int UTF_8 = 1;

    public static final int CP_INT = 2;

    // define in archive order

    public static final int CP_FLOAT = 3;
    public static final int CP_LONG = 4;
    public static final int CP_DOUBLE = 5;
    public static final int CP_STRING = 6;
    public static final int CP_CLASS = 7;
    public static final int SIGNATURE = 8; // TODO and more to come --
    public static final int CP_DESCR = 9;
    public static final int CP_FIELD = 10;
    public static final int CP_METHOD = 11;
    public static final int CP_IMETHOD = 12;
    protected static final String REGEX_MATCH_ALL = ".*";
    protected static final String INITSTRING = "<init>";
    protected static final String REGEX_MATCH_INIT = "^" + INITSTRING + ".*";

    /**
     * We don't want a dependency on regex in Pack200. The only place one exists is in matchSpecificPoolEntryIndex(). To
     * eliminate this dependency, we've implemented the world's stupidest regexMatch. It knows about the two forms we
     * care about: .* (aka REGEX_MATCH_ALL) {@code ^<init>;.*} (aka REGEX_MATCH_INIT) and will answer correctly if those
     * are passed as the regexString.
     *
     * @param regexString String against which the compareString will be matched
     * @param compareString String to match against the regexString
     * @return boolean true if the compareString matches the regexString; otherwise false.
     */
    protected static boolean regexMatches(final String regexString, final String compareString) {
        if (REGEX_MATCH_ALL.equals(regexString)) {
            return true;
        }
        if (REGEX_MATCH_INIT.equals(regexString)) {
            if (compareString.length() < (INITSTRING.length())) {
                return false;
            }
            return (INITSTRING.equals(compareString.substring(0, INITSTRING.length())));
        }
        throw new Error("regex trying to match a pattern I don't know: " + regexString);
    }
    private final CpBands bands;
    private final SegmentConstantPoolArrayCache arrayCache = new SegmentConstantPoolArrayCache();

    /**
     * @param bands TODO
     */
    public SegmentConstantPool(final CpBands bands) {
        this.bands = bands;
    }

    /**
     * Given the name of a class, answer the CPClass associated with that class. Answer null if the class doesn't exist.
     *
     * @param name Class name to look for (form: java/lang/Object)
     * @return CPClass for that class name, or null if not found.
     */
    public ConstantPoolEntry getClassPoolEntry(final String name) {
        final String[] classes = bands.getCpClass();
        final int index = matchSpecificPoolEntryIndex(classes, name, 0);
        if (index == -1) {
            return null;
        }
        try {
            return getConstantPoolEntry(CP_CLASS, index);
        } catch (final Pack200Exception ex) {
            throw new Error("Error getting class pool entry");
        }
    }

    /**
     * Subset the constant pool of the specified type to be just that which has the specified class name. Answer the
     * ConstantPoolEntry at the desiredIndex of the subsetted pool.
     *
     * @param cp type of constant pool array to search
     * @param desiredIndex index of the constant pool
     * @param desiredClassName class to use to generate a subset of the pool
     * @return ConstantPoolEntry
     * @throws Pack200Exception TODO
     */
    public ConstantPoolEntry getClassSpecificPoolEntry(final int cp, final long desiredIndex,
        final String desiredClassName) throws Pack200Exception {
        final int index = (int) desiredIndex;
        int realIndex = -1;
        String[] array = null;
        switch (cp) {
        case CP_FIELD:
            array = bands.getCpFieldClass();
            break;
        case CP_METHOD:
            array = bands.getCpMethodClass();
            break;
        case CP_IMETHOD:
            array = bands.getCpIMethodClass();
            break;
        default:
            throw new Error("Don't know how to handle " + cp);
        }
        realIndex = matchSpecificPoolEntryIndex(array, desiredClassName, index);
        return getConstantPoolEntry(cp, realIndex);
    }

    public ConstantPoolEntry getConstantPoolEntry(final int cp, final long value) throws Pack200Exception {
        final int index = (int) value;
        if (index == -1) {
            return null;
        }
        if (index < 0) {
            throw new Pack200Exception("Cannot have a negative range");
        }
        switch (cp) {
        case UTF_8:
            return bands.cpUTF8Value(index);
        case CP_INT:
            return bands.cpIntegerValue(index);
        case CP_FLOAT:
            return bands.cpFloatValue(index);
        case CP_LONG:
            return bands.cpLongValue(index);
        case CP_DOUBLE:
            return bands.cpDoubleValue(index);
        case CP_STRING:
            return bands.cpStringValue(index);
        case CP_CLASS:
            return bands.cpClassValue(index);
        case SIGNATURE:
            throw new Error("I don't know what to do with signatures yet");
            // return null /* new CPSignature(bands.getCpSignature()[index]) */;
        case CP_DESCR:
            throw new Error("I don't know what to do with descriptors yet");
            // return null /* new CPDescriptor(bands.getCpDescriptor()[index])
            // */;
        case CP_FIELD:
            return bands.cpFieldValue(index);
        case CP_METHOD:
            return bands.cpMethodValue(index);
        case CP_IMETHOD:
            return bands.cpIMethodValue(index);
        default:
            break;
        }
        // etc
        throw new Error("Get value incomplete");
    }

    /**
     * Answer the init method for the specified class.
     *
     * @param cp constant pool to search (must be CP_METHOD)
     * @param value index of init method
     * @param desiredClassName String class name of the init method
     * @return CPMethod init method
     * @throws Pack200Exception TODO
     */
    public ConstantPoolEntry getInitMethodPoolEntry(final int cp, final long value, final String desiredClassName)
        throws Pack200Exception {
        int realIndex = -1;
        if (cp != CP_METHOD) {
            // TODO really an error?
            throw new Error("Nothing but CP_METHOD can be an <init>");
        }
        realIndex = matchSpecificPoolEntryIndex(bands.getCpMethodClass(), bands.getCpMethodDescriptor(),
            desiredClassName, REGEX_MATCH_INIT, (int) value);
        return getConstantPoolEntry(cp, realIndex);
    }

    public ClassFileEntry getValue(final int cp, final long value) throws Pack200Exception {
        final int index = (int) value;
        if (index == -1) {
            return null;
        }
        if (index < 0) {
            throw new Pack200Exception("Cannot have a negative range");
        }
        switch (cp) {
        case UTF_8:
            return bands.cpUTF8Value(index);
        case CP_INT:
            return bands.cpIntegerValue(index);
        case CP_FLOAT:
            return bands.cpFloatValue(index);
        case CP_LONG:
            return bands.cpLongValue(index);
        case CP_DOUBLE:
            return bands.cpDoubleValue(index);
        case CP_STRING:
            return bands.cpStringValue(index);
        case CP_CLASS:
            return bands.cpClassValue(index);
        case SIGNATURE:
            return bands.cpSignatureValue(index);
        case CP_DESCR:
            return bands.cpNameAndTypeValue(index);
        default:
            break;
        }
        throw new Error("Tried to get a value I don't know about: " + cp);
    }

    /**
     * A number of things make use of subsets of structures. In one particular example, _super bytecodes will use a
     * subset of method or field classes which have just those methods / fields defined in the superclass. Similarly,
     * _this bytecodes use just those methods/fields defined in this class, and _init bytecodes use just those methods
     * that start with {@code <init>}.
     *
     * This method takes an array of names, a String to match for, an index and a boolean as parameters, and answers the
     * array position in the array of the indexth element which matches (or equals) the String (depending on the state
     * of the boolean)
     *
     * In other words, if the class array consists of: Object [position 0, 0th instance of Object] String [position 1,
     * 0th instance of String] String [position 2, 1st instance of String] Object [position 3, 1st instance of Object]
     * Object [position 4, 2nd instance of Object] then matchSpecificPoolEntryIndex(..., "Object", 2, false) will answer
     * 4. matchSpecificPoolEntryIndex(..., "String", 0, false) will answer 1.
     *
     * @param nameArray Array of Strings against which the compareString is tested
     * @param compareString String for which to search
     * @param desiredIndex nth element with that match (counting from 0)
     * @return int index into nameArray, or -1 if not found.
     */
    protected int matchSpecificPoolEntryIndex(final String[] nameArray, final String compareString,
        final int desiredIndex) {
        return matchSpecificPoolEntryIndex(nameArray, nameArray, compareString, REGEX_MATCH_ALL, desiredIndex);
    }

    /**
     * This method's function is to look through pairs of arrays. It keeps track of the number of hits it finds using
     * the following basis of comparison for a hit: - the primaryArray[index] must be .equals() to the
     * primaryCompareString - the secondaryArray[index] .matches() the secondaryCompareString. When the desiredIndex
     * number of hits has been reached, the index into the original two arrays of the element hit is returned.
     *
     * @param primaryArray The first array to search
     * @param secondaryArray The second array (must be same .length as primaryArray)
     * @param primaryCompareString The String to compare against primaryArray using .equals()
     * @param secondaryCompareRegex The String to compare against secondaryArray using .matches()
     * @param desiredIndex The nth hit whose position we're seeking
     * @return int index that represents the position of the nth hit in primaryArray and secondaryArray
     */
    protected int matchSpecificPoolEntryIndex(final String[] primaryArray, final String[] secondaryArray,
        final String primaryCompareString, final String secondaryCompareRegex, final int desiredIndex) {
        int instanceCount = -1;
        final List<Integer> indexList = arrayCache.indexesForArrayKey(primaryArray, primaryCompareString);
        if (indexList.isEmpty()) {
            // Primary key not found, no chance of finding secondary
            return -1;
        }

        for (final Integer element : indexList) {
            final int arrayIndex = element.intValue();
            if (regexMatches(secondaryCompareRegex, secondaryArray[arrayIndex])) {
                instanceCount++;
                if (instanceCount == desiredIndex) {
                    return arrayIndex;
                }
            }
        }
        // We didn't return in the for loop, so the desiredMatch
        // with desiredIndex must not exist in the arrays.
        return -1;
    }
}