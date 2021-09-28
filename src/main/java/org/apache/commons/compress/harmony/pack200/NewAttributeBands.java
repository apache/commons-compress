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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.harmony.pack200.AttributeDefinitionBands.AttributeDefinition;
import org.objectweb.asm.Label;

/**
 * Set of bands relating to a non-predefined attribute that has had a layout definition given to pack200 (e.g. via one
 * of the -C, -M, -F or -D command line options)
 */
public class NewAttributeBands extends BandSet {

    protected List attributeLayoutElements;
    private int[] backwardsCallCounts;
    private final CpBands cpBands;
    private final AttributeDefinition def;
    private boolean usedAtLeastOnce;

    // used when parsing
    private Integral lastPIntegral;

    public NewAttributeBands(final int effort, final CpBands cpBands, final SegmentHeader header,
        final AttributeDefinition def) throws IOException {
        super(effort, header);
        this.def = def;
        this.cpBands = cpBands;
        parseLayout();
    }

    public void addAttribute(final NewAttribute attribute) {
        usedAtLeastOnce = true;
        final InputStream stream = new ByteArrayInputStream(attribute.getBytes());
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement layoutElement = (AttributeLayoutElement) attributeLayoutElement;
            layoutElement.addAttributeToBand(attribute, stream);
        }
    }

    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement layoutElement = (AttributeLayoutElement) attributeLayoutElement;
            layoutElement.pack(out);
        }
    }

    public String getAttributeName() {
        return def.name.getUnderlyingString();
    }

    public int getFlagIndex() {
        return def.index;
    }

    public int[] numBackwardsCalls() {
        return backwardsCallCounts;
    }

    public boolean isUsedAtLeastOnce() {
        return usedAtLeastOnce;
    }

    private void parseLayout() throws IOException {
        final String layout = def.layout.getUnderlyingString();
        if (attributeLayoutElements == null) {
            attributeLayoutElements = new ArrayList();
            final StringReader stream = new StringReader(layout);
            AttributeLayoutElement e;
            while ((e = readNextAttributeElement(stream)) != null) {
                attributeLayoutElements.add(e);
            }
            resolveCalls();
        }
    }

    /**
     * Resolve calls in the attribute layout and returns the number of backwards callables
     *
     * @param tokens - the attribute layout as a List of AttributeElements
     */
    private void resolveCalls() {
        for (int i = 0; i < attributeLayoutElements.size(); i++) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElements.get(i);
            if (element instanceof Callable) {
                final Callable callable = (Callable) element;
                final List body = callable.body; // Look for calls in the body
                for (Object element2 : body) {
                    final LayoutElement layoutElement = (LayoutElement) element2;
                    // Set the callable for each call
                    resolveCallsForElement(i, callable, layoutElement);
                }
            }
        }
        int backwardsCallableIndex = 0;
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElement;
            if (element instanceof Callable) {
                final Callable callable = (Callable) element;
                if (callable.isBackwardsCallable) {
                    callable.setBackwardsCallableIndex(backwardsCallableIndex);
                    backwardsCallableIndex++;
                }
            }
        }
        backwardsCallCounts = new int[backwardsCallableIndex];
    }

    private void resolveCallsForElement(final int i, final Callable currentCallable,
        final LayoutElement layoutElement) {
        if (layoutElement instanceof Call) {
            final Call call = (Call) layoutElement;
            int index = call.callableIndex;
            if (index == 0) { // Calls the parent callable
                call.setCallable(currentCallable);
            } else if (index > 0) { // Forwards call
                for (int k = i + 1; k < attributeLayoutElements.size(); k++) {
                    final AttributeLayoutElement el = (AttributeLayoutElement) attributeLayoutElements.get(k);
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
                    final AttributeLayoutElement el = (AttributeLayoutElement) attributeLayoutElements.get(k);
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
            final List children = ((Replication) layoutElement).layoutElements;
            for (Object child : children) {
                final LayoutElement object = (LayoutElement) child;
                resolveCallsForElement(i, currentCallable, object);
            }
        }
    }

    private AttributeLayoutElement readNextAttributeElement(final StringReader stream) throws IOException {
        stream.mark(1);
        final int nextChar = stream.read();
        if (nextChar == -1) {
            return null;
        }
        if (nextChar == '[') {
            final List body = readBody(getStreamUpToMatchingBracket(stream));
            return new Callable(body);
        }
        stream.reset();
        return readNextLayoutElement(stream);
    }

    private LayoutElement readNextLayoutElement(final StringReader stream) throws IOException {
        final int nextChar = stream.read();
        if (nextChar == -1) {
            return null;
        }

        switch (nextChar) {
        // Integrals
        case 'B':
        case 'H':
        case 'I':
        case 'V':
            return new Integral(new String(new char[] {(char) nextChar}));
        case 'S':
        case 'F':
            return new Integral(new String(new char[] {(char) nextChar, (char) stream.read()}));
        case 'P':
            stream.mark(1);
            if (stream.read() != 'O') {
                stream.reset();
                lastPIntegral = new Integral("P" + (char) stream.read());
                return lastPIntegral;
            }
            lastPIntegral = new Integral("PO" + (char) stream.read(), lastPIntegral);
            return lastPIntegral;
        case 'O':
            stream.mark(1);
            if (stream.read() != 'S') {
                stream.reset();
                return new Integral("O" + (char) stream.read(), lastPIntegral);
            }
            return new Integral("OS" + (char) stream.read(), lastPIntegral);

            // Replication
        case 'N':
            final char uint_type = (char) stream.read();
            stream.read(); // '['
            final String str = readUpToMatchingBracket(stream);
            return new Replication("" + uint_type, str);

        // Union
        case 'T':
            String int_type = "" + (char) stream.read();
            if (int_type.equals("S")) {
                int_type += (char) stream.read();
            }
            final List unionCases = new ArrayList();
            UnionCase c;
            while ((c = readNextUnionCase(stream)) != null) {
                unionCases.add(c);
            }
            stream.read(); // '('
            stream.read(); // ')'
            stream.read(); // '['
            List body = null;
            stream.mark(1);
            final char next = (char) stream.read();
            if (next != ']') {
                stream.reset();
                body = readBody(getStreamUpToMatchingBracket(stream));
            }
            return new Union(int_type, unionCases, body);

        // Call
        case '(':
            final int number = readNumber(stream).intValue();
            stream.read(); // ')'
            return new Call(number);
        // Reference
        case 'K':
        case 'R':
            final StringBuilder string = new StringBuilder("").append((char) nextChar).append((char) stream.read());
            final char nxt = (char) stream.read();
            string.append(nxt);
            if (nxt == 'N') {
                string.append((char) stream.read());
            }
            return new Reference(string.toString());
        }
        return null;
    }

    /**
     * Read a UnionCase from the stream
     *
     * @param stream
     * @return
     * @throws IOException If an I/O error occurs.
     */
    private UnionCase readNextUnionCase(final StringReader stream) throws IOException {
        stream.mark(2);
        stream.read(); // '('
        char next = (char) stream.read();
        if (next == ')') {
            stream.reset();
            return null;
        }
        stream.reset();
        stream.read(); // '('
        final List tags = new ArrayList();
        Integer nextTag;
        do {
            nextTag = readNumber(stream);
            if (nextTag != null) {
                tags.add(nextTag);
                stream.read(); // ',' or ')'
            }
        } while (nextTag != null);
        stream.read(); // '['
        stream.mark(1);
        next = (char) stream.read();
        if (next == ']') {
            return new UnionCase(tags);
        }
        stream.reset();
        return new UnionCase(tags, readBody(getStreamUpToMatchingBracket(stream)));
    }

    /**
     * An AttributeLayoutElement is a part of an attribute layout and has one or more bands associated with it, which
     * transmit the AttributeElement data for successive Attributes of this type.
     */
    public interface AttributeLayoutElement {

        void addAttributeToBand(NewAttribute attribute, InputStream stream);

        void pack(OutputStream out) throws IOException, Pack200Exception;

        void renumberBci(IntList bciRenumbering, Map labelsToOffsets);

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

    public class Integral extends LayoutElement {

        private final String tag;

        private final List band = new ArrayList();
        private final BHSDCodec defaultCodec;

        // used for bytecode offsets (OH and POH)
        private Integral previousIntegral;
        private int previousPValue;

        public Integral(final String tag) {
            this.tag = tag;
            this.defaultCodec = getCodec(tag);
        }

        public Integral(final String tag, final Integral previousIntegral) {
            this.tag = tag;
            this.defaultCodec = getCodec(tag);
            this.previousIntegral = previousIntegral;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            Object val = null;
            int value = 0;
            if (tag.equals("B") || tag.equals("FB")) {
                value = readInteger(1, stream) & 0xFF; // unsigned byte
            } else if (tag.equals("SB")) {
                value = readInteger(1, stream);
            } else if (tag.equals("H") || tag.equals("FH")) {
                value = readInteger(2, stream) & 0xFFFF; // unsigned short
            } else if (tag.equals("SH")) {
                value = readInteger(2, stream);
            } else if (tag.equals("I") || tag.equals("FI")) {
                value = readInteger(4, stream);
            } else if (tag.equals("SI")) {
                value = readInteger(4, stream);
            } else if (tag.equals("V") || tag.equals("FV") || tag.equals("SV")) {
                // Not currently supported
            } else if (tag.startsWith("PO") || tag.startsWith("OS")) {
                final char uint_type = tag.substring(2).toCharArray()[0];
                final int length = getLength(uint_type);
                value = readInteger(length, stream);
                value += previousIntegral.previousPValue;
                val = attribute.getLabel(value);
                previousPValue = value;
            } else if (tag.startsWith("P")) {
                final char uint_type = tag.substring(1).toCharArray()[0];
                final int length = getLength(uint_type);
                value = readInteger(length, stream);
                val = attribute.getLabel(value);
                previousPValue = value;
            } else if (tag.startsWith("O")) {
                final char uint_type = tag.substring(1).toCharArray()[0];
                final int length = getLength(uint_type);
                value = readInteger(length, stream);
                value += previousIntegral.previousPValue;
                val = attribute.getLabel(value);
                previousPValue = value;
            }
            if (val == null) {
                val = Integer.valueOf(value);
            }
            band.add(val);
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            PackingUtils.log("Writing new attribute bands...");
            final byte[] encodedBand = encodeBandInt(tag, integerListToArray(band), defaultCodec);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from " + tag + "[" + band.size() + "]");
        }

        public int latestValue() {
            return ((Integer) band.get(band.size() - 1)).intValue();
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
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
                        final Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                        band.add(i, Integer.valueOf(bciRenumbering.get(bytecodeIndex.intValue())));
                    }
                }
            }
        }

        private void renumberOffsetBci(final List relative, final IntList bciRenumbering, final Map labelsToOffsets) {
            for (int i = band.size() - 1; i >= 0; i--) {
                final Object label = band.get(i);
                if (label instanceof Integer) {
                    break;
                }
                if (label instanceof Label) {
                    band.remove(i);
                    final Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                    final Integer renumberedOffset = Integer
                        .valueOf(bciRenumbering.get(bytecodeIndex.intValue()) - ((Integer) relative.get(i)).intValue());
                    band.add(i, renumberedOffset);
                }
            }
        }

    }

    /**
     * A replication is an array of layout elements, with an associated count
     */
    public class Replication extends LayoutElement {

        private final Integral countElement;

        private final List layoutElements = new ArrayList();

        public Integral getCountElement() {
            return countElement;
        }

        public List getLayoutElements() {
            return layoutElements;
        }

        public Replication(final String tag, final String contents) throws IOException {
            this.countElement = new Integral(tag);
            final StringReader stream = new StringReader(contents);
            LayoutElement e;
            while ((e = readNextLayoutElement(stream)) != null) {
                layoutElements.add(e);
            }
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            countElement.addAttributeToBand(attribute, stream);
            final int count = countElement.latestValue();
            for (int i = 0; i < count; i++) {
                for (Object layoutElement2 : layoutElements) {
                    final AttributeLayoutElement layoutElement = (AttributeLayoutElement) layoutElement2;
                    layoutElement.addAttributeToBand(attribute, stream);
                }
            }
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            countElement.pack(out);
            for (Object layoutElement2 : layoutElements) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) layoutElement2;
                layoutElement.pack(out);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            for (Object layoutElement2 : layoutElements) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) layoutElement2;
                layoutElement.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }
    }

    /**
     * A Union is a type of layout element where the tag value acts as a selector for one of the union cases
     */
    public class Union extends LayoutElement {

        private final Integral unionTag;
        private final List unionCases;
        private final List defaultCaseBody;

        public Union(final String tag, final List unionCases, final List body) {
            this.unionTag = new Integral(tag);
            this.unionCases = unionCases;
            this.defaultCaseBody = body;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            unionTag.addAttributeToBand(attribute, stream);
            final long tag = unionTag.latestValue();
            boolean defaultCase = true;
            for (Object element2 : unionCases) {
                final UnionCase element = (UnionCase) element2;
                if (element.hasTag(tag)) {
                    defaultCase = false;
                    element.addAttributeToBand(attribute, stream);
                }
            }
            if (defaultCase) {
                for (Object element2 : defaultCaseBody) {
                    final LayoutElement element = (LayoutElement) element2;
                    element.addAttributeToBand(attribute, stream);
                }
            }
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            unionTag.pack(out);
            for (Object element : unionCases) {
                final UnionCase unionCase = (UnionCase) element;
                unionCase.pack(out);
            }
            for (Object element : defaultCaseBody) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) element;
                layoutElement.pack(out);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            for (Object element : unionCases) {
                final UnionCase unionCase = (UnionCase) element;
                unionCase.renumberBci(bciRenumbering, labelsToOffsets);
            }
            for (Object element : defaultCaseBody) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) element;
                layoutElement.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }

        public Integral getUnionTag() {
            return unionTag;
        }

        public List getUnionCases() {
            return unionCases;
        }

        public List getDefaultCaseBody() {
            return defaultCaseBody;
        }
    }

    public class Call extends LayoutElement {

        private final int callableIndex;
        private Callable callable;

        public Call(final int callableIndex) {
            this.callableIndex = callableIndex;
        }

        public void setCallable(final Callable callable) {
            this.callable = callable;
            if (callableIndex < 1) {
                callable.setBackwardsCallable();
            }
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            callable.addAttributeToBand(attribute, stream);
            if (callableIndex < 1) {
                callable.addBackwardsCall();
            }
        }

        @Override
        public void pack(final OutputStream out) {
            // do nothing here as pack will be called on the callable at another time
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            // do nothing here as renumberBci will be called on the callable at another time
        }

        public int getCallableIndex() {
            return callableIndex;
        }

        public Callable getCallable() {
            return callable;
        }
    }

    /**
     * Constant Pool Reference
     */
    public class Reference extends LayoutElement {

        private final String tag;

        private List band;

        private boolean nullsAllowed = false;

        public Reference(final String tag) {
            this.tag = tag;
            nullsAllowed = tag.indexOf('N') != -1;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            final int index = readInteger(4, stream);
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
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            int[] ints;
            if (nullsAllowed) {
                ints = cpEntryOrNullListToArray(band);
            } else {
                ints = cpEntryListToArray(band);
            }
            final byte[] encodedBand = encodeBandInt(tag, ints, Codec.UNSIGNED5);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from " + tag + "[" + ints.length + "]");
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            // nothing to do here
        }

    }

    public class Callable implements AttributeLayoutElement {

        private final List body;

        private boolean isBackwardsCallable;

        private int backwardsCallableIndex;

        public Callable(final List body) {
            this.body = body;
        }

        public void setBackwardsCallableIndex(final int backwardsCallableIndex) {
            this.backwardsCallableIndex = backwardsCallableIndex;
        }

        public void addBackwardsCall() {
            backwardsCallCounts[backwardsCallableIndex]++;
        }

        public boolean isBackwardsCallable() {
            return isBackwardsCallable;
        }

        /**
         * Tells this Callable that it is a backwards callable
         */
        public void setBackwardsCallable() {
            this.isBackwardsCallable = true;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            for (Object element : body) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) element;
                layoutElement.addAttributeToBand(attribute, stream);
            }
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            for (Object element : body) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) element;
                layoutElement.pack(out);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            for (Object element : body) {
                final AttributeLayoutElement layoutElement = (AttributeLayoutElement) element;
                layoutElement.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }

        public List getBody() {
            return body;
        }
    }

    /**
     * A Union case
     */
    public class UnionCase extends LayoutElement {

        private final List body;

        private final List tags;

        public UnionCase(final List tags) {
            this.tags = tags;
            this.body = Collections.EMPTY_LIST;
        }

        public boolean hasTag(final long l) {
            return tags.contains(Integer.valueOf((int) l));
        }

        public UnionCase(final List tags, final List body) {
            this.tags = tags;
            this.body = body;
        }

        @Override
        public void addAttributeToBand(final NewAttribute attribute, final InputStream stream) {
            for (Object element2 : body) {
                final LayoutElement element = (LayoutElement) element2;
                element.addAttributeToBand(attribute, stream);
            }
        }

        @Override
        public void pack(final OutputStream out) throws IOException, Pack200Exception {
            for (Object element2 : body) {
                final LayoutElement element = (LayoutElement) element2;
                element.pack(out);
            }
        }

        @Override
        public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
            for (Object element2 : body) {
                final LayoutElement element = (LayoutElement) element2;
                element.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }

        public List getBody() {
            return body;
        }
    }

    /**
     * Utility method to get the contents of the given stream, up to the next ']', (ignoring pairs of brackets '[' and
     * ']')
     *
     * @param stream
     * @return
     * @throws IOException If an I/O error occurs.
     */
    private StringReader getStreamUpToMatchingBracket(final StringReader stream) throws IOException {
        final StringBuffer sb = new StringBuffer();
        int foundBracket = -1;
        while (foundBracket != 0) {
            final char c = (char) stream.read();
            if (c == ']') {
                foundBracket++;
            }
            if (c == '[') {
                foundBracket--;
            }
            if (!(foundBracket == 0)) {
                sb.append(c);
            }
        }
        return new StringReader(sb.toString());
    }

    private int readInteger(final int i, final InputStream stream) {
        int result = 0;
        for (int j = 0; j < i; j++) {
            try {
                result = result << 8 | stream.read();
            } catch (final IOException e) {
                throw new RuntimeException("Error reading unknown attribute");
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
        if (layoutElement.indexOf('S') >= 0 && layoutElement.indexOf("KS") < 0 //$NON-NLS-1$
            && layoutElement.indexOf("RS") < 0) { //$NON-NLS-1$
            return Codec.SIGNED5;
        }
        if (layoutElement.indexOf('B') >= 0) {
            return Codec.BYTE1;
        }
        return Codec.UNSIGNED5;
    }

    /**
     * Utility method to get the contents of the given stream, up to the next ']', (ignoring pairs of brackets '[' and
     * ']')
     *
     * @param stream
     * @return
     * @throws IOException If an I/O error occurs.
     */
    private String readUpToMatchingBracket(final StringReader stream) throws IOException {
        final StringBuffer sb = new StringBuffer();
        int foundBracket = -1;
        while (foundBracket != 0) {
            final char c = (char) stream.read();
            if (c == ']') {
                foundBracket++;
            }
            if (c == '[') {
                foundBracket--;
            }
            if (!(foundBracket == 0)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Read a number from the stream and return it
     *
     * @param stream
     * @return
     * @throws IOException If an I/O error occurs.
     */
    private Integer readNumber(final StringReader stream) throws IOException {
        stream.mark(1);
        final char first = (char) stream.read();
        final boolean negative = first == '-';
        if (!negative) {
            stream.reset();
        }
        stream.mark(100);
        int i;
        int length = 0;
        while ((i = (stream.read())) != -1 && Character.isDigit((char) i)) {
            length++;
        }
        stream.reset();
        if (length == 0) {
            return null;
        }
        final char[] digits = new char[length];
        final int read = stream.read(digits);
        if (read != digits.length) {
            throw new IOException("Error reading from the input stream");
        }
        return Integer.valueOf(Integer.parseInt((negative ? "-" : "") + new String(digits)));
    }

    /**
     * Read a 'body' section of the layout from the given stream
     *
     * @param stream
     * @return List of LayoutElements
     * @throws IOException If an I/O error occurs.
     */
    private List readBody(final StringReader stream) throws IOException {
        final List layoutElements = new ArrayList();
        LayoutElement e;
        while ((e = readNextLayoutElement(stream)) != null) {
            layoutElements.add(e);
        }
        return layoutElements;
    }

    /**
     * Renumber any bytecode indexes or offsets as described in section 5.5.2 of the pack200 specification
     *
     * @param bciRenumbering TODO
     * @param labelsToOffsets TODO
     */
    public void renumberBci(final IntList bciRenumbering, final Map labelsToOffsets) {
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElement;
            element.renumberBci(bciRenumbering, labelsToOffsets);
        }
    }

}
