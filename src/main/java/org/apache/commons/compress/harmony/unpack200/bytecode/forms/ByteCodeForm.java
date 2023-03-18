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
package org.apache.commons.compress.harmony.unpack200.bytecode.forms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.harmony.unpack200.bytecode.ByteCode;
import org.apache.commons.compress.harmony.unpack200.bytecode.CodeAttribute;
import org.apache.commons.compress.harmony.unpack200.bytecode.OperandManager;

public abstract class ByteCodeForm {

    protected static final boolean WIDENED = true;

    protected static final ByteCodeForm[] byteCodeArray = new ByteCodeForm[256];
    protected static final Map<String, ByteCodeForm> byteCodesByName = new HashMap<>(256);
    static {
        byteCodeArray[0] = new NoArgumentForm(0, "nop");
        byteCodeArray[1] = new NoArgumentForm(1, "aconst_null");
        byteCodeArray[2] = new NoArgumentForm(2, "iconst_m1");
        byteCodeArray[3] = new NoArgumentForm(3, "iconst_0");
        byteCodeArray[4] = new NoArgumentForm(4, "iconst_1");
        byteCodeArray[5] = new NoArgumentForm(5, "iconst_2");
        byteCodeArray[6] = new NoArgumentForm(6, "iconst_3");
        byteCodeArray[7] = new NoArgumentForm(7, "iconst_4");
        byteCodeArray[8] = new NoArgumentForm(8, "iconst_5");
        byteCodeArray[9] = new NoArgumentForm(9, "lconst_0");
        byteCodeArray[10] = new NoArgumentForm(10, "lconst_1");
        byteCodeArray[11] = new NoArgumentForm(11, "fconst_0");
        byteCodeArray[12] = new NoArgumentForm(12, "fconst_1");
        byteCodeArray[13] = new NoArgumentForm(13, "fconst_2");
        byteCodeArray[14] = new NoArgumentForm(14, "dconst_0");
        byteCodeArray[15] = new NoArgumentForm(15, "dconst_1");
        byteCodeArray[16] = new ByteForm(16, "bipush", new int[] {16, -1});
        byteCodeArray[17] = new ShortForm(17, "sipush", new int[] {17, -1, -1});
        byteCodeArray[18] = new StringRefForm(18, "ldc", new int[] {18, -1});
        byteCodeArray[19] = new StringRefForm(19, "ldc_w", new int[] {19, -1, -1}, WIDENED);
        byteCodeArray[20] = new LongForm(20, "ldc2_w", new int[] {20, -1, -1});
        byteCodeArray[21] = new LocalForm(21, "iload", new int[] {21, -1});
        byteCodeArray[22] = new LocalForm(22, "lload", new int[] {22, -1});
        byteCodeArray[23] = new LocalForm(23, "fload", new int[] {23, -1});
        byteCodeArray[24] = new LocalForm(24, "dload", new int[] {24, -1});
        byteCodeArray[25] = new LocalForm(25, "aload", new int[] {25, -1});
        byteCodeArray[26] = new NoArgumentForm(26, "iload_0");
        byteCodeArray[27] = new NoArgumentForm(27, "iload_1");
        byteCodeArray[28] = new NoArgumentForm(28, "iload_2");
        byteCodeArray[29] = new NoArgumentForm(29, "iload_3");
        byteCodeArray[30] = new NoArgumentForm(30, "lload_0");
        byteCodeArray[31] = new NoArgumentForm(31, "lload_1");
        byteCodeArray[32] = new NoArgumentForm(32, "lload_2");
        byteCodeArray[33] = new NoArgumentForm(33, "lload_3");
        byteCodeArray[34] = new NoArgumentForm(34, "fload_0");
        byteCodeArray[35] = new NoArgumentForm(35, "fload_1");
        byteCodeArray[36] = new NoArgumentForm(36, "fload_2");
        byteCodeArray[37] = new NoArgumentForm(37, "fload_3");
        byteCodeArray[38] = new NoArgumentForm(38, "dload_0");
        byteCodeArray[39] = new NoArgumentForm(39, "dload_1");
        byteCodeArray[40] = new NoArgumentForm(40, "dload_2");
        byteCodeArray[41] = new NoArgumentForm(41, "dload_3");
        byteCodeArray[42] = new NoArgumentForm(42, "aload_0");
        byteCodeArray[43] = new NoArgumentForm(43, "aload_1");
        byteCodeArray[44] = new NoArgumentForm(44, "aload_2");
        byteCodeArray[45] = new NoArgumentForm(45, "aload_3");
        byteCodeArray[46] = new NoArgumentForm(46, "iaload");
        byteCodeArray[47] = new NoArgumentForm(47, "laload");
        byteCodeArray[48] = new NoArgumentForm(48, "faload");
        byteCodeArray[49] = new NoArgumentForm(49, "daload");
        byteCodeArray[50] = new NoArgumentForm(50, "aaload");
        byteCodeArray[51] = new NoArgumentForm(51, "baload");
        byteCodeArray[52] = new NoArgumentForm(52, "caload");
        byteCodeArray[53] = new NoArgumentForm(53, "saload");
        byteCodeArray[54] = new LocalForm(54, "istore", new int[] {54, -1});
        byteCodeArray[55] = new LocalForm(55, "lstore", new int[] {55, -1});
        byteCodeArray[56] = new LocalForm(56, "fstore", new int[] {56, -1});
        byteCodeArray[57] = new LocalForm(57, "dstore", new int[] {57, -1});
        byteCodeArray[58] = new LocalForm(58, "astore", new int[] {58, -1});
        byteCodeArray[59] = new NoArgumentForm(59, "istore_0");
        byteCodeArray[60] = new NoArgumentForm(60, "istore_1");
        byteCodeArray[61] = new NoArgumentForm(61, "istore_2");
        byteCodeArray[62] = new NoArgumentForm(62, "istore_3");
        byteCodeArray[63] = new NoArgumentForm(63, "lstore_0");
        byteCodeArray[64] = new NoArgumentForm(64, "lstore_1");
        byteCodeArray[65] = new NoArgumentForm(65, "lstore_2");
        byteCodeArray[66] = new NoArgumentForm(66, "lstore_3");
        byteCodeArray[67] = new NoArgumentForm(67, "fstore_0");
        byteCodeArray[68] = new NoArgumentForm(68, "fstore_1");
        byteCodeArray[69] = new NoArgumentForm(69, "fstore_2");
        byteCodeArray[70] = new NoArgumentForm(70, "fstore_3");
        byteCodeArray[71] = new NoArgumentForm(71, "dstore_0");
        byteCodeArray[72] = new NoArgumentForm(72, "dstore_1");
        byteCodeArray[73] = new NoArgumentForm(73, "dstore_2");
        byteCodeArray[74] = new NoArgumentForm(74, "dstore_3");
        byteCodeArray[75] = new NoArgumentForm(75, "astore_0");
        byteCodeArray[76] = new NoArgumentForm(76, "astore_1");
        byteCodeArray[77] = new NoArgumentForm(77, "astore_2");
        byteCodeArray[78] = new NoArgumentForm(78, "astore_3");
        byteCodeArray[79] = new NoArgumentForm(79, "iastore");
        byteCodeArray[80] = new NoArgumentForm(80, "lastore");
        byteCodeArray[81] = new NoArgumentForm(81, "fastore");
        byteCodeArray[82] = new NoArgumentForm(82, "dastore");
        byteCodeArray[83] = new NoArgumentForm(83, "aastore");
        byteCodeArray[84] = new NoArgumentForm(84, "bastore");
        byteCodeArray[85] = new NoArgumentForm(85, "castore");
        byteCodeArray[86] = new NoArgumentForm(86, "sastore");
        byteCodeArray[87] = new NoArgumentForm(87, "pop");
        byteCodeArray[88] = new NoArgumentForm(88, "pop2");
        byteCodeArray[89] = new NoArgumentForm(89, "dup");
        byteCodeArray[90] = new NoArgumentForm(90, "dup_x1");
        byteCodeArray[91] = new NoArgumentForm(91, "dup_x2");
        byteCodeArray[92] = new NoArgumentForm(92, "dup2");
        byteCodeArray[93] = new NoArgumentForm(93, "dup2_x1");
        byteCodeArray[94] = new NoArgumentForm(94, "dup2_x2");
        byteCodeArray[95] = new NoArgumentForm(95, "swap");
        byteCodeArray[96] = new NoArgumentForm(96, "iadd");
        byteCodeArray[97] = new NoArgumentForm(97, "ladd");
        byteCodeArray[98] = new NoArgumentForm(98, "fadd");
        byteCodeArray[99] = new NoArgumentForm(99, "dadd");
        byteCodeArray[100] = new NoArgumentForm(100, "isub");
        byteCodeArray[101] = new NoArgumentForm(101, "lsub");
        byteCodeArray[102] = new NoArgumentForm(102, "fsub");
        byteCodeArray[103] = new NoArgumentForm(103, "dsub");
        byteCodeArray[104] = new NoArgumentForm(104, "imul");
        byteCodeArray[105] = new NoArgumentForm(105, "lmul");
        byteCodeArray[106] = new NoArgumentForm(106, "fmul");
        byteCodeArray[107] = new NoArgumentForm(107, "dmul");
        byteCodeArray[108] = new NoArgumentForm(108, "idiv");
        byteCodeArray[109] = new NoArgumentForm(109, "ldiv");
        byteCodeArray[110] = new NoArgumentForm(110, "fdiv");
        byteCodeArray[111] = new NoArgumentForm(111, "ddiv");
        byteCodeArray[112] = new NoArgumentForm(112, "irem");
        byteCodeArray[113] = new NoArgumentForm(113, "lrem");
        byteCodeArray[114] = new NoArgumentForm(114, "frem");
        byteCodeArray[115] = new NoArgumentForm(115, "drem");
        byteCodeArray[116] = new NoArgumentForm(116, "");
        byteCodeArray[117] = new NoArgumentForm(117, "lneg");
        byteCodeArray[118] = new NoArgumentForm(118, "fneg");
        byteCodeArray[119] = new NoArgumentForm(119, "dneg");
        byteCodeArray[120] = new NoArgumentForm(120, "ishl");
        byteCodeArray[121] = new NoArgumentForm(121, "lshl");
        byteCodeArray[122] = new NoArgumentForm(122, "ishr");
        byteCodeArray[123] = new NoArgumentForm(123, "lshr");
        byteCodeArray[124] = new NoArgumentForm(124, "iushr");
        byteCodeArray[125] = new NoArgumentForm(125, "lushr");
        byteCodeArray[126] = new NoArgumentForm(126, "iand");
        byteCodeArray[127] = new NoArgumentForm(127, "land");
        byteCodeArray[128] = new NoArgumentForm(128, "ior");
        byteCodeArray[129] = new NoArgumentForm(129, "lor");
        byteCodeArray[130] = new NoArgumentForm(130, "ixor");
        byteCodeArray[131] = new NoArgumentForm(131, "lxor");
        byteCodeArray[132] = new IincForm(132, "iinc", new int[] {132, -1, -1});
        byteCodeArray[133] = new NoArgumentForm(133, "i2l");
        byteCodeArray[134] = new NoArgumentForm(134, "i2f");
        byteCodeArray[135] = new NoArgumentForm(135, "i2d");
        byteCodeArray[136] = new NoArgumentForm(136, "l2i");
        byteCodeArray[137] = new NoArgumentForm(137, "l2f");
        byteCodeArray[138] = new NoArgumentForm(138, "l2d");
        byteCodeArray[139] = new NoArgumentForm(139, "f2i");
        byteCodeArray[140] = new NoArgumentForm(140, "f2l");
        byteCodeArray[141] = new NoArgumentForm(141, "f2d");
        byteCodeArray[142] = new NoArgumentForm(142, "d2i");
        byteCodeArray[143] = new NoArgumentForm(143, "d2l");
        byteCodeArray[144] = new NoArgumentForm(144, "d2f");
        byteCodeArray[145] = new NoArgumentForm(145, "i2b");
        byteCodeArray[146] = new NoArgumentForm(146, "i2c");
        byteCodeArray[147] = new NoArgumentForm(147, "i2s");
        byteCodeArray[148] = new NoArgumentForm(148, "lcmp");
        byteCodeArray[149] = new NoArgumentForm(149, "fcmpl");
        byteCodeArray[150] = new NoArgumentForm(150, "fcmpg");
        byteCodeArray[151] = new NoArgumentForm(151, "dcmpl");
        byteCodeArray[152] = new NoArgumentForm(152, "dcmpg");
        byteCodeArray[153] = new LabelForm(153, "ifeq", new int[] {153, -1, -1});
        byteCodeArray[154] = new LabelForm(154, "ifne", new int[] {154, -1, -1});
        byteCodeArray[155] = new LabelForm(155, "iflt", new int[] {155, -1, -1});
        byteCodeArray[156] = new LabelForm(156, "ifge", new int[] {156, -1, -1});
        byteCodeArray[157] = new LabelForm(157, "ifgt", new int[] {157, -1, -1});
        byteCodeArray[158] = new LabelForm(158, "ifle", new int[] {158, -1, -1});
        byteCodeArray[159] = new LabelForm(159, "if_icmpeq", new int[] {159, -1, -1});
        byteCodeArray[160] = new LabelForm(160, "if_icmpne", new int[] {160, -1, -1});
        byteCodeArray[161] = new LabelForm(161, "if_icmplt", new int[] {161, -1, -1});
        byteCodeArray[162] = new LabelForm(162, "if_icmpge", new int[] {162, -1, -1});
        byteCodeArray[163] = new LabelForm(163, "if_icmpgt", new int[] {163, -1, -1});
        byteCodeArray[164] = new LabelForm(164, "if_icmple", new int[] {164, -1, -1});
        byteCodeArray[165] = new LabelForm(165, "if_acmpeq", new int[] {165, -1, -1});
        byteCodeArray[166] = new LabelForm(166, "if_acmpne", new int[] {166, -1, -1});
        byteCodeArray[167] = new LabelForm(167, "goto", new int[] {167, -1, -1});
        byteCodeArray[168] = new LabelForm(168, "jsr", new int[] {168, -1, -1});
        byteCodeArray[169] = new LocalForm(169, "ret", new int[] {169, -1});
        byteCodeArray[170] = new TableSwitchForm(170, "tableswitch");
        byteCodeArray[171] = new LookupSwitchForm(171, "lookupswitch");
        byteCodeArray[172] = new NoArgumentForm(172, "ireturn");
        byteCodeArray[173] = new NoArgumentForm(173, "lreturn");
        byteCodeArray[174] = new NoArgumentForm(174, "freturn");
        byteCodeArray[175] = new NoArgumentForm(175, "dreturn");
        byteCodeArray[176] = new NoArgumentForm(176, "areturn");
        byteCodeArray[177] = new NoArgumentForm(177, "return");
        byteCodeArray[178] = new FieldRefForm(178, "getstatic", new int[] {178, -1, -1});
        byteCodeArray[179] = new FieldRefForm(179, "putstatic", new int[] {179, -1, -1});
        byteCodeArray[180] = new FieldRefForm(180, "getfield", new int[] {180, -1, -1});
        byteCodeArray[181] = new FieldRefForm(181, "putfield", new int[] {181, -1, -1});
        byteCodeArray[182] = new MethodRefForm(182, "invokevirtual", new int[] {182, -1, -1});
        byteCodeArray[183] = new MethodRefForm(183, "invokespecial", new int[] {183, -1, -1});
        byteCodeArray[184] = new MethodRefForm(184, "invokestatic", new int[] {184, -1, -1});
        byteCodeArray[185] = new IMethodRefForm(185, "invokeinterface", new int[] {185, -1, -1, /* count */-1, 0});
        byteCodeArray[186] = new NoArgumentForm(186, "xxxunusedxxx");
        byteCodeArray[187] = new NewClassRefForm(187, "new", new int[] {187, -1, -1});
        byteCodeArray[188] = new ByteForm(188, "newarray", new int[] {188, -1});
        byteCodeArray[189] = new ClassRefForm(189, "anewarray", new int[] {189, -1, -1});
        byteCodeArray[190] = new NoArgumentForm(190, "arraylength");
        byteCodeArray[191] = new NoArgumentForm(191, "athrow");
        byteCodeArray[192] = new ClassRefForm(192, "checkcast", new int[] {192, -1, -1});
        byteCodeArray[193] = new ClassRefForm(193, "instanceof", new int[] {193, -1, -1});
        byteCodeArray[194] = new NoArgumentForm(194, "monitorenter");
        byteCodeArray[195] = new NoArgumentForm(195, "monitorexit");
        byteCodeArray[196] = new WideForm(196, "wide");
        byteCodeArray[197] = new MultiANewArrayForm(197, "multianewarray", new int[] {197, -1, -1, -1});
        byteCodeArray[198] = new LabelForm(198, "ifnull", new int[] {198, -1, -1});
        byteCodeArray[199] = new LabelForm(199, "ifnonnull", new int[] {199, -1, -1});
        byteCodeArray[200] = new LabelForm(200, "goto_w", new int[] {200, -1, -1, -1, -1}, WIDENED);
        byteCodeArray[201] = new LabelForm(201, "jsr_w", new int[] {201, -1, -1, -1, -1}, WIDENED);

        // Extra ones defined by pack200
        byteCodeArray[202] = new ThisFieldRefForm(202, "getstatic_this", new int[] {178, -1, -1});
        byteCodeArray[203] = new ThisFieldRefForm(203, "putstatic_this", new int[] {179, -1, -1});
        byteCodeArray[204] = new ThisFieldRefForm(204, "getfield_this", new int[] {180, -1, -1});
        byteCodeArray[205] = new ThisFieldRefForm(205, "putfield_this", new int[] {181, -1, -1});
        byteCodeArray[206] = new ThisMethodRefForm(206, "invokevirtual_this", new int[] {182, -1, -1});
        byteCodeArray[207] = new ThisMethodRefForm(207, "invokespecial_this", new int[] {183, -1, -1});
        byteCodeArray[208] = new ThisMethodRefForm(208, "invokestatic_this", new int[] {184, -1, -1});
        byteCodeArray[209] = new ThisFieldRefForm(209, "aload_0_getstatic_this", new int[] {42, 178, -1, -1});
        byteCodeArray[210] = new ThisFieldRefForm(210, "aload_0_putstatic_this", new int[] {42, 179, -1, -1});
        byteCodeArray[211] = new ThisFieldRefForm(211, "aload_0_getfield_this", new int[] {42, 180, -1, -1});
        byteCodeArray[212] = new ThisFieldRefForm(212, "aload_0_putfield_this", new int[] {42, 181, -1, -1});
        byteCodeArray[213] = new ThisMethodRefForm(213, "aload_0_invokevirtual_this", new int[] {42, 182, -1, -1});
        byteCodeArray[214] = new ThisMethodRefForm(214, "aload_0_invokespecial_this", new int[] {42, 183, -1, -1});
        byteCodeArray[215] = new ThisMethodRefForm(215, "aload_0_invokestatic_this", new int[] {42, 184, -1, -1});
        byteCodeArray[216] = new SuperFieldRefForm(216, "getstatic_super", new int[] {178, -1, -1});
        byteCodeArray[217] = new SuperFieldRefForm(217, "putstatic_super", new int[] {179, -1, -1});
        byteCodeArray[218] = new SuperFieldRefForm(218, "getfield_super", new int[] {180, -1, -1});
        byteCodeArray[219] = new SuperFieldRefForm(219, "putfield_super", new int[] {181, -1, -1});
        byteCodeArray[220] = new SuperMethodRefForm(220, "invokevirtual_super", new int[] {182, -1, -1});
        byteCodeArray[221] = new SuperMethodRefForm(221, "invokespecial_super", new int[] {183, -1, -1});
        byteCodeArray[222] = new SuperMethodRefForm(222, "invokestatic_super", new int[] {184, -1, -1});
        byteCodeArray[223] = new SuperFieldRefForm(223, "aload_0_getstatic_super", new int[] {42, 178, -1, -1});
        byteCodeArray[224] = new SuperFieldRefForm(224, "aload_0_putstatic_super", new int[] {42, 179, -1, -1});
        byteCodeArray[225] = new SuperFieldRefForm(225, "aload_0_getfield_super", new int[] {42, 180, -1, -1});
        byteCodeArray[226] = new SuperFieldRefForm(226, "aload_0_putfield_super", new int[] {42, 181, -1, -1});
        byteCodeArray[227] = new SuperMethodRefForm(227, "aload_0_invokevirtual_super", new int[] {42, 182, -1, -1});
        byteCodeArray[228] = new SuperMethodRefForm(228, "aload_0_invokespecial_super", new int[] {42, 183, -1, -1});
        byteCodeArray[229] = new SuperMethodRefForm(229, "aload_0_invokestatic_super", new int[] {42, 184, -1, -1});
        byteCodeArray[230] = new ThisInitMethodRefForm(230, "invokespecial_this_init", new int[] {183, -1, -1});
        byteCodeArray[231] = new SuperInitMethodRefForm(231, "invokespecial_super_init", new int[] {183, -1, -1});
        byteCodeArray[232] = new NewInitMethodRefForm(232, "invokespecial_new_init", new int[] {183, -1, -1});
        byteCodeArray[233] = new NarrowClassRefForm(233, "cldc", new int[] {18, -1});
        byteCodeArray[234] = new IntRefForm(234, "ildc", new int[] {18, -1});
        byteCodeArray[235] = new FloatRefForm(235, "fldc", new int[] {18, -1});
        byteCodeArray[236] = new NarrowClassRefForm(236, "cldc_w", new int[] {19, -1, -1}, WIDENED);
        byteCodeArray[237] = new IntRefForm(237, "ildc_w", new int[] {19, -1, -1}, WIDENED);
        byteCodeArray[238] = new FloatRefForm(238, "fldc_w", new int[] {19, -1, -1}, WIDENED);
        byteCodeArray[239] = new DoubleForm(239, "dldc2_w", new int[] {20, -1, -1});

        // Reserved bytecodes
        byteCodeArray[254] = new NoArgumentForm(254, "impdep1");
        byteCodeArray[255] = new NoArgumentForm(255, "impdep2");

        // Bytecodes that aren't defined in the spec but are useful when
        // unpacking (all must be >255)
        // maybe wide versions of the others? etc.

        // Put all the bytecodes in a HashMap so we can
        // get them by either name or number
        for (final ByteCodeForm byteCode : byteCodeArray) {
            if (byteCode != null) {
                byteCodesByName.put(byteCode.getName(), byteCode);
            }
        }
    }

