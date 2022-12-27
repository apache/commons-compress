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
package org.apache.commons.compress.harmony.unpack200.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.harmony.unpack200.Segment;

public class CodeAttribute extends BCIRenumberedAttribute {

    private static CPUTF8 attributeName;
    public static void setAttributeName(final CPUTF8 attributeName) {
        CodeAttribute.attributeName = attributeName;
    }
    public List<Attribute> attributes = new ArrayList<>();
    // instances
    public List<Integer> byteCodeOffsets = new ArrayList<>();
    public List<ByteCode> byteCodes = new ArrayList<>();
    public int codeLength;
    public List<ExceptionTableEntry> exceptionTable;
    public int maxLocals;

    public int maxStack;

    public CodeAttribute(final int maxStack, final int maxLocals, final byte[] codePacked, final Segment segment,
        final OperandManager operandManager, final List<ExceptionTableEntry> exceptionTable) {
        super(attributeName);
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.codeLength = 0;
        this.exceptionTable = exceptionTable;
        byteCodeOffsets.add(Integer.valueOf(0));
        int byteCodeIndex = 0;
        for (int i = 0; i < codePacked.length; i++) {
            final ByteCode byteCode = ByteCode.getByteCode(codePacked[i] & 0xff);
            // Setting the offset must happen before extracting operands
            // because label bytecodes need to know their offsets.
            byteCode.setByteCodeIndex(byteCodeIndex);
            byteCodeIndex++;
            byteCode.extractOperands(operandManager, segment, codeLength);
            byteCodes.add(byteCode);
            codeLength += byteCode.getLength();
            final int lastBytecodePosition = byteCodeOffsets.get(byteCodeOffsets.size() - 1).intValue();
            // This code assumes all multiple byte bytecodes are
            // replaced by a single-byte bytecode followed by
            // another bytecode.
            if (byteCode.hasMultipleByteCodes()) {
                byteCodeOffsets.add(Integer.valueOf(lastBytecodePosition + 1));
                byteCodeIndex++;
            }
            // I've already added the first element (at 0) before
            // entering this loop, so make sure I don't add one
            // after the last element.
            if (i < (codePacked.length - 1)) {
                byteCodeOffsets.add(Integer.valueOf(lastBytecodePosition + byteCode.getLength()));
            }
            if (byteCode.getOpcode() == 0xC4) {
                // Special processing for wide bytecode - it knows what its
                // instruction is from the opcode manager, so ignore the
                // next instruction
                i++;
            }
        }
        // Now that all the bytecodes know their positions and
        // sizes, fix up the byte code targets
        // At this point, byteCodes may be a different size than
        // codePacked because of wide bytecodes.
        for (final ByteCode byteCode : byteCodes) {
            byteCode.applyByteCodeTargetFixup(this);
        }
    }

    public void addAttribute(final Attribute attribute) {
        attributes.add(attribute);
        if (attribute instanceof LocalVariableTableAttribute) {
            ((LocalVariableTableAttribute) attribute).setCodeLength(codeLength);
        }
        if (attribute instanceof LocalVariableTypeTableAttribute) {
            ((LocalVariableTypeTableAttribute) attribute).setCodeLength(codeLength);
        }
    }

    @Override
    protected int getLength() {
        int attributesSize = 0;
        for (final Attribute attribute : attributes) {
            attributesSize += attribute.getLengthIncludingHeader();
        }
        return 2 + 2 + 4 + codeLength + 2 + exceptionTable.size() * (2 + 2 + 2 + 2) + 2 + attributesSize;
    }

    @Override
    protected ClassFileEntry[] getNestedClassFileEntries() {
        final List<ClassFileEntry> nestedEntries = new ArrayList<>(attributes.size() + byteCodes.size() + 10);
        nestedEntries.add(getAttributeName());
        nestedEntries.addAll(byteCodes);
        nestedEntries.addAll(attributes);
        // Don't forget to add the ExceptionTable catch_types
        for (final ExceptionTableEntry entry : exceptionTable) {
            final CPClass catchType = entry.getCatchType();
            // If the catch type is null, this is a finally
            // block. If it's not null, we need to add the
            // CPClass to the list of nested class file entries.
            if (catchType != null) {
                nestedEntries.add(catchType);
            }
        }
        return nestedEntries.toArray(ClassFileEntry.NONE);
    }

    @Override
    protected int[] getStartPCs() {
        // Do nothing here as we've overriden renumber
        return null;
    }

    @Override
    public void renumber(final List<Integer> byteCodeOffsets) {
        exceptionTable.forEach(entry -> entry.renumber(byteCodeOffsets));
    }

    @Override
    protected void resolve(final ClassConstantPool pool) {
        super.resolve(pool);
        attributes.forEach(attribute -> attribute.resolve(pool));
        byteCodes.forEach(byteCode -> byteCode.resolve(pool));
        exceptionTable.forEach(byteCode -> byteCode.resolve(pool));
    }

    @Override
    public String toString() {
        return "Code: " + getLength() + " bytes";
    }

    @Override
    protected void writeBody(final DataOutputStream dos) throws IOException {
        dos.writeShort(maxStack);
        dos.writeShort(maxLocals);

        dos.writeInt(codeLength);
        for (final ByteCode byteCode : byteCodes) {
            byteCode.write(dos);
        }

        dos.writeShort(exceptionTable.size());
        for (final ExceptionTableEntry entry : exceptionTable) {
            entry.write(dos);
        }

        dos.writeShort(attributes.size());
        for (final Attribute attribute : attributes) {
            attribute.write(dos);
        }
    }
}