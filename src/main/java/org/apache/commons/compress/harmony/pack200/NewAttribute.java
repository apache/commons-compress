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

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * NewAttribute extends {@code Attribute} and manages unknown attributes encountered by ASM that have had a layout
 * definition given to pack200 (e.g. via one of the -C, -M, -F or -D command line options)
 */
public class NewAttribute extends Attribute {

    /**
     * ErrorAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had an error
     * action specified to pack200 (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=error)
     */
    public static class ErrorAttribute extends NewAttribute {

        public ErrorAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf,
            final int codeOff, final Label[] labels) {
            throw new Error("Attribute " + type + " was found");
        }

    }
    /**
     * PassAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had an pass
     * action specified to pack200 (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=pass)
     */
    public static class PassAttribute extends NewAttribute {

        public PassAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf,
            final int codeOff, final Label[] labels) {
            throw new Segment.PassException();
        }

    }
    /**
     * StripAttribute extends {@code NewAttribute} and manages attributes encountered by ASM that have had an strip
     * action specified to pack200 (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=strip)
     */
    public static class StripAttribute extends NewAttribute {

        public StripAttribute(final String type, final int context) {
            super(type, "", context);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf,
            final int codeOff, final Label[] labels) {
            // TODO Not sure if this works, can we really strip an attribute if we don't know the layout?
            return null;
        }
    }
    private boolean contextClass = false;

    private boolean contextMethod = false;
    private boolean contextField = false;
    private boolean contextCode = false;
    private final String layout;
    private byte[] contents;
    private int codeOff;

    private Label[] labels;

    private ClassReader classReader;

    private char[] buf;

    public NewAttribute(final ClassReader classReader, final String type, final String layout, final byte[] contents,
        final char[] buf, final int codeOff, final Label[] labels) {
        super(type);
        this.classReader = classReader;
        this.contents = contents;
        this.layout = layout;
        this.codeOff = codeOff;
        this.labels = labels;
        this.buf = buf;
    }

    public NewAttribute(final String type, final String layout, final int context) {
        super(type);
        this.layout = layout;
        addContext(context);
    }

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

    public byte[] getBytes() {
        return contents;
    }

    public Label getLabel(final int index) {
        return labels[index];
    }

    public String getLayout() {
        return layout;
    }

    @Override
    public boolean isCodeAttribute() {
        return codeOff != -1;
    }

    public boolean isContextClass() {
        return contextClass;
    }

    public boolean isContextCode() {
        return contextCode;
    }

    public boolean isContextField() {
        return contextField;
    }

    public boolean isContextMethod() {
        return contextMethod;
    }

    @Override
    public boolean isUnknown() {
        return false;
    }

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
    protected Attribute read(final ClassReader cr, final int off, final int len, final char[] buf, final int codeOff,
        final Label[] labels) {
        final byte[] attributeContents = new byte[len];
        System.arraycopy(cr.b, off, attributeContents, 0, len);
        return new NewAttribute(cr, type, layout, attributeContents, buf, codeOff, labels);
    }

    public String readClass(final int index) {
        return classReader.readClass(index, buf);
    }

    public Object readConst(final int index) {
        return classReader.readConst(index, buf);
    }

    public String readUTF8(final int index) {
        return classReader.readUTF8(index, buf);
    }
}