    public static ByteCodeForm get(final int opcode) {
        return byteCodeArray[opcode];
    }
    private final int opcode;
    private final String name;
    private final int[] rewrite;
    private int firstOperandIndex;

    private int operandLength;

    /**
     * Answer a new instance of this class with the specified opcode and name. Assume no rewrite.
     *
     * @param opcode int corresponding to the opcode's value
     * @param name String printable name of the opcode
     */
    public ByteCodeForm(final int opcode, final String name) {
        this(opcode, name, new int[] {opcode});
    }

    /**
     * Answer a new instance of this class with the specified opcode, name, operandType and rewrite
     *
     * @param opcode int corresponding to the opcode's value
     * @param name String printable name of the opcode
     * @param rewrite int[] Array of ints. Operand positions (which will later be rewritten in ByteCodes) are indicated
     *        by -1.
     */
    public ByteCodeForm(final int opcode, final String name, final int[] rewrite) {
        this.opcode = opcode;
        this.name = name;
        this.rewrite = rewrite;
        calculateOperandPosition();
    }

    protected void calculateOperandPosition() {
        firstOperandIndex = -1;
        operandLength = -1;

        // Find the first negative number in the rewrite array
        int iterationIndex = 0;
        while (iterationIndex < rewrite.length) {
            if (rewrite[iterationIndex] < 0) {
                // Found the first opcode to substitute
                firstOperandIndex = iterationIndex;
                iterationIndex = rewrite.length;
            } else {
                iterationIndex++;
            }
        }

        if (firstOperandIndex == -1) {
            // Nothing more to do since the opcode has no operands
            return;
        }

        // Find the last negative number in the rewrite array
        int lastOperandIndex = -1;
        iterationIndex = firstOperandIndex;
        while (iterationIndex < rewrite.length) {
            if (rewrite[iterationIndex] < 0) {
                lastOperandIndex = iterationIndex;
            }
            iterationIndex++;
        }

        // Now we have the first index and the last index.
        final int difference = lastOperandIndex - firstOperandIndex;

        // If last < first, something is wrong.
        if (difference < 0) {
            throw new Error("Logic error: not finding rewrite operands correctly");
        }
        operandLength = difference + 1;
    }

