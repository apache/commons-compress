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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.BHSDCodec;
import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.Attribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPClass;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPDouble;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFieldRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPFloat;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInteger;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPInterfaceMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPLong;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPMethodRef;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPNameAndType;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPString;
import org.apache.commons.compress.harmony.unpack200.bytecode.CPUTF8;
import org.apache.commons.compress.harmony.unpack200.bytecode.NewAttribute;

/**
 * Set of bands relating to a non-predefined attribute
 */
public class NewAttributeBands extends BandSet {

    private final AttributeLayout attributeLayout;

    private int backwardsCallCount;

    protected List attributeLayoutElements;

    public NewAttributeBands(final Segment segment, final AttributeLayout attributeLayout) throws IOException {
        super(segment);
        this.attributeLayout = attributeLayout;
        parseLayout();
        attributeLayout.setBackwardsCallCount(backwardsCallCount);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        // does nothing - use parseAttributes instead
    }

    /**
     * Parse the bands relating to this AttributeLayout and return the correct class file attributes as a List of
     * {@link Attribute}.
     *
     * @param in parse source.
     * @param occurrenceCount TODO
     * @return Class file attributes as a List of {@link Attribute}.
     * @throws IOException If an I/O error occurs.
     * @throws Pack200Exception TODO
     */
    public List parseAttributes(final InputStream in, final int occurrenceCount) throws IOException, Pack200Exception {
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElement;
            element.readBands(in, occurrenceCount);
        }

