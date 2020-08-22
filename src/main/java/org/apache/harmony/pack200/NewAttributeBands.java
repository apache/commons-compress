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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.harmony.pack200.AttributeDefinitionBands.AttributeDefinition;
import org.objectweb.asm.Label;

/**
 * Set of bands relating to a non-predefined attribute that has had a layout
 * definition given to pack200 (e.g. via one of the -C, -M, -F or -D command
 * line options)
 */
public class NewAttributeBands extends BandSet {

    protected List attributeLayoutElements;
    private int[] backwardsCallCounts;
    private final CpBands cpBands;
    private final AttributeDefinition def;
    private boolean usedAtLeastOnce;

    // used when parsing
    private Integral lastPIntegral;

    public NewAttributeBands(int effort, CpBands cpBands, SegmentHeader header, AttributeDefinition def) throws IOException {
        super(effort, header);
        this.def = def;
        this.cpBands = cpBands;
        parseLayout();
    }

    public void addAttribute(NewAttribute attribute) {
        usedAtLeastOnce = true;
        InputStream stream = new ByteArrayInputStream(attribute.getBytes());
        for (Iterator iterator = attributeLayoutElements.iterator(); iterator.hasNext();) {
            AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
            layoutElement.addAttributeToBand(attribute, stream);
        }
    }

