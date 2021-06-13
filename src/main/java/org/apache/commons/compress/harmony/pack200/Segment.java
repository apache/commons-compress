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
import org.objectweb.asm.Type;

/**
 * A Pack200 archive consists of one or more Segments.
 */
public class Segment implements ClassVisitor {

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
     * The main method on Segment. Reads in all the class files, packs them and
     * then writes the packed segment out to the given OutputStream.
     *
     * @param segmentUnit TODO
     * @param out
     *            the OutputStream to write the packed Segment to
     * @param options
     *            packing options
     * @throws IOException If an I/O error occurs.
     * @throws Pack200Exception TODO
     */
    public void pack(SegmentUnit segmentUnit, OutputStream out, PackingOptions options)
            throws IOException, Pack200Exception {
        this.options = options;
        this.stripDebug = options.isStripDebug();
        int effort = options.getEffort();
        nonStandardAttributePrototypes = options.getUnknownAttributePrototypes();

        PackingUtils.log("Start to pack a new segment with "
                + segmentUnit.fileListSize() + " files including "
                + segmentUnit.classListSize() + " classes");

        PackingUtils.log("Initialize a header for the segment");
        segmentHeader = new SegmentHeader();
        segmentHeader.setFile_count(segmentUnit.fileListSize());
        segmentHeader.setHave_all_code_flags(!stripDebug);
        if (!options.isKeepDeflateHint()) {
            segmentHeader.setDeflate_hint("true".equals(options
                    .getDeflateHint()));
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
        ByteArrayOutputStream bandsOutputStream = new ByteArrayOutputStream();

        PackingUtils.log("Packing...");
        int finalNumberOfClasses = classBands.numClassesProcessed();
        segmentHeader.setClass_count(finalNumberOfClasses);
        cpBands.pack(bandsOutputStream);
        if(finalNumberOfClasses > 0) {
            attributeDefinitionBands.pack(bandsOutputStream);
            icBands.pack(bandsOutputStream);
            classBands.pack(bandsOutputStream);
            bcBands.pack(bandsOutputStream);
        }
        fileBands.pack(bandsOutputStream);

        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        segmentHeader.pack(headerOutputStream);

        headerOutputStream.writeTo(out);
        bandsOutputStream.writeTo(out);

        segmentUnit.addPackedByteAmount(headerOutputStream.size());
        segmentUnit.addPackedByteAmount(bandsOutputStream.size());

        PackingUtils.log("Wrote total of " + segmentUnit.getPackedByteAmount()
                + " bytes");
        PackingUtils.log("Transmitted " + segmentUnit.fileListSize() + " files of "
                + segmentUnit.getByteAmount() + " input bytes in a segment of "
                + segmentUnit.getPackedByteAmount() + " bytes");
    }

    private void processClasses(SegmentUnit segmentUnit, Attribute[] attributes) throws Pack200Exception {
        segmentHeader.setClass_count(segmentUnit.classListSize());
        for (Iterator iterator = segmentUnit.getClassList().iterator(); iterator.hasNext();) {
            Pack200ClassReader classReader = (Pack200ClassReader) iterator
                    .next();
            currentClassReader = classReader;
            int flags = 0;
            if(stripDebug) {
                flags |= ClassReader.SKIP_DEBUG;
            }
            try {
                classReader.accept(this, attributes, flags);
            } catch (PassException pe) {
                // Pass this class through as-is rather than packing it
                // TODO: probably need to deal with any inner classes
                classBands.removeCurrentClass();
                String name = classReader.getFileName();
                options.addPassFile(name);
                cpBands.addCPUtf8(name);
                boolean found = false;
                for (Iterator iterator2 = segmentUnit.getFileList().iterator(); iterator2
                        .hasNext();) {
                    PackingFile file = (PackingFile) iterator2.next();
                    if(file.getName().equals(name)) {
                        found = true;
                        file.setContents(classReader.b);
                        break;
                    }
                }
                if(!found) {
                    throw new Pack200Exception("Error passing file " + name);
                }
            }
        }
    }

    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        bcBands.setCurrentClass(name, superName);
        segmentHeader.addMajorVersion(version);
        classBands.addClass(version, access, name, signature, superName,
                interfaces);
    }

