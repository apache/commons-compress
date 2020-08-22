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

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * NewAttribute extends <code>Attribute</code> and manages unknown attributes
 * encountered by ASM that have had a layout definition given to pack200 (e.g.
 * via one of the -C, -M, -F or -D command line options)
 */
public class NewAttribute extends Attribute {

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

    public NewAttribute(String type, String layout, int context) {
        super(type);
        this.layout = layout;
        addContext(context);
    }

    public NewAttribute(ClassReader classReader, String type, String layout, byte[] contents, char[] buf,
            int codeOff, Label[] labels) {
        super(type);
        this.classReader = classReader;
        this.contents = contents;
        this.layout = layout;
        this.codeOff = codeOff;
        this.labels = labels;
        this.buf = buf;
    }

    public void addContext(int context) {
        switch(context) {
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

    public boolean isContextClass() {
        return contextClass;
    }

    public boolean isContextMethod() {
        return contextMethod;
    }

    public boolean isContextField() {
        return contextField;
    }

    public boolean isContextCode() {
        return contextCode;
    }

    public String getLayout() {
        return layout;
    }

    public boolean isUnknown() {
        return false;
    }

    public boolean isCodeAttribute() {
        return codeOff != -1;
    }

    protected Attribute read(ClassReader cr, int off, int len, char[] buf,
            int codeOff, Label[] labels) {
        byte[] attributeContents = new byte[len];
        System.arraycopy(cr.b, off, attributeContents, 0, len);
        return new NewAttribute(cr, type, layout, attributeContents, buf, codeOff,
                labels);
    }

    public boolean isUnknown(int context) {
        switch(context) {
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

    public String readUTF8(int index) {
        return classReader.readUTF8(index, buf);
    }

    public String readClass(int index) {
        return classReader.readClass(index, buf);
    }

    public Object readConst(int index) {
        return classReader.readConst(index, buf);
    }

    public byte[] getBytes() {
        return contents;
    }

    public Label getLabel(int index) {
        return labels[index];
    }

    /**
     * ErrorAttribute extends <code>NewAttribute</code> and manages attributes
     * encountered by ASM that have had an error action specified to pack200
     * (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=error)
     */
    public static class ErrorAttribute extends NewAttribute {

        public ErrorAttribute(String type, int context) {
            super(type, "", context);
        }

        protected Attribute read(ClassReader cr, int off, int len, char[] buf,
                int codeOff, Label[] labels) {
            throw new Error("Attribute " + type + " was found");
        }

    }

    /**
     * StripAttribute extends <code>NewAttribute</code> and manages attributes
     * encountered by ASM that have had an strip action specified to pack200
     * (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=strip)
     */
    public static class StripAttribute extends NewAttribute {

        public StripAttribute(String type, int context) {
            super(type, "", context);
        }

        protected Attribute read(ClassReader cr, int off, int len, char[] buf,
                int codeOff, Label[] labels) {
            // TODO Not sure if this works, can we really strip an attribute if we don't know the layout?
            return null;
        }
    }

    /**
     * PassAttribute extends <code>NewAttribute</code> and manages attributes
     * encountered by ASM that have had an pass action specified to pack200
     * (e.g. via one of the -C, -M, -F or -D command line options such as
     * -Cattribute-name=pass)
     */
    public static class PassAttribute extends NewAttribute {

        public PassAttribute(String type, int context) {
            super(type, "", context);
        }

        protected Attribute read(ClassReader cr, int off, int len, char[] buf,
                int codeOff, Label[] labels) {
            throw new Segment.PassException();
        }

    }
}
