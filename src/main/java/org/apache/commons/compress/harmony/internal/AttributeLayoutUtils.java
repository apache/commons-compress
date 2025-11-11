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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.lang3.Range;

public final class AttributeLayoutUtils {

    /**
     * <pre>
     * integral:
     *       ( unsigned_int | signed_int | bc_index | bc_offset | flag )
     * signed_int:
     *       'S' uint_type
     * any_int:
     *       ( unsigned_int | signed_int )
     * bc_index:
     *       ( 'P' uint_type | 'PO' uint_type )
     * bc_offset:
     *       'O' any_int
     * flag:
     *       'F' uint_type
     * uint_type:
     *       ( 'B' | 'H' | 'I' | 'V' )
     * </pre>
     */
    private static final Set<String> INTEGRAL_TAGS;

    /**
     * <pre>
     * reference:
     *       reference_type ( 'N' )? uint_type
     * reference_type:
     *       ( constant_ref | schema_ref | utf8_ref | untyped_ref )
     * constant_ref:
     *       ( 'KI' | 'KJ' | 'KF' | 'KD' | 'KS' | 'KQ' | 'KM' | 'KT' | 'KL' )
     * schema_ref:
     *       ( 'RC' | 'RS' | 'RD' | 'RF' | 'RM' | 'RI' | 'RY' | 'RB' | 'RN' )
     * utf8_ref:
     *       'RU'
     * untyped_ref:
     *       'RQ'
     * uint_type:
     *       ( 'B' | 'H' | 'I' | 'V' )
     * </pre>
     */
    private static final Set<String> REFERENCE_TAGS;

    /**
     * <pre>
     * uint_type:
     *       ( 'B' | 'H' | 'I' | 'V' )
     * </pre>
     */
    private static final Set<String> UNSIGNED_INT_TAGS;

    static {
        final Set<String> unsignedIntTags = new HashSet<>();
        Collections.addAll(unsignedIntTags,
                // unsigned_int
                "B", "H", "I", "V");
        UNSIGNED_INT_TAGS = Collections.unmodifiableSet(unsignedIntTags);
        final Set<String> integralTags = new HashSet<>();
        UNSIGNED_INT_TAGS.forEach(tag -> Collections.addAll(integralTags,
                // unsigned_int
                tag,
                // signed_int
                "S" + tag,
                // bc_index
                "P" + tag, "PO" + tag,
                // bc_offset
                "O" + tag, "OS" + tag,
                // flag
                "F" + tag)); INTEGRAL_TAGS = Collections.unmodifiableSet(integralTags);
        final Set<String> referenceTags = new HashSet<>();
        Collections.addAll(referenceTags,
                // constant_ref
                "KI", "KJ", "KF", "KD", "KS", "KQ", "KM", "KT", "KL",
                // schema_ref
                "RC", "RS", "RD", "RF", "RM", "RI", "RY", "RB", "RN",
                // utf8_ref
                "RU",
                // untyped_ref
                "RQ");
        REFERENCE_TAGS = Collections.unmodifiableSet(referenceTags);
    }

    /**
     * Validates that the given tag matches the {@code unsigned_int} layout definition production rule.
     *
     * @param tag the layout tag to validate
     * @return the validated tag
     * @throws IllegalArgumentException if the tag is invalid
     */
    static String checkAnyIntTag(final String tag) {
        if (UNSIGNED_INT_TAGS.contains(tag) || tag.startsWith("S") && UNSIGNED_INT_TAGS.contains(tag.substring(1))) {
            return tag;
        }
        throw new IllegalArgumentException("Invalid unsigned int layout tag: " + tag);
    }

    /**
     * Validates that the given tag matches the {@code integral} layout definition production rule.
     *
     * @param tag the layout tag to validate
     * @return the validated tag
     * @throws IllegalArgumentException if the tag is invalid
     */
    public static String checkIntegralTag(final String tag) {
        if (INTEGRAL_TAGS.contains(tag)) {
            return tag;
        }
        throw new IllegalArgumentException("Invalid integral layout tag: " + tag);
    }

