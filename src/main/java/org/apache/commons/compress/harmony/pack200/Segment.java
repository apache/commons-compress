/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.commons.compress.harmony.pack200;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.harmony.pack200.Archive.PackingFile;
import org.apache.commons.compress.harmony.pack200.Archive.SegmentUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * A Pack200 archive consists of one or more Segments.
 */
public class Segment extends ClassVisitor {

     public static int ASM_API = Opcodes.ASM4; /* see https://asm.ow2.io/javadoc/org/objectweb/asm/Opcodes.html#ASM4 */
     
    public Segment() {
        super(ASM_API);
    }

    private SegmentHeader segmentHeader;
    private CpBands cpBands;
    private AttributeDefinitionBands attributeDefinitionBands;
    private IcBands icBands;
    private ClassBands classBands;
    private BcBands bcBands;
    private FileBands fileBands;

    private final SegmentFieldVisitor fieldVisitor = new SegmentFieldVisitor();
    private final SegmentMethodVisitor methodVisitor = new SegmentMethodVisitor();
    private Pack200ClassReader currentClassReader;
    private PackingOptions options;
    private boolean stripDebug;
    private Attribute[] nonStandardAttributePrototypes;

    /**
     * The main method on Segment. Reads in all the class files, packs them and then writes the packed segment out to
     * the given OutputStream.
     *
     * @param segmentUnit TODO
     * @param out the OutputStream to write the packed Segment to
     * @param options packing options
     * @throws IOException If an I/O error occurs.
     * @throws Pack200Exception TODO
     */
    public void pack(final SegmentUnit segmentUnit, final OutputStream out, final PackingOptions options)
        throws IOException, Pack200Exception {
        this.options = options;
        this.stripDebug = options.isStripDebug();
        final int effort = options.getEffort();
        nonStandardAttributePrototypes = options.getUnknownAttributePrototypes();

        PackingUtils.log("Start to pack a new segment with " + segmentUnit.fileListSize() + " files including "
            + segmentUnit.classListSize() + " classes");

        PackingUtils.log("Initialize a header for the segment");
        segmentHeader = new SegmentHeader();
        segmentHeader.setFile_count(segmentUnit.fileListSize());
        segmentHeader.setHave_all_code_flags(!stripDebug);
        if (!options.isKeepDeflateHint()) {
            segmentHeader.setDeflate_hint("true".equals(options.getDeflateHint()));
        }

        PackingUtils.log("Setup constant pool bands for the segment");
        cpBands = new CpBands(this, effort);

        PackingUtils.log("Setup attribute definition bands for the segment");
        attributeDefinitionBands = new AttributeDefinitionBands(this, effort, nonStandardAttributePrototypes);

        PackingUtils.log("Setup internal class bands for the segment");
        icBands = new IcBands(segmentHeader, cpBands, effort);

        PackingUtils.log("Setup class bands for the segment");
        classBands = new ClassBands(this, segmentUnit.classListSize(), effort, stripDebug);

        PackingUtils.log("Setup byte code bands for the segment");
        bcBands = new BcBands(cpBands, this, effort);

        PackingUtils.log("Setup file bands for the segment");
        fileBands = new FileBands(cpBands, segmentHeader, options, segmentUnit, effort);

        processClasses(segmentUnit, nonStandardAttributePrototypes);

        cpBands.finaliseBands();
        attributeDefinitionBands.finaliseBands();
        icBands.finaliseBands();
        classBands.finaliseBands();
        bcBands.finaliseBands();
        fileBands.finaliseBands();

        // Using a temporary stream because we have to pack the other bands
        // before segmentHeader because the band_headers band is only created
        // when the other bands are packed, but comes before them in the packed
        // file.
        final ByteArrayOutputStream bandsOutputStream = new ByteArrayOutputStream();

        PackingUtils.log("Packing...");
        final int finalNumberOfClasses = classBands.numClassesProcessed();
        segmentHeader.setClass_count(finalNumberOfClasses);
        cpBands.pack(bandsOutputStream);
        if (finalNumberOfClasses > 0) {
            attributeDefinitionBands.pack(bandsOutputStream);
            icBands.pack(bandsOutputStream);
            classBands.pack(bandsOutputStream);
            bcBands.pack(bandsOutputStream);
        }
        fileBands.pack(bandsOutputStream);

        final ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        segmentHeader.pack(headerOutputStream);

        headerOutputStream.writeTo(out);
        bandsOutputStream.writeTo(out);

        segmentUnit.addPackedByteAmount(headerOutputStream.size());
        segmentUnit.addPackedByteAmount(bandsOutputStream.size());

        PackingUtils.log("Wrote total of " + segmentUnit.getPackedByteAmount() + " bytes");
        PackingUtils.log("Transmitted " + segmentUnit.fileListSize() + " files of " + segmentUnit.getByteAmount()
            + " input bytes in a segment of " + segmentUnit.getPackedByteAmount() + " bytes");
    }

