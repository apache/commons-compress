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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Attribute;

/**
 * Attribute Definition bands define how any unknown attributes should be read by the decompressor.
 */
public class AttributeDefinitionBands extends BandSet {

    public static class AttributeDefinition {

        public int index;
        public int contextType;
        public CPUTF8 name;
        public CPUTF8 layout;

        public AttributeDefinition(final int index, final int contextType, final CPUTF8 name, final CPUTF8 layout) {
            this.index = index;
            this.contextType = contextType;
            this.name = name;
            this.layout = layout;
        }

    }
    public static final int CONTEXT_CLASS = 0;
    public static final int CONTEXT_CODE = 3;
    public static final int CONTEXT_FIELD = 1;

    public static final int CONTEXT_METHOD = 2;
    private final List<AttributeDefinition> classAttributeLayouts = new ArrayList<>();
    private final List<AttributeDefinition> methodAttributeLayouts = new ArrayList<>();
    private final List<AttributeDefinition> fieldAttributeLayouts = new ArrayList<>();

    private final List<AttributeDefinition> codeAttributeLayouts = new ArrayList<>();

    private final List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
    private final CpBands cpBands;

    private final Segment segment;

    public AttributeDefinitionBands(final Segment segment, final int effort, final Attribute[] attributePrototypes) {
        super(effort, segment.getSegmentHeader());
        this.cpBands = segment.getCpBands();
        this.segment = segment;
        final Map<String, String> classLayouts = new HashMap<>();
        final Map<String, String> methodLayouts = new HashMap<>();
        final Map<String, String> fieldLayouts = new HashMap<>();
        final Map<String, String> codeLayouts = new HashMap<>();

        for (final Attribute attributePrototype : attributePrototypes) {
            final NewAttribute newAttribute = (NewAttribute) attributePrototype;
            if (!(newAttribute instanceof NewAttribute.ErrorAttribute)
                && !(newAttribute instanceof NewAttribute.PassAttribute)
                && !(newAttribute instanceof NewAttribute.StripAttribute)) {
                if (newAttribute.isContextClass()) {
                    classLayouts.put(newAttribute.type, newAttribute.getLayout());
                }
                if (newAttribute.isContextMethod()) {
                    methodLayouts.put(newAttribute.type, newAttribute.getLayout());
                }
                if (newAttribute.isContextField()) {
                    fieldLayouts.put(newAttribute.type, newAttribute.getLayout());
                }
                if (newAttribute.isContextCode()) {
                    codeLayouts.put(newAttribute.type, newAttribute.getLayout());
                }
            }
        }
        if (classLayouts.size() > 7) {
            segmentHeader.setHave_class_flags_hi(true);
        }
        if (methodLayouts.size() > 6) {
            segmentHeader.setHave_method_flags_hi(true);
        }
        if (fieldLayouts.size() > 10) {
            segmentHeader.setHave_field_flags_hi(true);
        }
        if (codeLayouts.size() > 15) {
            segmentHeader.setHave_code_flags_hi(true);
        }
        int[] availableClassIndices = {25, 26, 27, 28, 29, 30, 31};
        if (classLayouts.size() > 7) {
            availableClassIndices = addHighIndices(availableClassIndices);
        }
        addAttributeDefinitions(classLayouts, availableClassIndices, CONTEXT_CLASS);
        int[] availableMethodIndices = {26, 27, 28, 29, 30, 31};
        if (methodAttributeLayouts.size() > 6) {
            availableMethodIndices = addHighIndices(availableMethodIndices);
        }
        addAttributeDefinitions(methodLayouts, availableMethodIndices, CONTEXT_METHOD);
        int[] availableFieldIndices = {18, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        if (fieldAttributeLayouts.size() > 10) {
            availableFieldIndices = addHighIndices(availableFieldIndices);
        }
        addAttributeDefinitions(fieldLayouts, availableFieldIndices, CONTEXT_FIELD);
        int[] availableCodeIndices = {17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        if (codeAttributeLayouts.size() > 15) {
            availableCodeIndices = addHighIndices(availableCodeIndices);
        }
        addAttributeDefinitions(codeLayouts, availableCodeIndices, CONTEXT_CODE);
    }

    private void addAttributeDefinitions(final Map<String, String> layoutMap, final int[] availableIndices, final int contextType) {
        final int i = 0;
        layoutMap.forEach((name, layout) -> {
            final int index = availableIndices[i];
            final AttributeDefinition definition = new AttributeDefinition(index, contextType, cpBands.getCPUtf8(name), cpBands.getCPUtf8(layout));
            attributeDefinitions.add(definition);
            switch (contextType) {
            case CONTEXT_CLASS:
                classAttributeLayouts.add(definition);
                break;
            case CONTEXT_METHOD:
                methodAttributeLayouts.add(definition);
                break;
            case CONTEXT_FIELD:
                fieldAttributeLayouts.add(definition);
                break;
            case CONTEXT_CODE:
                codeAttributeLayouts.add(definition);
            }
        });
    }

    private int[] addHighIndices(final int[] availableIndices) {
        final int[] temp = Arrays.copyOf(availableIndices, availableIndices.length + 32);
        int j = 32;
        for (int i = availableIndices.length; i < temp.length; i++) {
            temp[i] = j;
            j++;
        }
        return temp;
    }

    private void addSyntheticDefinitions() {
        final boolean anySytheticClasses = segment.getClassBands().isAnySyntheticClasses();
        final boolean anySyntheticMethods = segment.getClassBands().isAnySyntheticMethods();
        final boolean anySyntheticFields = segment.getClassBands().isAnySyntheticFields();
        if (anySytheticClasses || anySyntheticMethods || anySyntheticFields) {
            final CPUTF8 syntheticUTF = cpBands.getCPUtf8("Synthetic");
            final CPUTF8 emptyUTF = cpBands.getCPUtf8("");
            if (anySytheticClasses) {
                attributeDefinitions.add(new AttributeDefinition(12, CONTEXT_CLASS, syntheticUTF, emptyUTF));
            }
            if (anySyntheticMethods) {
                attributeDefinitions.add(new AttributeDefinition(12, CONTEXT_METHOD, syntheticUTF, emptyUTF));
            }
            if (anySyntheticFields) {
                attributeDefinitions.add(new AttributeDefinition(12, CONTEXT_FIELD, syntheticUTF, emptyUTF));
            }
        }
    }

    /**
     * All input classes for the segment have now been read in, so this method is called so that this class can
     * calculate/complete anything it could not do while classes were being read.
     */
    public void finaliseBands() {
        addSyntheticDefinitions();
        segmentHeader.setAttribute_definition_count(attributeDefinitions.size());
    }

    public List<AttributeDefinition> getClassAttributeLayouts() {
        return classAttributeLayouts;
    }

    public List<AttributeDefinition> getCodeAttributeLayouts() {
        return codeAttributeLayouts;
    }

    public List<AttributeDefinition> getFieldAttributeLayouts() {
        return fieldAttributeLayouts;
    }

    public List<AttributeDefinition> getMethodAttributeLayouts() {
        return methodAttributeLayouts;
    }

    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing attribute definition bands...");
        final int[] attributeDefinitionHeader = new int[attributeDefinitions.size()];
        final int[] attributeDefinitionName = new int[attributeDefinitions.size()];
        final int[] attributeDefinitionLayout = new int[attributeDefinitions.size()];
        for (int i = 0; i < attributeDefinitionLayout.length; i++) {
            final AttributeDefinition def = attributeDefinitions.get(i);
            attributeDefinitionHeader[i] = def.contextType | (def.index + 1 << 2);
            attributeDefinitionName[i] = def.name.getIndex();
            attributeDefinitionLayout[i] = def.layout.getIndex();
        }

        byte[] encodedBand = encodeBandInt("attributeDefinitionHeader", attributeDefinitionHeader, Codec.BYTE1);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from attributeDefinitionHeader["
            + attributeDefinitionHeader.length + "]");

        encodedBand = encodeBandInt("attributeDefinitionName", attributeDefinitionName, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from attributeDefinitionName["
            + attributeDefinitionName.length + "]");

        encodedBand = encodeBandInt("attributeDefinitionLayout", attributeDefinitionLayout, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from attributeDefinitionLayout["
            + attributeDefinitionLayout.length + "]");
    }
}
