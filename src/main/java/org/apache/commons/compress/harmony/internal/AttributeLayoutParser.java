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
package org.apache.commons.compress.harmony.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.lang3.IntegerRange;

/**
 * Encapsulates parsing of attribute layout definitions.
 *
 * <p>Previously the parsing logic was duplicated in {@link org.apache.commons.compress.harmony.pack200.NewAttributeBands} and
 * {@link org.apache.commons.compress.harmony.unpack200.NewAttributeBands}.</p>
 *
 * @param <T> a common type shared by {@code attribute_layout} and {@code callable}.
 */
public final class AttributeLayoutParser<T> {

    /**
     * Factory interface for creating attribute layout elements.
     */
    public interface Factory<T> {
        /**
         * Creates a {@code call} layout element.
         *
         * @param callableIndex Index of the callable to call.
         * @return A {@code call} layout element.
         */
        T createCall(int callableIndex);

        /**
         * Creates a {@code callable} attribute layout element.
         *
         * @param body Body of the callable.
         * @return A {@code callable} attribute layout element.
         * @throws Pack200Exception If the callable body is invalid.
         */
        T createCallable(List<T> body) throws Pack200Exception;

        /**
         * Creates an {@code integral} layout element.
         *
         * @param tag Integral tag.
         * @return An {@code integral} layout element.
         */
        T createIntegral(String tag);

        /**
         * Creates a {@code reference} layout element.
         *
         * @param tag Reference tag.
         * @return A {@code reference} layout element.
         */
        T createReference(String tag);

        /**
         * Creates a {@code replication} layout element.
         *
         * @param unsignedInt Unsigned int layout definition for the replication count.
         * @param body        Body of the replication.
         * @return A {@code replication} layout element.
         * @throws Pack200Exception If the replication body is invalid.
         */
        T createReplication(String unsignedInt, List<T> body) throws Pack200Exception;

        /**
         * Creates a {@code union} layout element.
         *
         * @param anyInt Any int layout definition for the union tag.
         * @param cases  List of union cases.
         * @param body   Body of the union.
         * @return A {@code union} layout element.
         * @throws Pack200Exception If the union body is invalid.
         */
        T createUnion(String anyInt, List<UnionCaseData<T>> cases, List<T> body) throws Pack200Exception;
    }

    /**
     * Data class representing a union case in an attribute layout definition.
     */
    public static final class UnionCaseData<T> {
        /**
         * Body of the union case.
         */
        public final List<T> body;

        /**
         * List of tag ranges for the union case.
         */
        public final List<IntegerRange> tagRanges;

        private UnionCaseData(final List<IntegerRange> tagRanges, final List<T> body) {
            this.tagRanges = Collections.unmodifiableList(tagRanges);
            this.body = body;
        }
    }

    private static final int MAX_DEPTH = 64;

    private final CharSequence definition;
    private final Factory<T> factory;
    private int p;
    private int depth;

    public AttributeLayoutParser(final CharSequence definition, final Factory<T> factory) {
        this.definition = definition;
        this.factory = factory;
    }

    private void decrementDepth() throws Pack200Exception {
        depth--;
        if (depth < 0) {
            throw new Pack200Exception("Invalid attribute layout definition: unmatched closing bracket.");
        }
    }

    private void ensureNotEof() throws Pack200Exception {
        if (eof()) {
            throw new Pack200Exception("Incomplete attribute layout definition: " + definition);
        }
    }

    private boolean eof() {
        return p >= definition.length();
    }

    private char expect(char... expected) throws Pack200Exception {
        final char c = next();
        for (char e : expected) {
            if (c == e) {
                return c;
            }
        }
        throw new Pack200Exception("Invalid attribute layout definition: expected one of " + new String(expected) + ", found '" + c + "'");
    }

    void incrementDepth() throws Pack200Exception {
        depth++;
        if (depth > MAX_DEPTH) {
            throw new Pack200Exception("Invalid attribute layout definition: maximum nesting depth exceeded.");
        }
    }

    private char next() throws Pack200Exception {
        ensureNotEof();
        return definition.charAt(p++);
    }

