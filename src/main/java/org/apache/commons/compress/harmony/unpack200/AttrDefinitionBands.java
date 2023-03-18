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

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.AnnotationDefaultAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.ConstantValueAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.DeprecatedAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.EnclosingMethodAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.ExceptionsAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.InnerClassesAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LineNumberTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LocalVariableTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.LocalVariableTypeTableAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.SignatureAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.SourceFileAttribute;

/**
 * Attribute definition bands are the set of bands used to define extra attributes transmitted in the archive.
 */
public class AttrDefinitionBands extends BandSet {

    private int[] attributeDefinitionHeader;

    private String[] attributeDefinitionLayout;

    private String[] attributeDefinitionName;

    private AttributeLayoutMap attributeDefinitionMap;

    private final String[] cpUTF8;

    public AttrDefinitionBands(final Segment segment) {
        super(segment);
        this.cpUTF8 = segment.getCpBands().getCpUTF8();
    }

    public AttributeLayoutMap getAttributeDefinitionMap() {
        return attributeDefinitionMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.BandSet#unpack(java.io.InputStream)
     */
    @Override
    public void read(final InputStream in) throws IOException, Pack200Exception {
        final int attributeDefinitionCount = header.getAttributeDefinitionCount();
        attributeDefinitionHeader = decodeBandInt("attr_definition_headers", in, Codec.BYTE1, attributeDefinitionCount);
        attributeDefinitionName = parseReferences("attr_definition_name", in, Codec.UNSIGNED5, attributeDefinitionCount,
            cpUTF8);
        attributeDefinitionLayout = parseReferences("attr_definition_layout", in, Codec.UNSIGNED5,
            attributeDefinitionCount, cpUTF8);

        attributeDefinitionMap = new AttributeLayoutMap();

        int overflowIndex = 32;
        if (segment.getSegmentHeader().getOptions().hasClassFlagsHi()) {
            overflowIndex = 63;
        }
        for (int i = 0; i < attributeDefinitionCount; i++) {
            final int context = attributeDefinitionHeader[i] & 0x03;
            int index = (attributeDefinitionHeader[i] >> 2) - 1;
            if (index == -1) {
                index = overflowIndex++;
            }
            final AttributeLayout layout = new AttributeLayout(attributeDefinitionName[i], context,
                attributeDefinitionLayout[i], index, false);
            final NewAttributeBands newBands = new NewAttributeBands(segment, layout);
            attributeDefinitionMap.add(layout, newBands);
        }
        attributeDefinitionMap.checkMap();
        setupDefaultAttributeNames();
    }

    private void setupDefaultAttributeNames() {
        AnnotationDefaultAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("AnnotationDefault")); //$NON-NLS-1$
        CodeAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("Code")); //$NON-NLS-1$
        ConstantValueAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("ConstantValue")); //$NON-NLS-1$
        DeprecatedAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("Deprecated")); //$NON-NLS-1$
        EnclosingMethodAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("EnclosingMethod")); //$NON-NLS-1$
        ExceptionsAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("Exceptions")); //$NON-NLS-1$
        InnerClassesAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("InnerClasses")); //$NON-NLS-1$
        LineNumberTableAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("LineNumberTable")); //$NON-NLS-1$
        LocalVariableTableAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("LocalVariableTable")); //$NON-NLS-1$
        LocalVariableTypeTableAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("LocalVariableTypeTable")); //$NON-NLS-1$
        SignatureAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("Signature")); //$NON-NLS-1$
        SourceFileAttribute.setAttributeName(segment.getCpBands().cpUTF8Value("SourceFile")); //$NON-NLS-1$
        MetadataBandGroup.setRvaAttributeName(segment.getCpBands().cpUTF8Value("RuntimeVisibleAnnotations"));
        MetadataBandGroup.setRiaAttributeName(segment.getCpBands().cpUTF8Value("RuntimeInvisibleAnnotations"));
        MetadataBandGroup.setRvpaAttributeName(segment.getCpBands().cpUTF8Value("RuntimeVisibleParameterAnnotations"));
        MetadataBandGroup
            .setRipaAttributeName(segment.getCpBands().cpUTF8Value("RuntimeInvisibleParameterAnnotations"));
    }

    @Override
    public void unpack() throws Pack200Exception, IOException {

    }

}