    private void processClasses(final SegmentUnit segmentUnit, final Attribute[] attributes) throws Pack200Exception {
        segmentHeader.setClass_count(segmentUnit.classListSize());
        for (final Iterator iterator = segmentUnit.getClassList().iterator(); iterator.hasNext();) {
            final Pack200ClassReader classReader = (Pack200ClassReader) iterator.next();
            currentClassReader = classReader;
            int flags = 0;
            if (stripDebug) {
                flags |= ClassReader.SKIP_DEBUG;
            }
            try {
                classReader.accept(this, attributes, flags);
            } catch (final PassException pe) {
                // Pass this class through as-is rather than packing it
                // TODO: probably need to deal with any inner classes
                classBands.removeCurrentClass();
                final String name = classReader.getFileName();
                options.addPassFile(name);
                cpBands.addCPUtf8(name);
                boolean found = false;
                for (final Iterator iterator2 = segmentUnit.getFileList().iterator(); iterator2.hasNext();) {
                    final PackingFile file = (PackingFile) iterator2.next();
                    if (file.getName().equals(name)) {
                        found = true;
                        file.setContents(classReader.b);
                        break;
                    }
                }
                if (!found) {
                    throw new Pack200Exception("Error passing file " + name);
                }
            }
        }
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
        final String superName, final String[] interfaces) {
        bcBands.setCurrentClass(name, superName);
        segmentHeader.addMajorVersion(version);
        classBands.addClass(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(final String source, final String debug) {
        if (!stripDebug) {
            classBands.addSourceFile(source);
        }
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
        classBands.addEnclosingMethod(owner, name, desc);

    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_CLASS, desc, visible);
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        if (attribute.isUnknown()) {
            final String action = options.getUnknownAttributeAction();
            if (action.equals(PackingOptions.PASS)) {
                passCurrentClass();
            } else if (action.equals(PackingOptions.ERROR)) {
                throw new Error("Unknown attribute encountered");
            } // else skip
        } else if (attribute instanceof NewAttribute) {
            final NewAttribute newAttribute = (NewAttribute) attribute;
            if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_CLASS)) {
                final String action = options.getUnknownClassAttributeAction(newAttribute.type);
                if (action.equals(PackingOptions.PASS)) {
                    passCurrentClass();
                } else if (action.equals(PackingOptions.ERROR)) {
                    throw new Error("Unknown attribute encountered");
                } // else skip
            }
            classBands.addClassAttribute(newAttribute);
        } else {
            throw new RuntimeException("Unexpected attribute encountered: " + attribute.type);
        }
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int flags) {
        icBands.addInnerClass(name, outerName, innerName, flags);
    }

    @Override
    public FieldVisitor visitField(final int flags, final String name, final String desc, final String signature,
        final Object value) {
        classBands.addField(flags, name, desc, signature, value);
        return fieldVisitor;
    }

    @Override
    public MethodVisitor visitMethod(final int flags, final String name, final String desc, final String signature,
        final String[] exceptions) {
        classBands.addMethod(flags, name, desc, signature, exceptions);
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        classBands.endOfClass();
    }

    /**
     * This class implements MethodVisitor to visit the contents and metadata related to methods in a class file.
     *
     * It delegates to BcBands for bytecode related visits and to ClassBands for everything else.
     */
    public class SegmentMethodVisitor extends MethodVisitor {
        
        public SegmentMethodVisitor() {
            super(ASM_API);
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_METHOD, desc, visible);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_METHOD);
        }

        @Override
        public void visitAttribute(final Attribute attribute) {
            if (attribute.isUnknown()) {
                final String action = options.getUnknownAttributeAction();
                if (action.equals(PackingOptions.PASS)) {
                    passCurrentClass();
                } else if (action.equals(PackingOptions.ERROR)) {
                    throw new Error("Unknown attribute encountered");
                } // else skip
            } else if (attribute instanceof NewAttribute) {
                final NewAttribute newAttribute = (NewAttribute) attribute;
                if (attribute.isCodeAttribute()) {
                    if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_CODE)) {
                        final String action = options.getUnknownCodeAttributeAction(newAttribute.type);
                        if (action.equals(PackingOptions.PASS)) {
                            passCurrentClass();
                        } else if (action.equals(PackingOptions.ERROR)) {
                            throw new Error("Unknown attribute encountered");
                        } // else skip
                    }
                    classBands.addCodeAttribute(newAttribute);
                } else {
                    if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_METHOD)) {
                        final String action = options.getUnknownMethodAttributeAction(newAttribute.type);
                        if (action.equals(PackingOptions.PASS)) {
                            passCurrentClass();
                        } else if (action.equals(PackingOptions.ERROR)) {
                            throw new Error("Unknown attribute encountered");
                        } // else skip
                    }
                    classBands.addMethodAttribute(newAttribute);
                }
            } else {
                throw new RuntimeException("Unexpected attribute encountered: " + attribute.type);
            }
        }

        @Override
        public void visitCode() {
            classBands.addCode();
        }

        @Override
        public void visitFrame(final int arg0, final int arg1, final Object[] arg2, final int arg3,
            final Object[] arg4) {
            // TODO: Java 6 - implement support for this

        }

        @Override
        public void visitLabel(final Label label) {
            bcBands.visitLabel(label);
        }

        @Override
        public void visitLineNumber(final int line, final Label start) {
            if (!stripDebug) {
                classBands.addLineNumber(line, start);
            }
        }

        @Override
        public void visitLocalVariable(final String name, final String desc, final String signature, final Label start,
            final Label end, final int index) {
            if (!stripDebug) {
                classBands.addLocalVariable(name, desc, signature, start, end, index);
            }
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            classBands.addMaxStack(maxStack, maxLocals);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc,
            final boolean visible) {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_METHOD, parameter, desc, visible);
        }

        @Override
        public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
            classBands.addHandler(start, end, handler, type);
        }

        @Override
        public void visitEnd() {
            classBands.endOfMethod();
            bcBands.visitEnd();
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
            bcBands.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitIincInsn(final int var, final int increment) {
            bcBands.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(final int opcode) {
            bcBands.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(final int opcode, final int operand) {
            bcBands.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitJumpInsn(final int opcode, final Label label) {
            bcBands.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(final Object cst) {
            bcBands.visitLdcInsn(cst);
        }

        @Override
        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            bcBands.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
            bcBands.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dimensions) {
            bcBands.visitMultiANewArrayInsn(desc, dimensions);
        }

        @Override
        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label[] labels) {
            bcBands.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            bcBands.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(final int opcode, final int var) {
            bcBands.visitVarInsn(opcode, var);
        }

    }

    public ClassBands getClassBands() {
        return classBands;
    }

    /**
     * SegmentAnnotationVisitor implements <code>AnnotationVisitor</code> to visit Annotations found in a class file.
     */
    public class SegmentAnnotationVisitor extends AnnotationVisitor {

        private int context = -1;
        private int parameter = -1;
        private String desc;
        private boolean visible;

        private final List nameRU = new ArrayList();
        private final List T = new ArrayList(); // tags
        private final List values = new ArrayList();
        private final List caseArrayN = new ArrayList();
        private final List nestTypeRS = new ArrayList();
        private final List nestNameRU = new ArrayList();
        private final List nestPairN = new ArrayList();

        public SegmentAnnotationVisitor(final int context, final String desc, final boolean visible) {
            super(ASM_API);
            this.context = context;
            this.desc = desc;
            this.visible = visible;
        }

        public SegmentAnnotationVisitor(final int context) {
            super(ASM_API);
            this.context = context;
        }

        public SegmentAnnotationVisitor(final int context, final int parameter, final String desc,
            final boolean visible) {
            super(ASM_API);
            this.context = context;
            this.parameter = parameter;
            this.desc = desc;
            this.visible = visible;
        }

        @Override
        public void visit(String name, final Object value) {
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            addValueAndTag(value, T, values);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, final String desc) {
            T.add("@");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            nestTypeRS.add(desc);
            nestPairN.add(Integer.valueOf(0));
            return new AnnotationVisitor(context, av) {
                @Override
                public void visit(final String name, final Object value) {
                    final Integer numPairs = (Integer) nestPairN.remove(nestPairN.size() - 1);
                    nestPairN.add(Integer.valueOf(numPairs.intValue() + 1));
                    nestNameRU.add(name);
                    addValueAndTag(value, T, values);
                }

                @Override
                public AnnotationVisitor visitAnnotation(final String arg0, final String arg1) {
                    throw new RuntimeException("Not yet supported");
//                    return null;
                }

                @Override
                public AnnotationVisitor visitArray(final String arg0) {
                    throw new RuntimeException("Not yet supported");
//                    return null;
                }

                @Override
                public void visitEnd() {
                }

                @Override
                public void visitEnum(final String name, final String desc, final String value) {
                    final Integer numPairs = (Integer) nestPairN.remove(nestPairN.size() - 1);
                    nestPairN.add(Integer.valueOf(numPairs.intValue() + 1));
                    T.add("e");
                    nestNameRU.add(name);
                    values.add(desc);
                    values.add(value);
                }
            };
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            T.add("[");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            caseArrayN.add(Integer.valueOf(0));
            return new ArrayVisitor(caseArrayN, T, nameRU, values);
        }

        @Override
        public void visitEnd() {
            if (desc == null) {
                Segment.this.classBands.addAnnotationDefault(nameRU, T, values, caseArrayN, nestTypeRS, nestNameRU,
                    nestPairN);
            } else if (parameter != -1) {
                Segment.this.classBands.addParameterAnnotation(parameter, desc, visible, nameRU, T, values, caseArrayN,
                    nestTypeRS, nestNameRU, nestPairN);
            } else {
                Segment.this.classBands.addAnnotation(context, desc, visible, nameRU, T, values, caseArrayN, nestTypeRS,
                    nestNameRU, nestPairN);
            }
        }

        @Override
        public void visitEnum(String name, final String desc, final String value) {
            T.add("e");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            values.add(desc);
            values.add(value);
        }
    }

    public class ArrayVisitor extends AnnotationVisitor {

        private final int indexInCaseArrayN;
        private final List caseArrayN;
        private final List values;
        private final List nameRU;
        private final List T;

        public ArrayVisitor(final List caseArrayN, final List T, final List nameRU, final List values) {
            super(ASM_API);

            this.caseArrayN = caseArrayN;
            this.T = T;
            this.nameRU = nameRU;
            this.values = values;
            this.indexInCaseArrayN = caseArrayN.size() - 1;
        }

        @Override
        public void visit(String name, final Object value) {
            final Integer numCases = (Integer) caseArrayN.remove(indexInCaseArrayN);
            caseArrayN.add(indexInCaseArrayN, Integer.valueOf(numCases.intValue() + 1));
            if (name == null) {
                name = "";
            }
            addValueAndTag(value, T, values);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String arg0, final String arg1) {
            throw new RuntimeException("Not yet supported");
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            T.add("[");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            caseArrayN.add(Integer.valueOf(0));
            return new ArrayVisitor(caseArrayN, T, nameRU, values);
        }

        @Override
        public void visitEnd() {
        }

        @Override
        public void visitEnum(final String name, final String desc, final String value) {
            final Integer numCases = (Integer) caseArrayN.remove(caseArrayN.size() - 1);
            caseArrayN.add(Integer.valueOf(numCases.intValue() + 1));
            T.add("e");
            values.add(desc);
            values.add(value);
        }
    }

    /**
     * SegmentFieldVisitor implements <code>FieldVisitor</code> to visit the metadata relating to fields in a class
     * file.
     */
    public class SegmentFieldVisitor extends FieldVisitor {

        public SegmentFieldVisitor() {
            super(ASM_API);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_FIELD, desc, visible);
        }

        @Override
        public void visitAttribute(final Attribute attribute) {
            if (attribute.isUnknown()) {
                final String action = options.getUnknownAttributeAction();
                if (action.equals(PackingOptions.PASS)) {
                    passCurrentClass();
                } else if (action.equals(PackingOptions.ERROR)) {
                    throw new Error("Unknown attribute encountered");
                } // else skip
            } else if (attribute instanceof NewAttribute) {
                final NewAttribute newAttribute = (NewAttribute) attribute;
                if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_FIELD)) {
                    final String action = options.getUnknownFieldAttributeAction(newAttribute.type);
                    if (action.equals(PackingOptions.PASS)) {
                        passCurrentClass();
                    } else if (action.equals(PackingOptions.ERROR)) {
                        throw new Error("Unknown attribute encountered");
                    } // else skip
                }
                classBands.addFieldAttribute(newAttribute);
            } else {
                throw new RuntimeException("Unexpected attribute encountered: " + attribute.type);
            }
        }

        @Override
        public void visitEnd() {
        }
    }

    // helper method for annotation visitors
    private void addValueAndTag(final Object value, final List T, final List values) {
        if (value instanceof Integer) {
            T.add("I");
            values.add(value);
        } else if (value instanceof Double) {
            T.add("D");
            values.add(value);
        } else if (value instanceof Float) {
            T.add("F");
            values.add(value);
        } else if (value instanceof Long) {
            T.add("J");
            values.add(value);
        } else if (value instanceof Byte) {
            T.add("B");
            values.add(Integer.valueOf(((Byte) value).intValue()));
        } else if (value instanceof Character) {
            T.add("C");
            values.add(Integer.valueOf(((Character) value).charValue()));
        } else if (value instanceof Short) {
            T.add("S");
            values.add(Integer.valueOf(((Short) value).intValue()));
        } else if (value instanceof Boolean) {
            T.add("Z");
            values.add(Integer.valueOf(((Boolean) value).booleanValue() ? 1 : 0));
        } else if (value instanceof String) {
            T.add("s");
            values.add(value);
        } else if (value instanceof Type) {
            T.add("c");
            values.add(((Type) value).toString());
        }
    }

    public boolean lastConstantHadWideIndex() {
        return currentClassReader.lastConstantHadWideIndex();
    }

    public CpBands getCpBands() {
        return cpBands;
    }

    public SegmentHeader getSegmentHeader() {
        return segmentHeader;
    }

    public AttributeDefinitionBands getAttrBands() {
        return attributeDefinitionBands;
    }

    public IcBands getIcBands() {
        return icBands;
    }

    public Pack200ClassReader getCurrentClassReader() {
        return currentClassReader;
    }

    private void passCurrentClass() {
        throw new PassException();
    }

    /**
     * Exception indicating that the class currently being visited contains an unknown attribute, which means that by
     * default the class file needs to be passed through as-is in the file_bands rather than being packed with pack200.
     */
    public static class PassException extends RuntimeException {

    }
}