    /**
     * Validates that the given tag matches the {@code reference} layout definition production rule.
     *
     * @param tag the layout tag to validate
     * @return the validated tag
     * @throws IllegalArgumentException if the tag is invalid
     */
    public static String checkReferenceTag(final String tag) {
        if (tag.length() >= 3) {
            final String baseTag = tag.substring(0, 2);
            final String uintType = tag.substring(tag.length() - 1);
            if (REFERENCE_TAGS.contains(baseTag) && UNSIGNED_INT_TAGS.contains(uintType)
                    && (tag.length() == 3 || tag.length() == 4 && tag.charAt(2) == 'N')) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Invalid reference layout tag: " + tag);
    }

    /**
     * Validates that the given tag matches the {@code unsigned_int} layout definition production rule.
     *
     * @param tag the layout tag to validate
     * @return the validated tag
     * @throws IllegalArgumentException if the tag is invalid
     */
    static String checkUnsignedIntTag(final String tag) {
        if (UNSIGNED_INT_TAGS.contains(tag)) {
            return tag;
        }
        throw new IllegalArgumentException("Invalid unsigned int layout tag: " + tag);
    }

    /**
     * Reads a {@code attribute_layout} from the stream.
     *
     * <p>The returned list <strong>may</strong> be empty if the stream is empty.</p>
     *
     * <pre>
     * attribute_layout:
     *       ( layout_element )* | ( callable )+
     * </pre>
     *
     * @param definition the attribute layout definition body.
     * @param factory the factory to create AttributeLayoutElements.
     * @param <ALE> the type of AttributeLayoutElement.
     * @param <LE> the type of LayoutElement.
     * @return not empty list of LayoutElements from the body or, if the stream is empty, an empty list.
     * @throws Pack200Exception If the layout definition is invalid.
     */
    public static <ALE, LE extends ALE> List<ALE> readAttributeLayout(final String definition,
            final AttributeLayoutParser.Factory<ALE, LE> factory) throws Pack200Exception {
        final List<ALE> layoutElements = new ArrayList<>();
        final AttributeLayoutParser<ALE, LE> parser = new AttributeLayoutParser<>(definition, factory);
        ALE e;
        while ((e = parser.readAttributeLayoutElement()) != null) {
            layoutElements.add(e);
        }
        return layoutElements;
    }

    /**
     * Reads a {@code body} from the stream.
     *
     * <p>The returned list <strong>may</strong> be empty if the stream is empty.</p>
     *
     * <pre>
     * body:
     *       ( layout_element )+
     * </pre>
     *
     * @param body the attribute layout definition body.
     * @param factory the factory to create LayoutElements.
     * @param <ALE> the type of AttributeLayoutElement.
     * @param <LE> the type of LayoutElement.
     * @return not empty list of LayoutElements from the body or, if the stream is empty, an empty list.
     * @throws Pack200Exception If the layout definition is invalid.
     */
    public static <ALE, LE extends ALE> List<LE> readBody(final String body, final AttributeLayoutParser.Factory<ALE, LE> factory) throws Pack200Exception {
        final List<LE> layoutElements = new ArrayList<>();
        final AttributeLayoutParser<ALE, LE> parser = new AttributeLayoutParser<>(body, factory);
        LE e;
        while ((e = parser.readLayoutElement()) != null) {
            layoutElements.add(e);
        }
        return layoutElements;
    }

    /**
     * Converts a list of integers to a list of ranges where each range represents a single integer.
     *
     * @param tags the list of integer tags
     * @return a list of ranges representing the tags
     */
    public static List<Range<Integer>> toRanges(final List<Integer> tags) {
        return tags.stream().map(n -> Range.of(n, n)).collect(Collectors.toList());
    }

    /**
     * Checks if any of the given tag ranges contains the specified tag.
     *
     * @param tagRanges the list of tag ranges
     * @param tag       the tag to check
     * @return {@code true} if any range contains the tag, {@code false} otherwise
     */
    public static boolean unionCaseMatches(final List<Range<Integer>> tagRanges, final int tag) {
        return tagRanges.stream().anyMatch(r -> r.contains(tag));
    }

    private AttributeLayoutUtils() {
        // Utility class
    }
}
