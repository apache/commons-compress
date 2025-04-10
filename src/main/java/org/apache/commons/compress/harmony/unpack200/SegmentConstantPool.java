/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.harmony.unpack200;

import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.compress.harmony.unpack200.bytecode.ConstantPoolEntry;

/**
 * Manages the constant pool used for re-creating class files.
 */
public class SegmentConstantPool {

    /**
     * Value {@value}.
     */
    public static final int ALL = 0;

    /**
     * Value {@value}.
     */
    public static final int UTF_8 = 1;

    /**
     * Value {@value}.
     */
    public static final int CP_INT = 2;

    // define in archive order

    /**
     * Value {@value}.
     */
    public static final int CP_FLOAT = 3;

    /**
     * Value {@value}.
     */
    public static final int CP_LONG = 4;

    /**
     * Value {@value}.
     */
    public static final int CP_DOUBLE = 5;

    /**
     * Value {@value}.
     */
    public static final int CP_STRING = 6;

    /**
     * Value {@value}.
     */
    public static final int CP_CLASS = 7;

    /**
     * Value {@value}.
     */
    public static final int SIGNATURE = 8; // TODO and more to come --

    /**
     * Value {@value}.
     */
    public static final int CP_DESCR = 9;

    /**
     * Value {@value}.
     */
    public static final int CP_FIELD = 10;

    /**
     * Value {@value}.
     */
    public static final int CP_METHOD = 11;

    /**
     * Value {@value}.
     */
    public static final int CP_IMETHOD = 12;

    /**
     * Value {@value}.
     */
    protected static final String REGEX_MATCH_ALL = ".*";

    /**
     * Value {@value}.
     */
    protected static final String INITSTRING = "<init>";

    /**
     * Value {@value}.
     */
    protected static final String REGEX_MATCH_INIT = "^" + INITSTRING + ".*";

    /**
     * We don't want a dependency on regex in Pack200. The only place one exists is in matchSpecificPoolEntryIndex(). To eliminate this dependency, we've
     * implemented the world's stupidest regexMatch. It knows about the two forms we care about: .* (aka REGEX_MATCH_ALL) {@code ^<init>;.*} (aka
     * REGEX_MATCH_INIT) and will answer correctly if those are passed as the regexString.
     *
     * @param regexString   String against which the compareString will be matched
     * @param compareString String to match against the regexString
     * @return boolean true if the compareString matches the regexString; otherwise false.
     */
    protected static boolean regexMatches(final String regexString, final String compareString) {
        if (REGEX_MATCH_ALL.equals(regexString)) {
            return true;
        }
        if (REGEX_MATCH_INIT.equals(regexString)) {
            if (compareString.length() < INITSTRING.length()) {
                return false;
            }
            return INITSTRING.equals(compareString.substring(0, INITSTRING.length()));
        }
        throw new Error("regex trying to match a pattern I don't know: " + regexString);
    }

    static int toIndex(final long index) throws Pack200Exception {
        if (index < 0) {
            throw new Pack200Exception("Cannot have a negative index.");
        }
        return toIntExact(index);
    }