    public void pack(OutputStream out) throws IOException, Pack200Exception {
        for (Iterator iterator = attributeLayoutElements.iterator(); iterator.hasNext();) {
            AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
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
        String layout = def.layout.getUnderlyingString();
        if (attributeLayoutElements == null) {
            attributeLayoutElements = new ArrayList();
            StringReader stream = new StringReader(layout);
            AttributeLayoutElement e;
            while ((e = readNextAttributeElement(stream)) != null) {
                attributeLayoutElements.add(e);
            }
            resolveCalls();
        }
    }

    /**
     * Resolve calls in the attribute layout and returns the number of backwards
     * callables
     *
     * @param tokens -
     *            the attribute layout as a List of AttributeElements
     */
    private void resolveCalls() {
        for (int i = 0; i < attributeLayoutElements.size(); i++) {
            AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElements
                    .get(i);
            if (element instanceof Callable) {
                Callable callable = (Callable) element;
                List body = callable.body; // Look for calls in the body
                for (int iIndex = 0; iIndex < body.size(); iIndex++) {
                    LayoutElement layoutElement = (LayoutElement) body
                            .get(iIndex);
                    // Set the callable for each call
                    resolveCallsForElement(i, callable, layoutElement);
                }
            }
        }
        int backwardsCallableIndex = 0;
        for (int i = 0; i < attributeLayoutElements.size(); i++) {
            AttributeLayoutElement element = (AttributeLayoutElement) attributeLayoutElements
                    .get(i);
            if (element instanceof Callable) {
                Callable callable = (Callable) element;
                if(callable.isBackwardsCallable) {
                    callable.setBackwardsCallableIndex(backwardsCallableIndex);
                    backwardsCallableIndex++;
                }
            }
        }
        backwardsCallCounts = new int[backwardsCallableIndex];
    }

    private void resolveCallsForElement(int i,
            Callable currentCallable, LayoutElement layoutElement) {
        if (layoutElement instanceof Call) {
            Call call = (Call) layoutElement;
            int index = call.callableIndex;
            if (index == 0) { // Calls the parent callable
                call.setCallable(currentCallable);
            } else if (index > 0) { // Forwards call
                for (int k = i + 1; k < attributeLayoutElements.size(); k++) {
                    AttributeLayoutElement el = (AttributeLayoutElement) attributeLayoutElements
                            .get(k);
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
                    AttributeLayoutElement el = (AttributeLayoutElement) attributeLayoutElements
                            .get(k);
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
            List children = ((Replication)layoutElement).layoutElements;
            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
                LayoutElement object = (LayoutElement) iterator.next();
                resolveCallsForElement(i, currentCallable, object);
            }
        }
    }

    private AttributeLayoutElement readNextAttributeElement(StringReader stream)
            throws IOException {
        stream.mark(1);
        int nextChar = stream.read();
        if (nextChar == -1) {
            return null;
        }
        if (nextChar == '[') {
            List body = readBody(getStreamUpToMatchingBracket(stream));
            return new Callable(body);
        } else {
            stream.reset();
            return readNextLayoutElement(stream);
        }
    }

    private LayoutElement readNextLayoutElement(StringReader stream)
            throws IOException {
        int nextChar = stream.read();
        if (nextChar == -1) {
            return null;
        }

        switch (nextChar) {
        // Integrals
        case 'B':
        case 'H':
        case 'I':
        case 'V':
            return new Integral(new String(new char[] { (char)nextChar }));
        case 'S':
        case 'F':
            return new Integral(new String(new char[] { (char)nextChar,
                    (char) stream.read() }));
        case 'P':
            stream.mark(1);
            if (stream.read() != 'O') {
                stream.reset();
                lastPIntegral = new Integral("P" + (char) stream.read());
                return lastPIntegral;
            } else {
                lastPIntegral = new Integral("PO" + (char) stream.read(), lastPIntegral);
                return lastPIntegral;
            }
        case 'O':
            stream.mark(1);
            if (stream.read() != 'S') {
                stream.reset();
                return new Integral("O" + (char) stream.read(), lastPIntegral);
            } else {
                return new Integral("OS" + (char) stream.read(), lastPIntegral);
            }

            // Replication
        case 'N':
            char uint_type = (char) stream.read();
            stream.read(); // '['
            String str = readUpToMatchingBracket(stream);
            return new Replication("" + uint_type, str);

            // Union
        case 'T':
            String int_type = "" + (char) stream.read();
            if (int_type.equals("S")) {
                int_type += (char) stream.read();
            }
            List unionCases = new ArrayList();
            UnionCase c;
            while ((c = readNextUnionCase(stream)) != null) {
                unionCases.add(c);
            }
            stream.read(); // '('
            stream.read(); // ')'
            stream.read(); // '['
            List body = null;
            stream.mark(1);
            char next = (char) stream.read();
            if (next != ']') {
                stream.reset();
                body = readBody(getStreamUpToMatchingBracket(stream));
            }
            return new Union(int_type, unionCases, body);

            // Call
        case '(':
            int number = readNumber(stream).intValue();
            stream.read(); // ')'
            return new Call(number);
            // Reference
        case 'K':
        case 'R':
            String string = "" + (char)nextChar + (char) stream.read();
            char nxt = (char) stream.read();
            string += nxt;
            if (nxt == 'N') {
                string += (char) stream.read();
            }
            return new Reference(string);
        }
        return null;
    }

    /**
     * Read a UnionCase from the stream
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private UnionCase readNextUnionCase(StringReader stream) throws IOException {
        stream.mark(2);
        stream.read(); // '('
        char next = (char) stream.read();
        if (next == ')') {
            stream.reset();
            return null;
        } else {
            stream.reset();
            stream.read(); // '('
        }
        List tags = new ArrayList();
        Integer nextTag;
        do {
            nextTag = readNumber(stream);
            if(nextTag != null) {
                tags.add(nextTag);
                stream.read(); // ',' or ')'
            }
        } while (nextTag != null);
        stream.read(); // '['
        stream.mark(1);
        next = (char) stream.read();
        if (next == ']') {
            return new UnionCase(tags);
        } else {
            stream.reset();
            return new UnionCase(tags,
                    readBody(getStreamUpToMatchingBracket(stream)));
        }
    }

    /**
     * An AttributeLayoutElement is a part of an attribute layout and has one or
     * more bands associated with it, which transmit the AttributeElement data
     * for successive Attributes of this type.
     */
    public interface AttributeLayoutElement {

        public void addAttributeToBand(NewAttribute attribute, InputStream stream);

        public void pack(OutputStream out) throws IOException, Pack200Exception;

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets);

    }

    public abstract class LayoutElement implements AttributeLayoutElement {

        protected int getLength(char uint_type) {
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

        public Integral(String tag) {
            this.tag = tag;
            this.defaultCodec = getCodec(tag);
        }

        public Integral(String tag, Integral previousIntegral) {
            this.tag = tag;
            this.defaultCodec = getCodec(tag);
            this.previousIntegral = previousIntegral;
        }

        public String getTag() {
            return tag;
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
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
                char uint_type = tag.substring(2).toCharArray()[0];
                int length = getLength(uint_type);
                value = readInteger(length, stream);
                value += previousIntegral.previousPValue;
                val = attribute.getLabel(value);
                previousPValue = value;
            } else if (tag.startsWith("P")) {
                char uint_type = tag.substring(1).toCharArray()[0];
                int length = getLength(uint_type);
                value = readInteger(length, stream);
                val = attribute.getLabel(value);
                previousPValue = value;
            } else if (tag.startsWith("O")) {
                char uint_type = tag.substring(1).toCharArray()[0];
                int length = getLength(uint_type);
                value = readInteger(length, stream);
                value += previousIntegral.previousPValue;
                val = attribute.getLabel(value);
                previousPValue = value;
            }
            if(val == null) {
                val = new Integer(value);
            }
            band.add(val);
        }

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            PackingUtils.log("Writing new attribute bands...");
            byte[] encodedBand = encodeBandInt(tag, integerListToArray(band),
                    defaultCodec);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from "
                    + tag + "[" + band.size() + "]");
        }