    public int firstOperandIndex() {
        return firstOperandIndex;
    }

    /**
     * The ByteCodeForm knows how to fix up a bytecode if it needs to be fixed up because it holds a Label bytecode.
     *
     * @param byteCode a ByteCode to be fixed up
     * @param codeAttribute a CodeAttribute used to determine how the ByteCode should be fixed up.
     */
    public void fixUpByteCodeTargets(final ByteCode byteCode, final CodeAttribute codeAttribute) {
        // Most ByteCodeForms don't have any fixing up to do.
        return;
    }

    public String getName() {
        return name;
    }

    public int getOpcode() {
        return opcode;
    }

    public int[] getRewrite() {
        return rewrite;
    }

    public int[] getRewriteCopy() {
        return Arrays.copyOf(rewrite, rewrite.length);
    }

    /**
     * This method will answer true if the receiver is a multi-bytecode instruction (such as aload0_putfield_super);
     * otherwise, it will answer false.
     *
     * @return boolean true if multibytecode, false otherwise
     */
    public boolean hasMultipleByteCodes() {
        // Currently, all multi-bytecode instructions
        // begin with aload_0, so this is how we test.
        if ((rewrite.length > 1) && (rewrite[0] == 42)) {
            // If there's an instruction (not a negative
            // number, which is an operand) after the
            // aload_0, it's a multibytecode instruction.
            return (rewrite[1] > 0);
        }
        return false;
    }

    public boolean hasNoOperand() {
        return false;
    }

    public boolean nestedMustStartClassPool() {
        return false;
    }

    public int operandLength() {
        return operandLength;
    }

    /**
     * When passed a byteCode, an OperandTable and a SegmentConstantPool, this method will set the rewrite of the
     * byteCode appropriately.
     *
     * @param byteCode ByteCode to be updated (!)
     * @param operandManager OperandTable from which to draw info
     * @param codeLength Length of bytes (excluding this bytecode) from the beginning of the method. Used in calculating
     *        padding for some variable-length bytecodes (such as lookupswitch, tableswitch).
     */
    public abstract void setByteCodeOperands(ByteCode byteCode, OperandManager operandManager, int codeLength);

    @Override
    public String toString() {
        return this.getClass().getName() + "(" + getName() + ")";
    }
}
