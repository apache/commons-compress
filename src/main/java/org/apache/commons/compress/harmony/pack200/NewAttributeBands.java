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
package org.apache.commons.compress.harmony.pack200;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.harmony.internal.AttributeLayoutParser;
import org.apache.commons.compress.harmony.internal.AttributeLayoutParser.UnionCaseData;
import org.apache.commons.compress.harmony.internal.AttributeLayoutUtils;
import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands.AttributeDefinition;
import org.apache.commons.lang3.IntegerRange;
import org.objectweb.asm.Label;

/**
 * Sets of bands relating to a non-predefined attribute that has had a layout definition given to pack200 (for example via one of the -C, -M, -F or -D command
 * line options)
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class NewAttributeBands extends BandSet {

    /**
     * An AttributeLayoutElement is a part of an attribute layout and has one or more bands associated with it, which transmit the AttributeElement data for
     * successive Attributes of this type.
     */
    public interface AttributeLayoutElement {

        void addAttributeToBand(NewAttribute attribute, InputStream inputStream);

        void pack(OutputStream ouputStream) throws IOException, Pack200Exception;

        void renumberBci(IntList bciRenumbering, Map<Label, Integer> labelsToOffsets);

    }

    private class AttributeLayoutFactory implements AttributeLayoutParser.Factory<LayoutElement> {

        /**
         * The last P-type integral seen (for use with subsequent O and PO types)
         */
        private Integral lastPIntegral;

        @Override
        public LayoutElement createCall(int callableIndex) {
            return new Call(callableIndex);
        }

        @Override
        public LayoutElement createCallable(List<LayoutElement> body) throws Pack200Exception {
            return new Callable(body);
        }

        @Override
        public LayoutElement createIntegral(String tag) {
            final Integral integral;
            if (tag.startsWith("O") || tag.startsWith("PO")) {
                integral = new Integral(tag, lastPIntegral);
            } else {
                integral = new Integral(tag);
            }
            if (tag.startsWith("P")) {
                lastPIntegral = integral;
            }
            return integral;
        }

        @Override
        public LayoutElement createReference(String tag) {
            return new Reference(tag);
        }

        @Override
        public LayoutElement createReplication(String unsignedInt, List<LayoutElement> body) throws Pack200Exception {
            return new Replication(unsignedInt, body);
        }

        @Override
        public LayoutElement createUnion(String anyInt, List<UnionCaseData<LayoutElement>> cases, List<LayoutElement> body) throws Pack200Exception {
            final List<UnionCase> unionCases = new ArrayList<>();
            for (final UnionCaseData<LayoutElement> unionCaseData : cases) {
                unionCases.add(new UnionCase(unionCaseData.tagRanges, unionCaseData.body, false));
            }
            return new Union(anyInt, unionCases, body);
        }
    }

    public class Call extends LayoutElement {

        private final int callableIndex;
        private Callable callable;

        public Call(final int callableIndex) {
            this.callableIndex = callableIndex;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            callable.addAttributeToBand(attribute, inputStream);
            if (callableIndex < 1) {
                callable.addBackwardsCall();
            }
        }

        public Callable getCallable() {
            return callable;
        }

        public int getCallableIndex() {
            return callableIndex;
        }

        @Override
        public void pack(final OutputStream outputStream) {
            // do nothing here as pack will be called on the callable at another time
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            // do nothing here as renumberBci will be called on the callable at another time
        }

        public void setCallable(final Callable callable) {
            this.callable = callable;
            if (callableIndex < 1) {
                callable.setBackwardsCallable();
            }
        }
    }

    public class Callable extends LayoutElement {

        private final List<LayoutElement> body;

        private boolean isBackwardsCallable;

        private int backwardsCallableIndex;

        /**
         * Constructs a new Callable layout element with the given body.
         *
         * @param body the body of the callable.
         * @throws Pack200Exception If the body is empty.
         */
        public Callable(final List<LayoutElement> body) throws Pack200Exception {
            if (body.isEmpty()) {
                throw new Pack200Exception("Corrupted Pack200 archive: Callable body is empty");
            }
            this.body = body;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            for (final AttributeLayoutElement element : body) {
                element.addAttributeToBand(attribute, inputStream);
            }
        }

        public void addBackwardsCall() {
            backwardsCallCounts[backwardsCallableIndex]++;
        }

        public List<LayoutElement> getBody() {
            return body;
        }

        public boolean isBackwardsCallable() {
            return isBackwardsCallable;
        }

        @Override
        public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
            for (final AttributeLayoutElement element : body) {
                element.pack(outputStream);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            for (final AttributeLayoutElement element : body) {
                element.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }

        /**
         * Tells this Callable that it is a backwards callable
         */
        public void setBackwardsCallable() {
            this.isBackwardsCallable = true;
        }

        public void setBackwardsCallableIndex(final int backwardsCallableIndex) {
            this.backwardsCallableIndex = backwardsCallableIndex;
        }
    }

    public class Integral extends LayoutElement {

        private final String tag;

        private final List band = new ArrayList();
        private final BHSDCodec defaultCodec;

        // used for bytecode offsets (OH and POH)
        private Integral previousIntegral;
        private int previousPValue;

        /**
         * Constructs a new Integral layout element with the given tag.
         *
         * @param tag The tag.
         * @throws IllegalArgumentException If the tag is invalid.
         */
        public Integral(final String tag) {
            this.tag = AttributeLayoutUtils.checkIntegralTag(tag);
            this.defaultCodec = getCodec(tag);
        }

        /**
         * Constructs a new Integral layout element with the given tag.
         *
         * @param tag The tag.
         * @param previousIntegral The previous integral (for PO and OS types).
         * @throws IllegalArgumentException If the tag is invalid.
         */
        public Integral(final String tag, final Integral previousIntegral) {
            this.tag = AttributeLayoutUtils.checkIntegralTag(tag);
            this.defaultCodec = getCodec(tag);
            this.previousIntegral = previousIntegral;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            Object val = null;
            int value = 0;
            switch (tag) {
            case "B":
            case "FB":
                value = readInteger(1, inputStream) & 0xFF; // unsigned byte
                break;
            case "SB":
                value = readInteger(1, inputStream);
                break;
            case "H":
            case "FH":
                value = readInteger(2, inputStream) & 0xFFFF; // unsigned short
                break;
            case "SH":
                value = readInteger(2, inputStream);
                break;
            case "I":
            case "FI":
            case "SI":
                value = readInteger(4, inputStream);
                break;
            case "V":
            case "FV":
            case "SV":
                break;
            default:
                if (tag.startsWith("PO") || tag.startsWith("OS")) {
                    final char uint_type = tag.substring(2).toCharArray()[0];
                    final int length = getLength(uint_type);
                    value = readInteger(length, inputStream);
                    value += previousIntegral.previousPValue;
                    val = attribute.getLabel(value);
                    previousPValue = value;
                } else if (tag.startsWith("P")) {
                    final char uint_type = tag.substring(1).toCharArray()[0];
                    final int length = getLength(uint_type);
                    value = readInteger(length, inputStream);
                    val = attribute.getLabel(value);
                    previousPValue = value;
                } else if (tag.startsWith("O")) {
                    final char uint_type = tag.substring(1).toCharArray()[0];
                    final int length = getLength(uint_type);
                    value = readInteger(length, inputStream);
                    value += previousIntegral.previousPValue;
                    val = attribute.getLabel(value);
                    previousPValue = value;
                }
                break;
            }
            if (val == null) {
                val = Integer.valueOf(value);
            }
            band.add(val);
        }

        public String getTag() {
            return tag;
        }

        public int latestValue() {
            return ((Integer) band.get(band.size() - 1)).intValue();
        }

        @Override
        public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
            PackingUtils.log("Writing new attribute bands...");
            final byte[] encodedBand = encodeBandInt(tag, integerListToArray(band), defaultCodec);
            outputStream.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from " + tag + "[" + band.size() + "]");
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            if (tag.startsWith("O") || tag.startsWith("PO")) {
                renumberOffsetBci(previousIntegral.band, bciRenumbering, labelsToOffsets);
            } else if (tag.startsWith("P")) {
                for (int i = band.size() - 1; i >= 0; i--) {
                    final Object label = band.get(i);
                    if (label instanceof Integer) {
                        break;
                    }
                    if (label instanceof Label) {
                        band.remove(i);
                        final Integer bytecodeIndex = labelsToOffsets.get(label);
                        band.add(i, Integer.valueOf(bciRenumbering.get(bytecodeIndex.intValue())));
                    }
                }
            }
        }

        private void renumberOffsetBci(final List relative, final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            for (int i = band.size() - 1; i >= 0; i--) {
                final Object label = band.get(i);
                if (label instanceof Integer) {
                    break;
                }
                if (label instanceof Label) {
                    band.remove(i);
                    final Integer bytecodeIndex = labelsToOffsets.get(label);
                    final Integer renumberedOffset = Integer.valueOf(bciRenumbering.get(bytecodeIndex.intValue()) - ((Integer) relative.get(i)).intValue());
                    band.add(i, renumberedOffset);
                }
            }
        }

    }

    public abstract class LayoutElement implements AttributeLayoutElement {

        protected int getLength(final char uint_type) {
            int length = 0;
            switch (uint_type) {
            case 'B':
                length = 1;
                break;
            case 'H':
                length = 2;
                break;
            case 'I':
                length = 4;
                break;
            case 'V':
                length = 0;
                break;
            }
            return length;
        }
    }

    /**
     * Constant Pool Reference
     */
    public class Reference extends LayoutElement {

        private final String tag;

        private final List<ConstantPoolEntry> band = new ArrayList<>();

        private final boolean nullsAllowed;

        /**
         * Constructs a new Reference layout element with the given tag.
         *
         * @param tag The tag.
         * @throws IllegalArgumentException If the tag is invalid.
         */
        public Reference(final String tag) {
            this.tag = AttributeLayoutUtils.checkReferenceTag(tag);
            nullsAllowed = tag.indexOf('N') != -1;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            final int index = readInteger(4, inputStream);
            if (tag.startsWith("RC")) { // Class
                band.add(cpBands.getCPClass(attribute.readClass(index)));
            } else if (tag.startsWith("RU")) { // UTF8 String
                band.add(cpBands.getCPUtf8(attribute.readUTF8(index)));
            } else if (tag.startsWith("RS")) { // Signature
                band.add(cpBands.getCPSignature(attribute.readUTF8(index)));
            } else { // Constant
                band.add(cpBands.getConstant(attribute.readConst(index)));
            }
            // TODO method and field references
        }

        public String getTag() {
            return tag;
        }

        @Override
        public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
            final int[] ints;
            if (nullsAllowed) {
                ints = cpEntryOrNullListToArray(band);
            } else {
                ints = cpEntryListToArray(band);
            }
            final byte[] encodedBand = encodeBandInt(tag, ints, Codec.UNSIGNED5);
            outputStream.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from " + tag + "[" + ints.length + "]");
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            // nothing to do here
        }

    }

    /**
     * A replication is an array of layout elements, with an associated count
     */
    public class Replication extends LayoutElement {

        private final Integral countElement;

        private final List<LayoutElement> layoutElements;

        /**
         * Constructs a new Replication layout element.
         *
         * @param tag the tag of the Integral element.
         * @param contents the contents of the replication.
         * @throws IllegalArgumentException If the tag is invalid or the contents are empty.
         * @throws Pack200Exception If the contents are invalid.
         */
        public Replication(final String tag, final String contents) throws Pack200Exception {
            this(tag, AttributeLayoutUtils.readBody(contents, attributeLayoutFactory));
        }

        private Replication(final String tag, final List<LayoutElement> contents) throws Pack200Exception {
            this.countElement = new Integral(tag);
            this.layoutElements = contents;
            if (layoutElements.isEmpty()) {
                throw new Pack200Exception("Corrupted Pack200 archive: Replication body is empty");
            }
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            countElement.addAttributeToBand(attribute, inputStream);
            final int count = countElement.latestValue();
            for (int i = 0; i < count; i++) {
                for (final AttributeLayoutElement layoutElement : layoutElements) {
                    layoutElement.addAttributeToBand(attribute, inputStream);
                }
            }
        }

        public Integral getCountElement() {
            return countElement;
        }

        public List<LayoutElement> getLayoutElements() {
            return layoutElements;
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            countElement.pack(out);
            for (final AttributeLayoutElement layoutElement : layoutElements) {
                layoutElement.pack(out);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            for (final AttributeLayoutElement layoutElement : layoutElements) {
                layoutElement.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }
    }

    /**
     * A Union is a type of layout element where the tag value acts as a selector for one of the union cases
     */
    public class Union extends LayoutElement {

        private final Integral unionTag;
        private final List<UnionCase> unionCases;
        private final List<LayoutElement> defaultCaseBody;

        /**
         * Constructs a new Union layout element.
         *
         * @param tag the tag of the Integral element.
         * @param unionCases the union cases.
         * @param body the default case body.
         * @throws IllegalArgumentException If the tag is invalid.
         */
        public Union(final String tag, final List<UnionCase> unionCases, final List<LayoutElement> body) {
            this.unionTag = new Integral(tag);
            this.unionCases = unionCases;
            this.defaultCaseBody = body;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            unionTag.addAttributeToBand(attribute, inputStream);
            final long tag = unionTag.latestValue();
            boolean defaultCase = true;
            for (final UnionCase unionCase : unionCases) {
                if (unionCase.hasTag(tag)) {
                    defaultCase = false;
                    unionCase.addAttributeToBand(attribute, inputStream);
                }
            }
            if (defaultCase) {
                for (final LayoutElement layoutElement : defaultCaseBody) {
                    layoutElement.addAttributeToBand(attribute, inputStream);
                }
            }
        }

        public List<LayoutElement> getDefaultCaseBody() {
            return defaultCaseBody;
        }

        public List<UnionCase> getUnionCases() {
            return unionCases;
        }

        public Integral getUnionTag() {
            return unionTag;
        }

        @Override
        public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
            unionTag.pack(outputStream);
            for (final UnionCase unionCase : unionCases) {
                unionCase.pack(outputStream);
            }
            for (final LayoutElement element : defaultCaseBody) {
                element.pack(outputStream);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            for (final UnionCase unionCase : unionCases) {
                unionCase.renumberBci(bciRenumbering, labelsToOffsets);
            }
            for (final LayoutElement element : defaultCaseBody) {
                element.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }
    }

    /**
     * A Union case
     */
    public class UnionCase extends LayoutElement {

        private final List<IntegerRange> tagRanges;
        private final List<LayoutElement> body;

        public UnionCase(final List<Integer> tags) {
            this(tags, Collections.emptyList());
        }

        public UnionCase(final List<Integer> tags, final List<LayoutElement> body) {
            this(AttributeLayoutUtils.toRanges(tags), body, false);
        }

        private UnionCase(final List<IntegerRange> tagRanges, final List<LayoutElement> body, final boolean ignored) {
            this.tagRanges = tagRanges;
            this.body = body;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream inputStream) {
            for (final LayoutElement element : body) {
                element.addAttributeToBand(attribute, inputStream);
            }
        }

        public List<LayoutElement> getBody() {
            return body;
        }

        public boolean hasTag(final long l) {
            return AttributeLayoutUtils.unionCaseMatches(tagRanges, (int) l);
        }

        @Override
        public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
            for (final LayoutElement element : body) {
                element.pack(outputStream);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
            for (final LayoutElement element : body) {
                element.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }
    }

    protected List<LayoutElement> attributeLayoutElements;

    private int[] backwardsCallCounts;

    private final CpBands cpBands;

    private final AttributeDefinition def;

    private boolean usedAtLeastOnce;

    private final AttributeLayoutFactory attributeLayoutFactory = new AttributeLayoutFactory();

    public NewAttributeBands(final int effort, final CpBands cpBands, final SegmentHeader header, final AttributeDefinition def) throws IOException {
        super(effort, header);
        this.def = def;
        this.cpBands = cpBands;
        parseLayout();
    }

    public void addAttribute(final NewAttribute attribute) {
        usedAtLeastOnce = true;
        final InputStream stream = new ByteArrayInputStream(attribute.getBytes());
        for (final AttributeLayoutElement attributeLayoutElement : attributeLayoutElements) {
            attributeLayoutElement.addAttributeToBand(attribute, stream);
        }
    }

    public String getAttributeName() {
        return def.name.getUnderlyingString();
    }

    /**
     * Returns the {@link BHSDCodec} that should be used for the given layout element
     *
     * @param layoutElement
     */
    private BHSDCodec getCodec(final String layoutElement) {
        if (layoutElement.indexOf('O') >= 0) {
            return Codec.BRANCH5;
        }
        if (layoutElement.indexOf('P') >= 0) {
            return Codec.BCI5;
        }
        if (layoutElement.indexOf('S') >= 0 && !layoutElement.contains("KS") //$NON-NLS-1$
                && !layoutElement.contains("RS")) { //$NON-NLS-1$
            return Codec.SIGNED5;
        }
        if (layoutElement.indexOf('B') >= 0) {
            return Codec.BYTE1;
        }
        return Codec.UNSIGNED5;
    }

    public int getFlagIndex() {
        return def.index;
    }

    public boolean isUsedAtLeastOnce() {
        return usedAtLeastOnce;
    }

    public int[] numBackwardsCalls() {
        return backwardsCallCounts;
    }

    @Override
    public void pack(final OutputStream outputStream) throws IOException, Pack200Exception {
        for (final AttributeLayoutElement attributeLayoutElement : attributeLayoutElements) {
            attributeLayoutElement.pack(outputStream);
        }
    }

    private void parseLayout() throws IOException {
        final String layout = def.layout.getUnderlyingString();
        if (attributeLayoutElements == null) {
            attributeLayoutElements = AttributeLayoutUtils.readAttributeLayout(layout, attributeLayoutFactory);
            resolveCalls();
        }
    }

    private int readInteger(final int i, final InputStream inputStream) {
        int result = 0;
        for (int j = 0; j < i; j++) {
            try {
                result = result << 8 | inputStream.read();
            } catch (final IOException e) {
                throw new UncheckedIOException("Error reading unknown attribute", e);
            }
        }
        // use casting to preserve sign
        if (i == 1) {
            result = (byte) result;
        }
        if (i == 2) {
            result = (short) result;
        }
        return result;
    }

    /**
     * Renumber any bytecode indexes or offsets as described in section 5.5.2 of the pack200 specification
     *
     * @param bciRenumbering  TODO
     * @param labelsToOffsets TODO
     */
    public void renumberBci(final IntList bciRenumbering, final Map<Label, Integer> labelsToOffsets) {
        for (final AttributeLayoutElement attributeLayoutElement : attributeLayoutElements) {
            attributeLayoutElement.renumberBci(bciRenumbering, labelsToOffsets);
        }
    }

    /**
     * Resolve calls in the attribute layout and returns the number of backwards callables
     */
    private void resolveCalls() {
        for (int i = 0; i < attributeLayoutElements.size(); i++) {
            final AttributeLayoutElement element = attributeLayoutElements.get(i);
            if (element instanceof Callable) {
                final Callable callable = (Callable) element;
                final List<LayoutElement> body = callable.body; // Look for calls in the body
                for (final LayoutElement layoutElement : body) {
                    // Set the callable for each call
                    resolveCallsForElement(i, callable, layoutElement);
                }
            }
        }
        int backwardsCallableIndex = 0;
        for (final AttributeLayoutElement attributeLayoutElement : attributeLayoutElements) {
            if (attributeLayoutElement instanceof Callable) {
                final Callable callable = (Callable) attributeLayoutElement;
                if (callable.isBackwardsCallable) {
                    callable.setBackwardsCallableIndex(backwardsCallableIndex);
                    backwardsCallableIndex++;
                }
            }
        }
        backwardsCallCounts = new int[backwardsCallableIndex];
    }

    private void resolveCallsForElement(final int i, final Callable currentCallable, final LayoutElement layoutElement) {
        if (layoutElement instanceof Call) {
            final Call call = (Call) layoutElement;
            int index = call.callableIndex;
            if (index == 0) { // Calls the parent callable
                call.setCallable(currentCallable);
            } else if (index > 0) { // Forwards call
                for (int k = i + 1; k < attributeLayoutElements.size(); k++) {
                    final AttributeLayoutElement el = attributeLayoutElements.get(k);
                    if (el instanceof Callable) {
                        index--;
                        if (index == 0) {
                            call.setCallable((Callable) el);
                            break;
                        }
                    }
                }
            } else { // Backwards call
                for (int k = i - 1; k >= 0; k--) {
                    final AttributeLayoutElement el = attributeLayoutElements.get(k);
                    if (el instanceof Callable) {
                        index++;
                        if (index == 0) {
                            call.setCallable((Callable) el);
                            break;
                        }
                    }
                }
            }
        } else if (layoutElement instanceof Replication) {
            final List<LayoutElement> children = ((Replication) layoutElement).layoutElements;
            for (final LayoutElement child : children) {
                resolveCallsForElement(i, currentCallable, child);
            }
        }
    }

}