    static int toIntExact(final long index) throws Pack200Exception {
        try {
            return Math.toIntExact(index);
        } catch (final ArithmeticException e) {
            throw new Pack200Exception("index", e);
        }
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
     * Gets the subset constant pool of the specified type to be just that which has the specified class name. Answer the ConstantPoolEntry at the desiredIndex
     * of the subset pool.
     *
     * @param cp               type of constant pool array to search.
     * @param desiredIndex     index of the constant pool.
     * @param desiredClassName class to use to generate a subset of the pool.
     * @return ConstantPoolEntry
     * @throws Pack200Exception if support for a type is not supported or the index not in the range [0, {@link Integer#MAX_VALUE}].
     */
    public ConstantPoolEntry getClassSpecificPoolEntry(final int cp, final long desiredIndex, final String desiredClassName) throws Pack200Exception {
        final String[] array;
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
        final int index = toIndex(desiredIndex);
        final int realIndex = matchSpecificPoolEntryIndex(array, desiredClassName, index);
        return getConstantPoolEntry(cp, realIndex);
    }

    /**
     * Gets the constant pool entry of the given type and index.
     *
     * @param type Constant pool type.
     * @param index Index into a specific constant pool.
     * @return a constant pool entry.
     * @throws Pack200Exception if support for a type is not supported or the index not in the range [0, {@link Integer#MAX_VALUE}].
     */
    public ConstantPoolEntry getConstantPoolEntry(final int type, final long index) throws Pack200Exception {
        if (index == -1) {
            return null;
        }
        final int actualIndex = toIndex(index);
        switch (type) {
        case UTF_8:
            return bands.cpUTF8Value(actualIndex);
        case CP_INT:
            return bands.cpIntegerValue(actualIndex);
        case CP_FLOAT:
            return bands.cpFloatValue(actualIndex);
        case CP_LONG:
            return bands.cpLongValue(actualIndex);
        case CP_DOUBLE:
            return bands.cpDoubleValue(actualIndex);
        case CP_STRING:
            return bands.cpStringValue(actualIndex);
        case CP_CLASS:
            return bands.cpClassValue(actualIndex);
        case SIGNATURE:
            throw new Pack200Exception("Type SIGNATURE is not supported yet: " + SIGNATURE);
        // return null /* new CPSignature(bands.getCpSignature()[index]) */;
        case CP_DESCR:
            throw new Pack200Exception("Type CP_DESCR is not supported yet: " + CP_DESCR);
        // return null /* new CPDescriptor(bands.getCpDescriptor()[index])
        // */;
        case CP_FIELD:
            return bands.cpFieldValue(actualIndex);
        case CP_METHOD:
            return bands.cpMethodValue(actualIndex);
        case CP_IMETHOD:
            return bands.cpIMethodValue(actualIndex);
        default:
            break;
        }
        // etc
        throw new Pack200Exception("Type is not supported yet: " + type);
    }

    /**
     * Gets the {@code init} method for the specified class.
     *
     * @param cp               constant pool to search, must be {@link #CP_METHOD}.
     * @param value            index of {@code init} method.
     * @param desiredClassName String class name of the {@code init} method.
     * @return CPMethod {@code init} method.
     * @throws Pack200Exception if support for a type is not supported or the index not in the range [0, {@link Integer#MAX_VALUE}].
     */
    public ConstantPoolEntry getInitMethodPoolEntry(final int cp, final long value, final String desiredClassName) throws Pack200Exception {
        if (cp != CP_METHOD) {
            throw new Pack200Exception("Nothing but CP_METHOD can be an <init>");
        }
        final int realIndex = matchSpecificPoolEntryIndex(bands.getCpMethodClass(), bands.getCpMethodDescriptor(), desiredClassName, REGEX_MATCH_INIT,
                toIndex(value));
        return getConstantPoolEntry(cp, realIndex);
    }

    public ClassFileEntry getValue(final int cp, final long longIndex) throws Pack200Exception {
        final int index = (int) longIndex;
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
     * A number of things make use of subsets of structures. In one particular example, _super bytecodes will use a subset of method or field classes which have
     * just those methods / fields defined in the superclass. Similarly, _this bytecodes use just those methods/fields defined in this class, and _init
     * bytecodes use just those methods that start with {@code <init>}.
     *
     * This method takes an array of names, a String to match for, an index and a boolean as parameters, and answers the array position in the array of the
     * indexth element which matches (or equals) the String (depending on the state of the boolean)
     *
     * In other words, if the class array consists of: Object [position 0, 0th instance of Object] String [position 1, 0th instance of String] String [position
     * 2, 1st instance of String] Object [position 3, 1st instance of Object] Object [position 4, 2nd instance of Object] then matchSpecificPoolEntryIndex(...,
     * "Object", 2, false) will answer 4. matchSpecificPoolEntryIndex(..., "String", 0, false) will answer 1.
     *
     * @param nameArray     Array of Strings against which the compareString is tested
     * @param compareString String for which to search
     * @param desiredIndex  nth element with that match (counting from 0)
     * @return int index into nameArray, or -1 if not found.
     */
    protected int matchSpecificPoolEntryIndex(final String[] nameArray, final String compareString, final int desiredIndex) {
        return matchSpecificPoolEntryIndex(nameArray, nameArray, compareString, REGEX_MATCH_ALL, desiredIndex);
    }

    /**
     * This method's function is to look through pairs of arrays. It keeps track of the number of hits it finds using the following basis of comparison for a
     * hit: - the primaryArray[index] must be .equals() to the primaryCompareString - the secondaryArray[index] .matches() the secondaryCompareString. When the
     * desiredIndex number of hits has been reached, the index into the original two arrays of the element hit is returned.
     *
     * @param primaryArray          The first array to search
     * @param secondaryArray        The second array (must be same .length as primaryArray)
     * @param primaryCompareString  The String to compare against primaryArray using .equals()
     * @param secondaryCompareRegex The String to compare against secondaryArray using .matches()
     * @param desiredIndex          The nth hit whose position we're seeking
     * @return int index that represents the position of the nth hit in primaryArray and secondaryArray
     */
    protected int matchSpecificPoolEntryIndex(final String[] primaryArray, final String[] secondaryArray, final String primaryCompareString,
            final String secondaryCompareRegex, final int desiredIndex) {
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