        public int latestValue() {
            return ((Integer)band.get(band.size() - 1)).intValue();
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            if(tag.startsWith("O") || tag.startsWith("PO")) {
                renumberOffsetBci(previousIntegral.band, bciRenumbering, labelsToOffsets);
            } else if (tag.startsWith("P")) {
                for (int i = band.size() - 1; i >= 0; i--) {
                    Object label = band.get(i);
                    if (label instanceof Integer) {
                        break;
                    } else if (label instanceof Label) {
                        band.remove(i);
                        Integer bytecodeIndex = (Integer) labelsToOffsets
                                .get(label);
                        band.add(i, new Integer(bciRenumbering.get(bytecodeIndex
                                .intValue())));
                    }
                }
            }
        }

        private void renumberOffsetBci(List relative,
                IntList bciRenumbering, Map labelsToOffsets) {
            for (int i = band.size() - 1; i >= 0; i--) {
                Object label = band.get(i);
                if (label instanceof Integer) {
                    break;
                } else if (label instanceof Label) {
                    band.remove(i);
                    Integer bytecodeIndex = (Integer) labelsToOffsets.get(label);
                    Integer renumberedOffset = new Integer(bciRenumbering
                            .get(bytecodeIndex.intValue())
                            - ((Integer) relative.get(i)).intValue());
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

        public Replication(String tag, String contents) throws IOException {
            this.countElement = new Integral(tag);
            StringReader stream = new StringReader(contents);
            LayoutElement e;
            while ((e = readNextLayoutElement(stream)) != null) {
                layoutElements.add(e);
            }
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            countElement.addAttributeToBand(attribute, stream);
            int count = countElement.latestValue();
            for (int i = 0; i < count; i++) {
                for (Iterator iterator = layoutElements.iterator(); iterator.hasNext();) {
                    AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
                    layoutElement.addAttributeToBand(attribute, stream);
                }
            }
        }

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            countElement.pack(out);
            for (Iterator iterator = layoutElements.iterator(); iterator.hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
                layoutElement.pack(out);
            }
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            for (Iterator iterator = layoutElements.iterator(); iterator.hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
                layoutElement.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }
    }

    /**
     * A Union is a type of layout element where the tag value acts as a
     * selector for one of the union cases
     */
    public class Union extends LayoutElement {

        private final Integral unionTag;
        private final List unionCases;
        private final List defaultCaseBody;

        public Union(String tag, List unionCases, List body) {
            this.unionTag = new Integral(tag);
            this.unionCases = unionCases;
            this.defaultCaseBody = body;
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            unionTag.addAttributeToBand(attribute, stream);
            long tag = unionTag.latestValue();
            boolean defaultCase = true;
            for (int i = 0; i < unionCases.size(); i++) {
                UnionCase element = (UnionCase) unionCases.get(i);
                if (element.hasTag(tag)) {
                    defaultCase = false;
                    element.addAttributeToBand(attribute, stream);
                }
            }
            if (defaultCase) {
                for (int i = 0; i < defaultCaseBody.size(); i++) {
                    LayoutElement element = (LayoutElement) defaultCaseBody
                            .get(i);
                    element.addAttributeToBand(attribute, stream);
                }
            }
        }

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            unionTag.pack(out);
            for (Iterator iterator = unionCases.iterator(); iterator.hasNext();) {
                UnionCase unionCase = (UnionCase) iterator.next();
                unionCase.pack(out);
            }
            for (Iterator iterator = defaultCaseBody.iterator(); iterator
                    .hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator
                        .next();
                layoutElement.pack(out);
            }
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            for (Iterator iterator = unionCases.iterator(); iterator.hasNext();) {
                UnionCase unionCase = (UnionCase) iterator.next();
                unionCase.renumberBci(bciRenumbering, labelsToOffsets);
            }
            for (Iterator iterator = defaultCaseBody.iterator(); iterator
                    .hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator
                        .next();
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

        public Call(int callableIndex) {
            this.callableIndex = callableIndex;
        }

        public void setCallable(Callable callable) {
            this.callable = callable;
            if (callableIndex < 1) {
                callable.setBackwardsCallable();
            }
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            callable.addAttributeToBand(attribute, stream);
            if(callableIndex < 1) {
                callable.addBackwardsCall();
            }
        }

        public void pack(OutputStream out) {
            // do nothing here as pack will be called on the callable at another time
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
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

        public Reference(String tag) {
            this.tag = tag;
            nullsAllowed = tag.indexOf('N') != -1;
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            int index = readInteger(4, stream);
            if(tag.startsWith("RC")) { // Class
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

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            int[] ints;
            if(nullsAllowed) {
                ints = cpEntryOrNullListToArray(band);
            } else {
                ints = cpEntryListToArray(band);
            }
            byte[] encodedBand = encodeBandInt(tag, ints, Codec.UNSIGNED5);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from "
                    + tag + "[" + ints.length + "]");
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            // nothing to do here
        }

    }

    public class Callable implements AttributeLayoutElement {

        private final List body;

        private boolean isBackwardsCallable;

        private int backwardsCallableIndex;

        public Callable(List body) throws IOException {
            this.body = body;
        }

        public void setBackwardsCallableIndex(int backwardsCallableIndex) {
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

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            for (Iterator iterator = body.iterator(); iterator.hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
                layoutElement.addAttributeToBand(attribute, stream);
            }
        }

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            for (Iterator iterator = body.iterator(); iterator.hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
                layoutElement.pack(out);
            }
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            for (Iterator iterator = body.iterator(); iterator.hasNext();) {
                AttributeLayoutElement layoutElement = (AttributeLayoutElement) iterator.next();
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

        public UnionCase(List tags) {
            this.tags = tags;
            this.body = Collections.EMPTY_LIST;
        }

        public boolean hasTag(long l) {
            return tags.contains(new Integer((int) l));
        }

        public UnionCase(List tags, List body) throws IOException {
            this.tags = tags;
            this.body = body;
        }

        public void addAttributeToBand(NewAttribute attribute,
                InputStream stream) {
            for (int i = 0; i < body.size(); i++) {
                LayoutElement element = (LayoutElement) body.get(i);
                element.addAttributeToBand(attribute, stream);
            }
        }

        public void pack(OutputStream out) throws IOException, Pack200Exception {
            for (int i = 0; i < body.size(); i++) {
                LayoutElement element = (LayoutElement) body.get(i);
                element.pack(out);
            }
        }

        public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
            for (int i = 0; i < body.size(); i++) {
                LayoutElement element = (LayoutElement) body.get(i);
                element.renumberBci(bciRenumbering, labelsToOffsets);
            }
        }

        public List getBody() {
            return body;
        }
    }

    /**
     * Utility method to get the contents of the given stream, up to the next
     * ']', (ignoring pairs of brackets '[' and ']')
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private StringReader getStreamUpToMatchingBracket(StringReader stream)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        int foundBracket = -1;
        while (foundBracket != 0) {
            char c = (char) stream.read();
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

    private int readInteger(int i, InputStream stream) {
        int result = 0;
        for (int j = 0; j < i; j++) {
            try {
                result = result << 8 | stream.read();
            } catch (IOException e) {
                throw new RuntimeException("Error reading unknown attribute");
            }
        }
        // use casting to preserve sign
        if(i == 1) result = (byte) result;
        if(i == 2) result = (short) result;
        return result;
    }

    /**
     * Returns the {@link BHSDCodec} that should be used for the given layout
     * element
     *
     * @param layoutElement
     */
    private BHSDCodec getCodec(String layoutElement) {
        if (layoutElement.indexOf('O') >= 0) {
            return Codec.BRANCH5;
        } else if (layoutElement.indexOf('P') >= 0) {
            return Codec.BCI5;
        } else if (layoutElement.indexOf('S') >= 0 && layoutElement.indexOf("KS") < 0 //$NON-NLS-1$
                && layoutElement.indexOf("RS") < 0) { //$NON-NLS-1$
            return Codec.SIGNED5;
        } else if (layoutElement.indexOf('B') >= 0) {
            return Codec.BYTE1;
        } else {
            return Codec.UNSIGNED5;
        }
    }

    /**
     * Utility method to get the contents of the given stream, up to the next
     * ']', (ignoring pairs of brackets '[' and ']')
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private String readUpToMatchingBracket(StringReader stream)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        int foundBracket = -1;
        while (foundBracket != 0) {
            char c = (char) stream.read();
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
     * @throws IOException
     */
    private Integer readNumber(StringReader stream) throws IOException {
        stream.mark(1);
        char first = (char) stream.read();
        boolean negative = first == '-';
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
        if(length == 0) {
            return null;
        }
        char[] digits = new char[length];
        int read = stream.read(digits);
        if (read != digits.length) {
            throw new IOException("Error reading from the input stream");
        }
        return new Integer(Integer.parseInt((negative ? "-" : "") + new String(digits)));
    }

    /**
     * Read a 'body' section of the layout from the given stream
     *
     * @param stream
     * @return List of LayoutElements
     * @throws IOException
     */
    private List readBody(StringReader stream) throws IOException {
        List layoutElements = new ArrayList();
        LayoutElement e;
        while ((e = readNextLayoutElement(stream)) != null) {
            layoutElements.add(e);
        }
        return layoutElements;
    }

    /**
     * Renumber any bytecode indexes or offsets as described in section 5.5.2 of
     * the pack200 specification
     *
     * @param bciRenumbering
     * @param labelsToOffsets
     */
    public void renumberBci(IntList bciRenumbering, Map labelsToOffsets) {
        for (Iterator iterator = attributeLayoutElements.iterator(); iterator.hasNext();) {
            AttributeLayoutElement element = (AttributeLayoutElement) iterator.next();
            element.renumberBci(bciRenumbering, labelsToOffsets);
        }
    }

}