    private char peek() throws Pack200Exception {
        ensureNotEof();
        return definition.charAt(p);
    }

    private String readAnyInt() throws Pack200Exception {
        final char first = next();
        return AttributeLayoutUtils.checkAnyIntTag(first == 'S' ? "S" + next() : String.valueOf(first));
    }

    /**
     * Reads an {@code attribute_layout} definition from the stream.
     *
     * @return A {@code attribute_layout} definition.
     * @throws Pack200Exception If the layout definition is invalid.
     */
    List<T> readAttributeLayout() throws Pack200Exception {
        final List<T> elements = new ArrayList<>();
        T element;
        while ((element = readElement()) != null) {
            elements.add(element);
        }
        return elements;
    }

    private List<T> readBody() throws Pack200Exception {
        expect('[');
        incrementDepth();
        final List<T> bodyElements = readAttributeLayout();
        decrementDepth();
        expect(']');
        return bodyElements;
    }

    /**
     * Reads the next {@code attribute_layout_element} from the stream.
     *
     * <pre>
     * attribute_layout_element:
     *       ( callable | layout_element )
     * callable:
     *       '[' (body)? ']'
     * </pre>
     *
     * @return next AttributeLayoutElement from the stream or {@code null} if end of stream is reached.
     * @throws Pack200Exception If the layout definition is invalid.
     */
    T readElement() throws Pack200Exception {
        if (eof()) {
            return null;
        }
        final char first = peek();
        if (depth == 0 && first == '[') {
            try {
                final List<T> body = readBody();
                if (body.isEmpty()) {
                    throw new Pack200Exception("Corrupted Pack200 archive: Callable body cannot be empty.");
                }
                return factory.createCallable(body);
            } catch (final Exception ex) {
                throw new Pack200Exception(String.format("Corrupted Pack200 archive: Invalid layout '%s' at position %d.", definition, p), ex);
            }
        }
        return readLayoutElement();
    }

    private String readIntegralTag() throws Pack200Exception {
        char c = next();
        final char[] buf = new char[3];
        int len = 0;
        buf[len++] = c;
        if (c == 'F' || c == 'O' || c == 'P' || c == 'S') {
            c = next();
            buf[len++] = c;
            if (c == 'O' || c == 'S') {
                c = next();
                buf[len++] = c;
            }
        }
        return AttributeLayoutUtils.checkIntegralTag(new String(buf, 0, len));
    }

    /**
     * Reads the next {@code layout_element} from the stream.
     *
     * <pre>
     * layout_element:
     *       ( integral | replication | union | call | reference )
     * </pre>
     *
     * @return next LayoutElement from the stream or {@code null} if end of stream is reached.
     * @throws Pack200Exception If the layout definition is invalid.
     */
    T readLayoutElement() throws Pack200Exception {
        if (eof()) {
            return null;
        }
        try {
            switch (peek()) {
                // Integrals
                case 'B':
                case 'H':
                case 'I':
                case 'V':
                case 'S':
                case 'F':
                case 'O':
                case 'P':
                    return factory.createIntegral(readIntegralTag());
                // Replication
                case 'N': {
                    next();
                    final String integral = readUnsignedInt();
                    final List<T> body = readBody();
                    if (body.isEmpty()) {
                        throw new Pack200Exception("Corrupted Pack200 archive: Replication body cannot be empty.");
                    }
                    return factory.createReplication(integral, body);
                }
                // Union
                case 'T': {
                    next();
                    final String anyInt = readAnyInt();
                    final List<UnionCaseData<T>> unionCases = new ArrayList<>();
                    UnionCaseData<T> data;
                    while ((data = readUnionCase()) != null) {
                        unionCases.add(data);
                    }
                    expect('(');
                    expect(')');
                    final List<T> body = readBody();
                    return factory.createUnion(anyInt, unionCases, body);
                }
                // Call
                case '(': {
                    next();
                    final int callNumber = readNumeral();
                    expect(')');
                    return factory.createCall(callNumber);
                }
                // Reference
                case 'K':
                case 'R': {
                    return factory.createReference(readReferenceTag());
                }
                // End of body
                case ']':
                    return null;
                default: {
                    throw new Pack200Exception("Unexpected character '" + peek() + "' in attribute layout definition.");
                }
            }
        } catch (final Exception ex) {
            throw new Pack200Exception(String.format("Corrupted Pack200 archive: Invalid layout '%s' at position %d.", definition, p), ex);
        }
    }

