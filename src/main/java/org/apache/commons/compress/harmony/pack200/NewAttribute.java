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

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * NewAttribute extends {@code Attribute} and manages unknown attributes encountered by ASM that have had a layout definition given to pack200 (for example via
 * one of the -C, -M, -F or -D command line options)
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class NewAttribute extends Attribute {

    /**
     * ErrorAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had an error action specified to pack200 (for example via
     * one of the -C, -M, -F or -D command line options such as -Cattribute-name=error)
     */
    public static class ErrorAttribute extends NewAttribute {

        /**
         * Constructs a new ErrorAttribute.
         *
         * @param type the attribute type.
         * @param context the context.
         */
        public ErrorAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf, final int codeOff, final Label[] labels) {
            throw new IllegalStateException("Attribute " + type + " was found");
        }
    }

    /**
     * PassAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had a pass action specified to pack200 (for example via
     * one of the -C, -M, -F or -D command line options such as -Cattribute-name=pass)
     */
    public static class PassAttribute extends NewAttribute {

        /**
         * Constructs a new PassAttribute.
         *
         * @param type the attribute type.
         * @param context the context.
         */
        public PassAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf, final int codeOff, final Label[] labels) {
            throw new Segment.PassException();
        }
    }

    /**
     * StripAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had a strip action specified to pack200 (for example via
     * one of the -C, -M, -F or -D command line options such as -Cattribute-name=strip)
     */
    public static class StripAttribute extends NewAttribute {

        /**
         * Constructs a new StripAttribute.
         *
         * @param type the attribute type.
         * @param context the context.
         */
        public StripAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf, final int codeOff, final Label[] labels) {
            // TODO Not sure if this works, can we really strip an attribute if we don't know the layout?
            return null;
        }
    }

    private boolean contextClass;
    private boolean contextMethod;
    private boolean contextField;
    private boolean contextCode;
    private final String layout;
    private byte[] contents;
    private int codeOff;
    private Label[] labels;
    private ClassReader classReader;
    private char[] buf;

    /**
     * Constructs a new NewAttribute with class reader and full details.
     *
     * @param classReader the class reader.
     * @param type the attribute type.
     * @param layout the attribute layout.
     * @param contents the attribute contents.
     * @param buf the character buffer.
     * @param codeOff the code offset.
     * @param labels the labels.
     */
    public NewAttribute(final ClassReader classReader, final String type, final String layout, final byte[] contents, final char[] buf, final int codeOff,
            final Label[] labels) {
        super(type);
        this.classReader = classReader;
        this.contents = contents;
        this.layout = layout;
        this.codeOff = codeOff;
        this.labels = labels;
        this.buf = buf;
    }

    /**
     * Constructs a new NewAttribute.
     *
     * @param type the attribute type.
     * @param layout the attribute layout.
     * @param context the context.
     */
    public NewAttribute(final String type, final String layout, final int context) {
        super(type);
        this.layout = layout;
        addContext(context);
    }

    /**
     * Adds a context to this attribute.
     *
     * @param context the context to add.
     */
    public void addContext(final int context) {
        switch (context) {
        case AttributeDefinitionBands.CONTEXT_CLASS:
            contextClass = true;
            break;
        case AttributeDefinitionBands.CONTEXT_METHOD:
            contextMethod = true;
            break;
        case AttributeDefinitionBands.CONTEXT_FIELD:
            contextField = true;
            break;
        case AttributeDefinitionBands.CONTEXT_CODE:
            contextCode = true;
            break;
        }
    }

    /**
     * Gets the attribute bytes.
     *
     * @return the bytes.
     */
    public byte[] getBytes() {
        return contents;
    }

    /**
     * Gets the label at the specified index.
     *
     * @param index the index.
     * @return the label.
     */
    public Label getLabel(final int index) {
        return labels[index];
    }

    /**
     * Gets the layout.
     *
     * @return the layout string.
     */
    public String getLayout() {
        return layout;
    }

    @Override
    public boolean isCodeAttribute() {
        return codeOff != -1;
    }

    /**
     * Tests whether this is a class context attribute.
     *
     * @return true if class context.
     */
    public boolean isContextClass() {
        return contextClass;
    }

    /**
     * Tests whether this is a code context attribute.
     *
     * @return true if code context.
     */
    public boolean isContextCode() {
        return contextCode;
    }

    /**
     * Tests whether this is a field context attribute.
     *
     * @return true if field context.
     */
    public boolean isContextField() {
        return contextField;
    }

    /**
     * Tests whether this is a method context attribute.
     *
     * @return true if method context.
     */
    public boolean isContextMethod() {
        return contextMethod;
    }

    @Override
    public boolean isUnknown() {
        return false;
    }

    /**
     * Tests whether this attribute is unknown for the given context.
     *
     * @param context the context.
     * @return true if unknown for this context.
     */
    public boolean isUnknown(final int context) {
        switch (context) {
        case AttributeDefinitionBands.CONTEXT_CLASS:
            return !contextClass;
        case AttributeDefinitionBands.CONTEXT_METHOD:
            return !contextMethod;
        case AttributeDefinitionBands.CONTEXT_FIELD:
            return !contextField;
        case AttributeDefinitionBands.CONTEXT_CODE:
            return !contextCode;
        }
        return false;
    }

    @Override
    protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf, final int codeOff, final Label[] labels) {
        final byte[] attributeContents = new byte[len];
        System.arraycopy(cr.b, off, attributeContents, 0, len);
        return new NewAttribute(cr, type, layout, attributeContents, buf, codeOff, labels);
    }

    /**
     * Reads a class name at the specified index.
     *
     * @param index the index.
     * @return the class name.
     */
    public String readClass(final int index) {
        return classReader.readClass(index, buf);
    }

    /**
     * Reads a constant at the specified index.
     *
     * @param index the index.
     * @return the constant.
     */
    public Object readConst(final int index) {
        return classReader.readConst(index, buf);
    }

    /**
     * Reads a UTF-8 string at the specified index.
     *
     * @param index the index.
     * @return the UTF-8 string.
     */
    public String readUTF8(final int index) {
        return classReader.readUTF8(index, buf);
    }
}