    public void visitSource(String source, String debug) {
        if(!stripDebug) {
            classBands.addSourceFile(source);
        }
    }

    public void visitOuterClass(String owner, String name, String desc) {
        classBands.addEnclosingMethod(owner, name, desc);

    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_CLASS,
                desc, visible);
    }

    public void visitAttribute(Attribute attribute) {
        if(attribute.isUnknown()) {
            String action = options.getUnknownAttributeAction();
            if(action.equals(PackingOptions.PASS)) {
                passCurrentClass();
            } else if (action.equals(PackingOptions.ERROR)) {
                throw new Error("Unknown attribute encountered");
            } // else skip
        } else {
            if(attribute instanceof NewAttribute) {
                NewAttribute newAttribute = (NewAttribute) attribute;
                if(newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_CLASS)) {
                    String action = options.getUnknownClassAttributeAction(newAttribute.type);
                    if(action.equals(PackingOptions.PASS)) {
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
    }

    public void visitInnerClass(String name, String outerName,
            String innerName, int flags) {
        icBands.addInnerClass(name, outerName, innerName, flags);
    }

    public FieldVisitor visitField(int flags, String name, String desc,
            String signature, Object value) {
        classBands.addField(flags, name, desc, signature, value);
        return fieldVisitor;
    }

    public MethodVisitor visitMethod(int flags, String name, String desc,
            String signature, String[] exceptions) {
        classBands.addMethod(flags, name, desc, signature, exceptions);
        return methodVisitor;
    }

    public void visitEnd() {
        classBands.endOfClass();
    }

    /**
     * This class implements MethodVisitor to visit the contents and metadata
     * related to methods in a class file.
     *
     * It delegates to BcBands for bytecode related visits and to ClassBands for
     * everything else.
     */
    public class SegmentMethodVisitor implements MethodVisitor {

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new SegmentAnnotationVisitor(
                    MetadataBandGroup.CONTEXT_METHOD, desc, visible);
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_METHOD);
        }

        public void visitAttribute(Attribute attribute) {
            if(attribute.isUnknown()) {
                String action = options.getUnknownAttributeAction();
                if(action.equals(PackingOptions.PASS)) {
                    passCurrentClass();
                } else if (action.equals(PackingOptions.ERROR)) {
                    throw new Error("Unknown attribute encountered");
                } // else skip
            } else {
                if(attribute instanceof NewAttribute) {
                    NewAttribute newAttribute = (NewAttribute) attribute;
                    if (attribute.isCodeAttribute()) {
                        if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_CODE)) {
                            String action = options
                                    .getUnknownCodeAttributeAction(newAttribute.type);
                            if (action.equals(PackingOptions.PASS)) {
                                passCurrentClass();
                            } else if (action.equals(PackingOptions.ERROR)) {
                                throw new Error("Unknown attribute encountered");
                            } // else skip
                        }
                        classBands.addCodeAttribute(newAttribute);
                    } else {
                        if (newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_METHOD)) {
                            String action = options
                                    .getUnknownMethodAttributeAction(newAttribute.type);
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
        }

        public void visitCode() {
            classBands.addCode();
        }

        public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
                Object[] arg4) {
            // TODO: Java 6 - implement support for this

        }

        public void visitLabel(Label label) {
            bcBands.visitLabel(label);
        }

        public void visitLineNumber(int line, Label start) {
            if(!stripDebug) {
                classBands.addLineNumber(line, start);
            }
        }

        public void visitLocalVariable(String name, String desc,
                String signature, Label start, Label end, int index) {
            if(!stripDebug) {
                classBands.addLocalVariable(name, desc, signature, start, end,
                        index);
            }
        }

        public void visitMaxs(int maxStack, int maxLocals) {
            classBands.addMaxStack(maxStack, maxLocals);
        }

        public AnnotationVisitor visitParameterAnnotation(int parameter,
                String desc, boolean visible) {
            return new SegmentAnnotationVisitor(
                    MetadataBandGroup.CONTEXT_METHOD, parameter, desc, visible);
        }

        public void visitTryCatchBlock(Label start, Label end, Label handler,
                String type) {
            classBands.addHandler(start, end, handler, type);
        }

        public void visitEnd() {
            classBands.endOfMethod();
            bcBands.visitEnd();
        }

        public void visitFieldInsn(int opcode, String owner, String name,
                String desc) {
            bcBands.visitFieldInsn(opcode, owner, name, desc);
        }

        public void visitIincInsn(int var, int increment) {
            bcBands.visitIincInsn(var, increment);
        }

        public void visitInsn(int opcode) {
            bcBands.visitInsn(opcode);
        }

        public void visitIntInsn(int opcode, int operand) {
            bcBands.visitIntInsn(opcode, operand);
        }

        public void visitJumpInsn(int opcode, Label label) {
            bcBands.visitJumpInsn(opcode, label);
        }

        public void visitLdcInsn(Object cst) {
            bcBands.visitLdcInsn(cst);
        }

        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            bcBands.visitLookupSwitchInsn(dflt, keys, labels);
        }

        public void visitMethodInsn(int opcode, String owner, String name,
                String desc) {
            bcBands.visitMethodInsn(opcode, owner, name, desc);
        }

        public void visitMultiANewArrayInsn(String desc, int dimensions) {
            bcBands.visitMultiANewArrayInsn(desc, dimensions);
        }

        public void visitTableSwitchInsn(int min, int max, Label dflt,
                Label[] labels) {
            bcBands.visitTableSwitchInsn(min, max, dflt, labels);
        }

        public void visitTypeInsn(int opcode, String type) {
            bcBands.visitTypeInsn(opcode, type);
        }

        public void visitVarInsn(int opcode, int var) {
            bcBands.visitVarInsn(opcode, var);
        }

    }

    public ClassBands getClassBands() {
        return classBands;
    }

    /**
     * SegmentAnnotationVisitor implements <code>AnnotationVisitor</code> to
     * visit Annotations found in a class file.
     */
    public class SegmentAnnotationVisitor implements AnnotationVisitor {

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

        public SegmentAnnotationVisitor(int context, String desc,
                boolean visible) {
            this.context = context;
            this.desc = desc;
            this.visible = visible;
        }

        public SegmentAnnotationVisitor(int context) {
            this.context = context;
        }

        public SegmentAnnotationVisitor(int context, int parameter,
                String desc, boolean visible) {
            this.context = context;
            this.parameter = parameter;
            this.desc = desc;
            this.visible = visible;
        }

        public void visit(String name, Object value) {
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            addValueAndTag(value, T, values);
        }
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            T.add("@");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            nestTypeRS.add(desc);
            nestPairN.add(new Integer(0));
            return new AnnotationVisitor() {
                public void visit(String name, Object value) {
                    Integer numPairs = (Integer) nestPairN.remove(nestPairN.size() - 1);
                    nestPairN.add(new Integer(numPairs.intValue() + 1));
                    nestNameRU.add(name);
                    addValueAndTag(value, T, values);
                }

                public AnnotationVisitor visitAnnotation(String arg0,
                        String arg1) {
                    throw new RuntimeException("Not yet supported");
//                    return null;
                }

                public AnnotationVisitor visitArray(String arg0) {
                    throw new RuntimeException("Not yet supported");
//                    return null;
                }

                public void visitEnd() {
                }

                public void visitEnum(String name, String desc, String value) {
                    Integer numPairs = (Integer) nestPairN.remove(nestPairN.size() - 1);
                    nestPairN.add(new Integer(numPairs.intValue() + 1));
                    T.add("e");
                    nestNameRU.add(name);
                    values.add(desc);
                    values.add(value);
                }
            };
        }

        public AnnotationVisitor visitArray(String name) {
            T.add("[");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            caseArrayN.add(new Integer(0));
            return new ArrayVisitor(caseArrayN, T, nameRU, values);
        }

        public void visitEnd() {
            if (desc == null) {
                Segment.this.classBands.addAnnotationDefault(nameRU, T, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
            } else if(parameter != -1) {
                Segment.this.classBands.addParameterAnnotation(parameter, desc, visible, nameRU, T, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
            } else {
                Segment.this.classBands.addAnnotation(context, desc, visible, nameRU, T, values, caseArrayN, nestTypeRS, nestNameRU, nestPairN);
            }
        }

        public void visitEnum(String name, String desc, String value) {
            T.add("e");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            values.add(desc);
            values.add(value);
        }
    }
    
    public class ArrayVisitor implements AnnotationVisitor  {
        
        private int indexInCaseArrayN;
        private List caseArrayN;
        private List values;
        private List nameRU;
        private List T;

        public ArrayVisitor(List caseArrayN, List T, List nameRU, List values) {
            this.caseArrayN = caseArrayN;
            this.T = T;
            this.nameRU = nameRU;
            this.values = values;
            this.indexInCaseArrayN = caseArrayN.size() - 1;
        }
        
        public void visit(String name, Object value) {
            Integer numCases = (Integer) caseArrayN.remove(indexInCaseArrayN);
            caseArrayN.add(indexInCaseArrayN, new Integer(numCases.intValue() + 1));
            if (name == null) {
                name = "";
            }
            addValueAndTag(value, T, values);
        }

        public AnnotationVisitor visitAnnotation(String arg0,
                String arg1) {
            throw new RuntimeException("Not yet supported");
        }

        public AnnotationVisitor visitArray(String name) {
            T.add("[");
            if (name == null) {
                name = "";
            }
            nameRU.add(name);
            caseArrayN.add(new Integer(0));
            return new ArrayVisitor(caseArrayN, T, nameRU, values);
        }

        public void visitEnd() {
        }

        public void visitEnum(String name, String desc, String value) {
            Integer numCases = (Integer) caseArrayN.remove(caseArrayN.size() - 1);
            caseArrayN.add(new Integer(numCases.intValue() + 1));
            T.add("e");
            values.add(desc);
            values.add(value);
        }
    }

    /**
     * SegmentFieldVisitor implements <code>FieldVisitor</code> to visit the
     * metadata relating to fields in a class file.
     */
    public class SegmentFieldVisitor implements FieldVisitor {

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new SegmentAnnotationVisitor(MetadataBandGroup.CONTEXT_FIELD,
                    desc, visible);
        }

        public void visitAttribute(Attribute attribute) {
            if(attribute.isUnknown()) {
                String action = options.getUnknownAttributeAction();
                if(action.equals(PackingOptions.PASS)) {
                    passCurrentClass();
                } else if (action.equals(PackingOptions.ERROR)) {
                    throw new Error("Unknown attribute encountered");
                } // else skip
            } else {
                if(attribute instanceof NewAttribute) {
                    NewAttribute newAttribute = (NewAttribute) attribute;
                    if(newAttribute.isUnknown(AttributeDefinitionBands.CONTEXT_FIELD)) {
                        String action = options.getUnknownFieldAttributeAction(newAttribute.type);
                        if(action.equals(PackingOptions.PASS)) {
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
        }

        public void visitEnd() {
        }
    }

    // helper method for annotation visitors
    private void addValueAndTag(Object value, List T, List values) {
        if(value instanceof Integer) {
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
            values.add(new Integer(((Byte)value).intValue()));
        } else if (value instanceof Character) {
            T.add("C");
            values.add(new Integer(((Character)value).charValue()));
        } else if (value instanceof Short) {
            T.add("S");
            values.add(new Integer(((Short)value).intValue()));
        } else if (value instanceof Boolean) {
            T.add("Z");
            values.add(new Integer(((Boolean)value).booleanValue() ? 1 : 0));
        } else if (value instanceof String) {
            T.add("s");
            values.add(value);
        } else if (value instanceof Type) {
            T.add("c");
            values.add(((Type)value).toString());
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
     * Exception indicating that the class currently being visited contains an
     * unknown attribute, which means that by default the class file needs to be
     * passed through as-is in the file_bands rather than being packed with
     * pack200.
     */
    public static class PassException extends RuntimeException {

    }
}