    /**
     * Reads a number from the stream.
     *
     * <p>Stops reading at the first character, which is not a digit.</p>
     *
     * <p><strong>Note:</strong> there is a typo in the
     * <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/pack-spec.html#attribute-layout-definitions">official {@code numeral} definition</a>.
     * Parentheses should <strong>not</strong> be part of the numeral definition.</p>
     *
     * <pre>
     * numeral:
     *       ('-')? (digit)+
     * </pre>
     *
     * @return A number from the stream.
     * @throws Pack200Exception If the numeral is invalid or out of range.
     */
    private int readNumeral() throws Pack200Exception {
        // Determine if the number is negative
        final char first = peek();
        final boolean negative = first == '-';
        if (negative) {
            next();
        }
        // Read the number
        final char firstDigit = next();
        if (!Character.isDigit(firstDigit)) {
            throw new Pack200Exception("Corrupted Pack200 archive: numeral must start with a digit.");
        }
        long result = firstDigit - '0';
        while (!eof()) {
            if (!Character.isDigit(peek())) {
                break;
            }
            result = result * 10 + next() - '0';
            // Check for overflow
            if (result > Integer.MAX_VALUE) {
                throw new Pack200Exception("Corrupted Pack200 archive: numeral value out of range.");
            }
        }
        return (int) (negative ? -result : result);
    }

    /**
     * Reads a reference tag from the stream.
     *
     * @return A reference tag from the stream.
     * @throws Pack200Exception If the reference tag is invalid.
     */
    private String readReferenceTag() throws Pack200Exception {
        final char[] buf = new char[4];
        int len = 0;
        buf[len++] = next();
        buf[len++] = next();
        char c = next();
        buf[len++] = c;
        if (c == 'N') {
            c = next();
            buf[len++] = c;
        }
        return AttributeLayoutUtils.checkReferenceTag(new String(buf, 0, len));
    }

    /**
     * Reads a non default {@code union_case} from the stream
     *
     * <p>Reads a non default {@code union_case} or returns {@code null} if the default case {@code ()} is encountered.</p>
     *
     * <pre>
     * union_case:
     *       '(' union_case_tag (',' union_case_tag)* ')' '[' (body)? ']'
     * union_case_tag:
     *       ( numeral | numeral '-' numeral )
     * </pre>
     *
     * @return A UnionCase from the stream or {@code null} if the default case is encountered.
     * @throws Pack200Exception If the union case is invalid.
     */
    private UnionCaseData<T> readUnionCase() throws Pack200Exception {
        // Check for default case
        expect('(');
        char c = peek();
        if (c == ')') {
            // Default case
            p--;
            return null;
        }
        // Read the tag ranges
        final List<IntegerRange> tagRanges = new ArrayList<>();
        int nextTag;
        Integer startTag = null;
        do {
            nextTag = readNumeral();
            c = expect('-', ',', ')');
            if (startTag == null) { // First number of a range
                if (c == '-') {
                    startTag = nextTag;
                } else {
                    tagRanges.add(IntegerRange.of(nextTag, nextTag));
                }
            } else { // Second number of a range
                tagRanges.add(IntegerRange.of((int) startTag, nextTag));
                startTag = null;
            }
        } while (c != ')');
        // Read the body
        final List<T> body = readBody();
        return new UnionCaseData<>(tagRanges, body);
    }

    /**
     * Reads an {@code unsigned_int} layout definition from the stream.
     *
     * @return an {@code unsigned_int} layout definition from the stream.
     * @throws Pack200Exception If the definition is invalid.
     */
    private String readUnsignedInt() throws Pack200Exception {
        return AttributeLayoutUtils.checkUnsignedIntTag(String.valueOf(next()));
    }
}
