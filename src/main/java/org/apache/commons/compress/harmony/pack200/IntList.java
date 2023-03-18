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

import java.util.Arrays;

/**
 * IntList is based on {@code java.util.ArrayList}, but is written specifically for ints in order to reduce boxing
 * and unboxing to Integers, reduce the memory required and improve performance of pack200.
 */
public class IntList {

    private int[] array;
    private int firstIndex;
    private int lastIndex;
    private int modCount;

    /**
     * Constructs a new instance of IntList with capacity for ten elements.
     */
    public IntList() {
        this(10);
    }

    /**
     * Constructs a new instance of IntList with the specified capacity.
     *
     * @param capacity the initial capacity of this IntList
     */
    public IntList(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        firstIndex = lastIndex = 0;
        array = new int[capacity];
    }

    /**
     * Adds the specified object at the end of this IntList.
     *
     * @param object the object to add
     * @return true
     */
    public boolean add(final int object) {
        if (lastIndex == array.length) {
            growAtEnd(1);
        }
        array[lastIndex++] = object;
        modCount++;
        return true;
    }

    public void add(final int location, final int object) {
        final int size = lastIndex - firstIndex;
        if (0 < location && location < size) {
            if (firstIndex == 0 && lastIndex == array.length) {
                growForInsert(location, 1);
            } else if ((location < size / 2 && firstIndex > 0) || lastIndex == array.length) {
                System.arraycopy(array, firstIndex, array, --firstIndex, location);
            } else {
                final int index = location + firstIndex;
                System.arraycopy(array, index, array, index + 1, size - location);
                lastIndex++;
            }
            array[location + firstIndex] = object;
        } else if (location == 0) {
            if (firstIndex == 0) {
                growAtFront(1);
            }
            array[--firstIndex] = object;
        } else if (location == size) {
            if (lastIndex == array.length) {
                growAtEnd(1);
            }
            array[lastIndex++] = object;
        } else {
            throw new IndexOutOfBoundsException();
        }

        modCount++;
    }

    public void addAll(final IntList list) {
        growAtEnd(list.size());
        for (int i = 0; i < list.size(); i++) {
            add(list.get(i));
        }
    }

    public void clear() {
        if (firstIndex != lastIndex) {
            Arrays.fill(array, firstIndex, lastIndex, -1);
            firstIndex = lastIndex = 0;
            modCount++;
        }
    }

    public int get(final int location) {
        if (0 <= location && location < (lastIndex - firstIndex)) {
            return array[firstIndex + location];
        }
        throw new IndexOutOfBoundsException("" + location);
    }

    private void growAtEnd(final int required) {
        final int size = lastIndex - firstIndex;
        if (firstIndex >= required - (array.length - lastIndex)) {
            final int newLast = lastIndex - firstIndex;
            if (size > 0) {
                System.arraycopy(array, firstIndex, array, 0, size);
            }
            firstIndex = 0;
            lastIndex = newLast;
        } else {
            int increment = size / 2;
            if (required > increment) {
                increment = required;
            }
            if (increment < 12) {
                increment = 12;
            }
            final int[] newArray = new int[size + increment];
            if (size > 0) {
                System.arraycopy(array, firstIndex, newArray, 0, size);
                firstIndex = 0;
                lastIndex = size;
            }
            array = newArray;
        }
    }

    private void growAtFront(final int required) {
        final int size = lastIndex - firstIndex;
        if (array.length - lastIndex + firstIndex >= required) {
            final int newFirst = array.length - size;
            if (size > 0) {
                System.arraycopy(array, firstIndex, array, newFirst, size);
            }
            firstIndex = newFirst;
            lastIndex = array.length;
        } else {
            int increment = size / 2;
            if (required > increment) {
                increment = required;
            }
            if (increment < 12) {
                increment = 12;
            }
            final int[] newArray = new int[size + increment];
            if (size > 0) {
                System.arraycopy(array, firstIndex, newArray, newArray.length - size, size);
            }
            firstIndex = newArray.length - size;
            lastIndex = newArray.length;
            array = newArray;
        }
    }

    private void growForInsert(final int location, final int required) {
        final int size = lastIndex - firstIndex;
        int increment = size / 2;
        if (required > increment) {
            increment = required;
        }
        if (increment < 12) {
            increment = 12;
        }
        final int[] newArray = new int[size + increment];
        final int newFirst = increment - required;
        // Copy elements after location to the new array skipping inserted
        // elements
        System.arraycopy(array, location + firstIndex, newArray, newFirst + location + required, size - location);
        // Copy elements before location to the new array from firstIndex
        System.arraycopy(array, firstIndex, newArray, newFirst, location);
        firstIndex = newFirst;
        lastIndex = size + increment;

        array = newArray;
    }

    public void increment(final int location) {
        if ((0 > location) || (location >= (lastIndex - firstIndex))) {
            throw new IndexOutOfBoundsException("" + location);
        }
        array[firstIndex + location]++;
    }

    public boolean isEmpty() {
        return lastIndex == firstIndex;
    }

    public int remove(final int location) {
        int result;
        final int size = lastIndex - firstIndex;
        if ((0 > location) || (location >= size)) {
            throw new IndexOutOfBoundsException();
        }
        if (location == size - 1) {
            result = array[--lastIndex];
            array[lastIndex] = 0;
        } else if (location == 0) {
            result = array[firstIndex];
            array[firstIndex++] = 0;
        } else {
            final int elementIndex = firstIndex + location;
            result = array[elementIndex];
            if (location < size / 2) {
                System.arraycopy(array, firstIndex, array, firstIndex + 1, location);
                array[firstIndex++] = 0;
            } else {
                System.arraycopy(array, elementIndex + 1, array, elementIndex, size - location - 1);
                array[--lastIndex] = 0;
            }
        }
        if (firstIndex == lastIndex) {
            firstIndex = lastIndex = 0;
        }

        modCount++;
        return result;
    }

    public int size() {
        return lastIndex - firstIndex;
    }

    public int[] toArray() {
        final int size = lastIndex - firstIndex;
        final int[] result = new int[size];
        System.arraycopy(array, firstIndex, result, 0, size);
        return result;
    }

}