        final List attributes = new ArrayList(occurrenceCount);
        for (int i = 0; i < occurrenceCount; i++) {
            attributes.add(getOneAttribute(i, attributeLayoutElements));
        }
        return attributes;
    }

    /**
     * Get one attribute at the given index from the various bands. The correct bands must have already been read in.
     *
     * @param index TODO
     * @param elements TODO
     * @return attribute at the given index.
     */
    private Attribute getOneAttribute(final int index, final List elements) {
        final NewAttribute attribute = new NewAttribute(segment.getCpBands().cpUTF8Value(attributeLayout.getName()),
            attributeLayout.getIndex());
        for (Object element2 : elements) {
            final AttributeLayoutElement element = (AttributeLayoutElement) element2;
            element.addToAttribute(index, attribute);
        }
        return attribute;
    }

    /**
     * Tokenise the layout into AttributeElements
     *
     * @throws IOException If an I/O error occurs.
     */
    private void parseLayout() throws IOException {
        if (attributeLayoutElements == null) {
            attributeLayoutElements = new ArrayList();
            final StringReader stream = new StringReader(attributeLayout.getLayout());
            AttributeLayoutElement e;
            while ((e = readNextAttributeElement(stream)) != null) {
                attributeLayoutElements.add(e);
            }
            resolveCalls();
        }
    }

    /**
     * Resolve calls in the attribute layout and returns the number of backwards calls
     */
    private void resolveCalls() {
        int backwardsCalls = 0;
        for (int i = 0; i < attributeLayoutElements.size(); i++) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElements.get(i);
            if (element instanceof Callable) {
                final Callable callable = (Callable) element;
                if (i == 0) {
                    callable.setFirstCallable(true);
                }
                final List body = callable.body; // Look for calls in the body
                for (Object element2 : body) {
                    final LayoutElement layoutElement = (LayoutElement) element2;
                    // Set the callable for each call
                    backwardsCalls += resolveCallsForElement(i, callable, layoutElement);
                }
            }
        }
        backwardsCallCount = backwardsCalls;
    }

    private int resolveCallsForElement(final int i, final Callable currentCallable, final LayoutElement layoutElement) {
        int backwardsCalls = 0;
        if (layoutElement instanceof Call) {
            final Call call = (Call) layoutElement;
            int index = call.callableIndex;
            if (index == 0) { // Calls the parent callable
                backwardsCalls++;
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
                backwardsCalls++;
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
                backwardsCalls += resolveCallsForElement(i, currentCallable, object);
            }
        }
        return backwardsCalls;
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
                return new Integral("P" + (char) stream.read());
            }
            return new Integral("PO" + (char) stream.read());
        case 'O':
            stream.mark(1);
            if (stream.read() != 'S') {
                stream.reset();
                return new Integral("O" + (char) stream.read());
            }
            return new Integral("OS" + (char) stream.read());

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
     * Read a UnionCase from the stream.
     *
     * @param stream source stream.
     * @return A UnionCase from the stream.
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
    private interface AttributeLayoutElement {

        /**
         * Read the bands associated with this part of the layout.
         *
         * @param in TODO
         * @param count TODO
         * @throws Pack200Exception Bad archive.
         * @throws IOException If an I/O error occurs.
         */
        void readBands(InputStream in, int count) throws IOException, Pack200Exception;

        /**
         * Adds the band data for this element at the given index to the attribute.
         *
         * @param index Index position to add the attribute.
         * @param attribute The attribute to add.
         */
        void addToAttribute(int index, NewAttribute attribute);

    }

    private abstract class LayoutElement implements AttributeLayoutElement {

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

        private int[] band;

        public Integral(final String tag) {
            this.tag = tag;
        }

        @Override
        public void readBands(final InputStream in, final int count) throws IOException, Pack200Exception {
            band = decodeBandInt(attributeLayout.getName() + "_" + tag, in, getCodec(tag), count);
        }

        @Override
        public void addToAttribute(final int n, final NewAttribute attribute) {
            int value = band[n];
            if (tag.equals("B") || tag.equals("FB")) {
                attribute.addInteger(1, value);
            } else if (tag.equals("SB")) {
                attribute.addInteger(1, (byte) value);
            } else if (tag.equals("H") || tag.equals("FH")) {
                attribute.addInteger(2, value);
            } else if (tag.equals("SH")) {
                attribute.addInteger(2, (short) value);
            } else if (tag.equals("I") || tag.equals("FI")) {
                attribute.addInteger(4, value);
            } else if (tag.equals("SI")) {
                attribute.addInteger(4, value);
            } else if (tag.equals("V") || tag.equals("FV") || tag.equals("SV")) {
                // Don't add V's - they shouldn't be written out to the class
                // file
            } else if (tag.startsWith("PO")) {
                final char uint_type = tag.substring(2).toCharArray()[0];
                final int length = getLength(uint_type);
                attribute.addBCOffset(length, value);
            } else if (tag.startsWith("P")) {
                final char uint_type = tag.substring(1).toCharArray()[0];
                final int length = getLength(uint_type);
                attribute.addBCIndex(length, value);
            } else if (tag.startsWith("OS")) {
                final char uint_type = tag.substring(2).toCharArray()[0];
                final int length = getLength(uint_type);
                if (length == 1) {
                    value = (byte) value;
                } else if (length == 2) {
                    value = (short) value;
                } else if (length == 4) {
                    value = value;
                }
                attribute.addBCLength(length, value);
            } else if (tag.startsWith("O")) {
                final char uint_type = tag.substring(1).toCharArray()[0];
                final int length = getLength(uint_type);
                attribute.addBCLength(length, value);
            }
        }

        int getValue(final int index) {
            return band[index];
        }

        public String getTag() {
            return tag;
        }

    }

    /**
     * A replication is an array of layout elements, with an associated count
     */
    public class Replication extends LayoutElement {

        private final Integral countElement;

        private final List layoutElements = new ArrayList();

        public Replication(final String tag, final String contents) throws IOException {
            this.countElement = new Integral(tag);
            final StringReader stream = new StringReader(contents);
            LayoutElement e;
            while ((e = readNextLayoutElement(stream)) != null) {
                layoutElements.add(e);
            }
        }

        @Override
        public void readBands(final InputStream in, final int count) throws IOException, Pack200Exception {
            countElement.readBands(in, count);
            int arrayCount = 0;
            for (int i = 0; i < count; i++) {
                arrayCount += countElement.getValue(i);
            }
            for (Object layoutElement : layoutElements) {
                final LayoutElement element = (LayoutElement) layoutElement;
                element.readBands(in, arrayCount);
            }
        }

        @Override
        public void addToAttribute(final int index, final NewAttribute attribute) {
            // Add the count value
            countElement.addToAttribute(index, attribute);

            // Add the corresponding array values
            int offset = 0;
            for (int i = 0; i < index; i++) {
                offset += countElement.getValue(i);
            }
            final long numElements = countElement.getValue(index);
            for (int i = offset; i < offset + numElements; i++) {
                for (Object layoutElement : layoutElements) {
                    final LayoutElement element = (LayoutElement) layoutElement;
                    element.addToAttribute(i, attribute);
                }
            }
        }

        public Integral getCountElement() {
            return countElement;
        }

        public List getLayoutElements() {
            return layoutElements;
        }
    }

    /**
     * A Union is a type of layout element where the tag value acts as a selector for one of the union cases
     */
    public class Union extends LayoutElement {

        private final Integral unionTag;
        private final List unionCases;
        private final List defaultCaseBody;
        private int[] caseCounts;
        private int defaultCount;

        public Union(final String tag, final List unionCases, final List body) {
            this.unionTag = new Integral(tag);
            this.unionCases = unionCases;
            this.defaultCaseBody = body;
        }

        @Override
        public void readBands(final InputStream in, final int count) throws IOException, Pack200Exception {
            unionTag.readBands(in, count);
            final int[] values = unionTag.band;
            // Count the band size for each union case then read the bands
            caseCounts = new int[unionCases.size()];
            for (int i = 0; i < caseCounts.length; i++) {
                final UnionCase unionCase = (UnionCase) unionCases.get(i);
                for (int value : values) {
                    if (unionCase.hasTag(value)) {
                        caseCounts[i]++;
                    }
                }
                unionCase.readBands(in, caseCounts[i]);
            }
            // Count number of default cases then read the default bands
            for (int value : values) {
                boolean found = false;
                for (Object element : unionCases) {
                    final UnionCase unionCase = (UnionCase) element;
                    if (unionCase.hasTag(value)) {
                        found = true;
                    }
                }
                if (!found) {
                    defaultCount++;
                }
            }
            if (defaultCaseBody != null) {
                for (Object element2 : defaultCaseBody) {
                    final LayoutElement element = (LayoutElement) element2;
                    element.readBands(in, defaultCount);
                }
            }
        }

        @Override
        public void addToAttribute(final int n, final NewAttribute attribute) {
            unionTag.addToAttribute(n, attribute);
            int offset = 0;
            final int[] tagBand = unionTag.band;
            final int tag = unionTag.getValue(n);
            boolean defaultCase = true;
            for (Object element2 : unionCases) {
                final UnionCase element = (UnionCase) element2;
                if (element.hasTag(tag)) {
                    defaultCase = false;
                    for (int j = 0; j < n; j++) {
                        if (element.hasTag(tagBand[j])) {
                            offset++;
                        }
                    }
                    element.addToAttribute(offset, attribute);
                }
            }
            if (defaultCase) {
                // default case
                int defaultOffset = 0;
                for (int j = 0; j < n; j++) {
                    boolean found = false;
                    for (Object element2 : unionCases) {
                        final UnionCase element = (UnionCase) element2;
                        if (element.hasTag(tagBand[j])) {
                            found = true;
                        }
                    }
                    if (!found) {
                        defaultOffset++;
                    }
                }
                if (defaultCaseBody != null) {
                    for (Object element2 : defaultCaseBody) {
                        final LayoutElement element = (LayoutElement) element2;
                        element.addToAttribute(defaultOffset, attribute);
                    }
                }
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
        public void readBands(final InputStream in, final int count) {
            /*
             * We don't read anything here, but we need to pass the extra count to the callable if it's a forwards call.
             * For backwards callables the count is transmitted directly in the attribute bands and so it is added
             * later.
             */
            if (callableIndex > 0) {
                callable.addCount(count);
            }
        }

        @Override
        public void addToAttribute(final int n, final NewAttribute attribute) {
            callable.addNextToAttribute(attribute);
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

        private Object band;

        private final int length;

        public Reference(final String tag) {
            this.tag = tag;
            length = getLength(tag.charAt(tag.length() - 1));
        }

        @Override
        public void readBands(final InputStream in, final int count) throws IOException, Pack200Exception {
            if (tag.startsWith("KI")) { // Integer
                band = parseCPIntReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("KJ")) { // Long
                band = parseCPLongReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("KF")) { // Float
                band = parseCPFloatReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("KD")) { // Double
                band = parseCPDoubleReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("KS")) { // String
                band = parseCPStringReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RC")) { // Class
                band = parseCPClassReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RS")) { // Signature
                band = parseCPSignatureReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RD")) { // Descriptor
                band = parseCPDescriptorReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RF")) { // Field Reference
                band = parseCPFieldRefReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RM")) { // Method Reference
                band = parseCPMethodRefReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RI")) { // Interface Method Reference
                band = parseCPInterfaceMethodRefReferences(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            } else if (tag.startsWith("RU")) { // UTF8 String
                band = parseCPUTF8References(attributeLayout.getName(), in, Codec.UNSIGNED5, count);
            }
        }

        @Override
        public void addToAttribute(final int n, final NewAttribute attribute) {
            if (tag.startsWith("KI")) { // Integer
                attribute.addToBody(length, ((CPInteger[]) band)[n]);
            } else if (tag.startsWith("KJ")) { // Long
                attribute.addToBody(length, ((CPLong[]) band)[n]);
            } else if (tag.startsWith("KF")) { // Float
                attribute.addToBody(length, ((CPFloat[]) band)[n]);
            } else if (tag.startsWith("KD")) { // Double
                attribute.addToBody(length, ((CPDouble[]) band)[n]);
            } else if (tag.startsWith("KS")) { // String
                attribute.addToBody(length, ((CPString[]) band)[n]);
            } else if (tag.startsWith("RC")) { // Class
                attribute.addToBody(length, ((CPClass[]) band)[n]);
            } else if (tag.startsWith("RS")) { // Signature
                attribute.addToBody(length, ((CPUTF8[]) band)[n]);
            } else if (tag.startsWith("RD")) { // Descriptor
                attribute.addToBody(length, ((CPNameAndType[]) band)[n]);
            } else if (tag.startsWith("RF")) { // Field Reference
                attribute.addToBody(length, ((CPFieldRef[]) band)[n]);
            } else if (tag.startsWith("RM")) { // Method Reference
                attribute.addToBody(length, ((CPMethodRef[]) band)[n]);
            } else if (tag.startsWith("RI")) { // Interface Method Reference
                attribute.addToBody(length, ((CPInterfaceMethodRef[]) band)[n]);
            } else if (tag.startsWith("RU")) { // UTF8 String
                attribute.addToBody(length, ((CPUTF8[]) band)[n]);
            }
        }

        public String getTag() {
            return tag;
        }

    }

    public static class Callable implements AttributeLayoutElement {

        private final List body;

        private boolean isBackwardsCallable;

        private boolean isFirstCallable;

        public Callable(final List body) {
            this.body = body;
        }

        private int count;
        private int index;

        /**
         * Used by calls when adding band contents to attributes so they don't have to keep track of the internal index
         * of the callable.
         *
         * @param attribute TODO
         */
        public void addNextToAttribute(final NewAttribute attribute) {
            for (Object element2 : body) {
                final LayoutElement element = (LayoutElement) element2;
                element.addToAttribute(index, attribute);
            }
            index++;
        }

        /**
         * Adds the count of a call to this callable (ie the number of calls)
         *
         * @param count TODO
         */
        public void addCount(final int count) {
            this.count += count;
        }

        @Override
        public void readBands(final InputStream in, int count) throws IOException, Pack200Exception {
            if (isFirstCallable) {
                count += this.count;
            } else {
                count = this.count;
            }
            for (Object element2 : body) {
                final LayoutElement element = (LayoutElement) element2;
                element.readBands(in, count);
            }
        }

        @Override
        public void addToAttribute(final int n, final NewAttribute attribute) {
            if (isFirstCallable) {
                // Ignore n because bands also contain element parts from calls
                for (Object element2 : body) {
                    final LayoutElement element = (LayoutElement) element2;
                    element.addToAttribute(index, attribute);
                }
                index++;
            }
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

        public void setFirstCallable(final boolean isFirstCallable) {
            this.isFirstCallable = isFirstCallable;
        }

        public List getBody() {
            return body;
        }
    }

    /**
     * A Union case
     */
    public class UnionCase extends LayoutElement {

        private List body;

        private final List tags;

        public UnionCase(final List tags) {
            this.tags = tags;
        }

        public boolean hasTag(final int i) {
            return tags.contains(Integer.valueOf(i));
        }

        public boolean hasTag(final long l) {
            return tags.contains(Integer.valueOf((int) l));
        }

        public UnionCase(final List tags, final List body) {
            this.tags = tags;
            this.body = body;
        }

        @Override
        public void readBands(final InputStream in, final int count) throws IOException, Pack200Exception {
            if (body != null) {
                for (Object element2 : body) {
                    final LayoutElement element = (LayoutElement) element2;
                    element.readBands(in, count);
                }
            }
        }

        @Override
        public void addToAttribute(final int index, final NewAttribute attribute) {
            if (body != null) {
                for (Object element2 : body) {
                    final LayoutElement element = (LayoutElement) element2;
                    element.addToAttribute(index, attribute);
                }
            }
        }

        public List getBody() {
            return body == null ? Collections.EMPTY_LIST : body;
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

    /**
     * Returns the {@link BHSDCodec} that should be used for the given layout element.
     *
     * @param layoutElement TODO
     * @return the {@link BHSDCodec} that should be used for the given layout element.
     */
    public BHSDCodec getCodec(final String layoutElement) {
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
     * Gets the contents of the given stream, up to the next ']', (ignoring pairs of brackets '[' and ']')
     *
     * @param stream input stream.
     * @return the contents of the given stream.
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

    public int getBackwardsCallCount() {
        return backwardsCallCount;
    }

    /**
     * Once the attribute bands have been read the callables can be informed about the number of times each is subject
     * to a backwards call. This method is used to set this information.
     *
     * @param backwardsCalls one int for each backwards callable, which contains the number of times that callable is
     *        subject to a backwards call.
     * @throws IOException If an I/O error occurs.
     */
    public void setBackwardsCalls(final int[] backwardsCalls) throws IOException {
        int index = 0;
        parseLayout();
        for (Object attributeLayoutElement : attributeLayoutElements) {
            final AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElement;
            if (element instanceof Callable && ((Callable) element).isBackwardsCallable()) {
                ((Callable) element).addCount(backwardsCalls[index]);
                index++;
            }
        }
    }

    @Override
    public void unpack() throws IOException, Pack200Exception {

    }

}