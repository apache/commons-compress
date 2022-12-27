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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Type;

/**
 * Pack200 Constant Pool Bands
 */
public class CpBands extends BandSet {

    // Don't need to include default attribute names in the constant pool bands
    private final Set<String> defaultAttributeNames = new HashSet<>();

    private final Set<CPUTF8> cp_Utf8 = new TreeSet<>();
    private final Set<CPInt> cp_Int = new TreeSet<>();
    private final Set<CPFloat> cp_Float = new TreeSet<>();
    private final Set<CPLong> cp_Long = new TreeSet<>();
    private final Set<CPDouble> cp_Double = new TreeSet<>();
    private final Set<CPString> cp_String = new TreeSet<>();
    private final Set<CPClass> cp_Class = new TreeSet<>();
    private final Set<CPSignature> cp_Signature = new TreeSet<>();
    private final Set<CPNameAndType> cp_Descr = new TreeSet<>();
    private final Set<CPMethodOrField> cp_Field = new TreeSet<>();
    private final Set<CPMethodOrField> cp_Method = new TreeSet<>();
    private final Set<CPMethodOrField> cp_Imethod = new TreeSet<>();

    private final Map<String, CPUTF8> stringsToCpUtf8 = new HashMap<>();
    private final Map<String, CPNameAndType> stringsToCpNameAndType = new HashMap<>();
    private final Map<String, CPClass> stringsToCpClass = new HashMap<>();
    private final Map<String, CPSignature> stringsToCpSignature = new HashMap<>();
    private final Map<String, CPMethodOrField> stringsToCpMethod = new HashMap<>();
    private final Map<String, CPMethodOrField> stringsToCpField = new HashMap<>();
    private final Map<String, CPMethodOrField> stringsToCpIMethod = new HashMap<>();

    private final Map<Object, CPConstant<?>> objectsToCPConstant = new HashMap<>();

    private final Segment segment;

    public CpBands(final Segment segment, final int effort) {
        super(effort, segment.getSegmentHeader());
        this.segment = segment;
        defaultAttributeNames.add("AnnotationDefault");
        defaultAttributeNames.add("RuntimeVisibleAnnotations");
        defaultAttributeNames.add("RuntimeInvisibleAnnotations");
        defaultAttributeNames.add("RuntimeVisibleParameterAnnotations");
        defaultAttributeNames.add("RuntimeInvisibleParameterAnnotations");
        defaultAttributeNames.add("Code");
        defaultAttributeNames.add("LineNumberTable");
        defaultAttributeNames.add("LocalVariableTable");
        defaultAttributeNames.add("LocalVariableTypeTable");
        defaultAttributeNames.add("ConstantValue");
        defaultAttributeNames.add("Deprecated");
        defaultAttributeNames.add("EnclosingMethod");
        defaultAttributeNames.add("Exceptions");
        defaultAttributeNames.add("InnerClasses");
        defaultAttributeNames.add("Signature");
        defaultAttributeNames.add("SourceFile");
    }

    private void addCharacters(final List<Character> chars, final char[] charArray) {
        for (final char element : charArray) {
            chars.add(Character.valueOf(element));
        }
    }

    public void addCPClass(final String className) {
        getCPClass(className);
    }

    void addCPUtf8(final String utf8) {
        getCPUtf8(utf8);
    }

    private void addIndices() {
		for (final Set<? extends ConstantPoolEntry> set : Arrays.asList(cp_Utf8, cp_Int, cp_Float, cp_Long, cp_Double,
				cp_String, cp_Class, cp_Signature, cp_Descr, cp_Field, cp_Method, cp_Imethod)) {
			int j = 0;
			for (final ConstantPoolEntry entry : set) {
				entry.setIndex(j);
				j++;
			}
		}
		final Map<CPClass, Integer> classNameToIndex = new HashMap<>();
		cp_Field.forEach(mOrF -> {
			final CPClass cpClassName = mOrF.getClassName();
			final Integer index = classNameToIndex.get(cpClassName);
			if (index == null) {
				classNameToIndex.put(cpClassName, Integer.valueOf(1));
				mOrF.setIndexInClass(0);
			} else {
				final int theIndex = index.intValue();
				mOrF.setIndexInClass(theIndex);
				classNameToIndex.put(cpClassName, Integer.valueOf(theIndex + 1));
			}
		});
		classNameToIndex.clear();
		final Map<CPClass, Integer> classNameToConstructorIndex = new HashMap<>();
		cp_Method.forEach(mOrF -> {
			final CPClass cpClassName = mOrF.getClassName();
			final Integer index = classNameToIndex.get(cpClassName);
			if (index == null) {
				classNameToIndex.put(cpClassName, Integer.valueOf(1));
				mOrF.setIndexInClass(0);
			} else {
				final int theIndex = index.intValue();
				mOrF.setIndexInClass(theIndex);
				classNameToIndex.put(cpClassName, Integer.valueOf(theIndex + 1));
			}
			if (mOrF.getDesc().getName().equals("<init>")) {
				final Integer constructorIndex = classNameToConstructorIndex.get(cpClassName);
				if (constructorIndex == null) {
					classNameToConstructorIndex.put(cpClassName, Integer.valueOf(1));
					mOrF.setIndexInClassForConstructor(0);
				} else {
					final int theIndex = constructorIndex.intValue();
					mOrF.setIndexInClassForConstructor(theIndex);
					classNameToConstructorIndex.put(cpClassName, Integer.valueOf(theIndex + 1));
				}
			}
		});
	}

    public boolean existsCpClass(final String className) {
        final CPClass cpClass = stringsToCpClass.get(className);
        return cpClass != null;
    }

    /**
     * All input classes for the segment have now been read in, so this method is called so that this class can
     * calculate/complete anything it could not do while classes were being read.
     */
    public void finaliseBands() {
        addCPUtf8("");
        removeSignaturesFromCpUTF8();
        addIndices();
        segmentHeader.setCp_Utf8_count(cp_Utf8.size());
        segmentHeader.setCp_Int_count(cp_Int.size());
        segmentHeader.setCp_Float_count(cp_Float.size());
        segmentHeader.setCp_Long_count(cp_Long.size());
        segmentHeader.setCp_Double_count(cp_Double.size());
        segmentHeader.setCp_String_count(cp_String.size());
        segmentHeader.setCp_Class_count(cp_Class.size());
        segmentHeader.setCp_Signature_count(cp_Signature.size());
        segmentHeader.setCp_Descr_count(cp_Descr.size());
        segmentHeader.setCp_Field_count(cp_Field.size());
        segmentHeader.setCp_Method_count(cp_Method.size());
        segmentHeader.setCp_Imethod_count(cp_Imethod.size());
    }

    public CPConstant<?> getConstant(final Object value) {
        CPConstant<?> constant = objectsToCPConstant.get(value);
        if (constant == null) {
            if (value instanceof Integer) {
                constant = new CPInt(((Integer) value).intValue());
                cp_Int.add((CPInt) constant);
            } else if (value instanceof Long) {
                constant = new CPLong(((Long) value).longValue());
                cp_Long.add((CPLong) constant);
            } else if (value instanceof Float) {
                constant = new CPFloat(((Float) value).floatValue());
                cp_Float.add((CPFloat) constant);
            } else if (value instanceof Double) {
                constant = new CPDouble(((Double) value).doubleValue());
                cp_Double.add((CPDouble) constant);
            } else if (value instanceof String) {
                constant = new CPString(getCPUtf8((String) value));
                cp_String.add((CPString) constant);
            } else if (value instanceof Type) {
                String className = ((Type) value).getClassName();
                if (className.endsWith("[]")) {
                    className = "[L" + className.substring(0, className.length() - 2);
                    while (className.endsWith("[]")) {
                        className = "[" + className.substring(0, className.length() - 2);
                    }
                    className += ";";
                }
                constant = getCPClass(className);
            }
            objectsToCPConstant.put(value, constant);
        }
        return constant;
    }

    public CPClass getCPClass(String className) {
        if (className == null) {
            return null;
        }
        className = className.replace('.', '/');
        CPClass cpClass = stringsToCpClass.get(className);
        if (cpClass == null) {
            final CPUTF8 cpUtf8 = getCPUtf8(className);
            cpClass = new CPClass(cpUtf8);
            cp_Class.add(cpClass);
            stringsToCpClass.put(className, cpClass);
        }
        if (cpClass.isInnerClass()) {
            segment.getClassBands().currentClassReferencesInnerClass(cpClass);
        }
        return cpClass;
    }

    public CPMethodOrField getCPField(final CPClass cpClass, final String name, final String desc) {
        final String key = cpClass.toString() + ":" + name + ":" + desc;
        CPMethodOrField cpF = stringsToCpField.get(key);
        if (cpF == null) {
            final CPNameAndType nAndT = getCPNameAndType(name, desc);
            cpF = new CPMethodOrField(cpClass, nAndT);
            cp_Field.add(cpF);
            stringsToCpField.put(key, cpF);
        }
        return cpF;
    }

    public CPMethodOrField getCPField(final String owner, final String name, final String desc) {
        return getCPField(getCPClass(owner), name, desc);
    }

    public CPMethodOrField getCPIMethod(final CPClass cpClass, final String name, final String desc) {
        final String key = cpClass.toString() + ":" + name + ":" + desc;
        CPMethodOrField cpIM = stringsToCpIMethod.get(key);
        if (cpIM == null) {
            final CPNameAndType nAndT = getCPNameAndType(name, desc);
            cpIM = new CPMethodOrField(cpClass, nAndT);
            cp_Imethod.add(cpIM);
            stringsToCpIMethod.put(key, cpIM);
        }
        return cpIM;
    }

    public CPMethodOrField getCPIMethod(final String owner, final String name, final String desc) {
        return getCPIMethod(getCPClass(owner), name, desc);
    }

    public CPMethodOrField getCPMethod(final CPClass cpClass, final String name, final String desc) {
        final String key = cpClass.toString() + ":" + name + ":" + desc;
        CPMethodOrField cpM = stringsToCpMethod.get(key);
        if (cpM == null) {
            final CPNameAndType nAndT = getCPNameAndType(name, desc);
            cpM = new CPMethodOrField(cpClass, nAndT);
            cp_Method.add(cpM);
            stringsToCpMethod.put(key, cpM);
        }
        return cpM;
    }

    public CPMethodOrField getCPMethod(final String owner, final String name, final String desc) {
        return getCPMethod(getCPClass(owner), name, desc);
    }

	public CPNameAndType getCPNameAndType(final String name, final String signature) {
        final String descr = name + ":" + signature;
        CPNameAndType nameAndType = stringsToCpNameAndType.get(descr);
        if (nameAndType == null) {
            nameAndType = new CPNameAndType(getCPUtf8(name), getCPSignature(signature));
            stringsToCpNameAndType.put(descr, nameAndType);
            cp_Descr.add(nameAndType);
        }
        return nameAndType;
    }

    public CPSignature getCPSignature(final String signature) {
        if (signature == null) {
            return null;
        }
        CPSignature cpS = stringsToCpSignature.get(signature);
        if (cpS == null) {
            final List<CPClass> cpClasses = new ArrayList<>();
            CPUTF8 signatureUTF8;
            if (signature.length() > 1 && signature.indexOf('L') != -1) {
                final List<String> classes = new ArrayList<>();
                final char[] chars = signature.toCharArray();
                final StringBuilder signatureString = new StringBuilder();
                for (int i = 0; i < chars.length; i++) {
                    signatureString.append(chars[i]);
                    if (chars[i] == 'L') {
                        final StringBuilder className = new StringBuilder();
                        for (int j = i + 1; j < chars.length; j++) {
                            final char c = chars[j];
                            if (!Character.isLetter(c) && !Character.isDigit(c) && (c != '/') && (c != '$')
                                && (c != '_')) {
                                classes.add(className.toString());
                                i = j - 1;
                                break;
                            }
                            className.append(c);
                        }
                    }
                }
                removeCpUtf8(signature);
                for (String className : classes) {
                    CPClass cpClass = null;
                    if (className != null) {
                        className = className.replace('.', '/');
                        cpClass = stringsToCpClass.get(className);
                        if (cpClass == null) {
                            final CPUTF8 cpUtf8 = getCPUtf8(className);
                            cpClass = new CPClass(cpUtf8);
                            cp_Class.add(cpClass);
                            stringsToCpClass.put(className, cpClass);
                        }
                    }
                    cpClasses.add(cpClass);
                }

                signatureUTF8 = getCPUtf8(signatureString.toString());
            } else {
                signatureUTF8 = getCPUtf8(signature);
            }
            cpS = new CPSignature(signature, signatureUTF8, cpClasses);
            cp_Signature.add(cpS);
            stringsToCpSignature.put(signature, cpS);
        }
        return cpS;
    }

    public CPUTF8 getCPUtf8(final String utf8) {
        if (utf8 == null) {
            return null;
        }
        CPUTF8 cpUtf8 = stringsToCpUtf8.get(utf8);
        if (cpUtf8 == null) {
            cpUtf8 = new CPUTF8(utf8);
            cp_Utf8.add(cpUtf8);
            stringsToCpUtf8.put(utf8, cpUtf8);
        }
        return cpUtf8;
    }

    @Override
    public void pack(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing constant pool bands...");
        writeCpUtf8(out);
        writeCpInt(out);
        writeCpFloat(out);
        writeCpLong(out);
        writeCpDouble(out);
        writeCpString(out);
        writeCpClass(out);
        writeCpSignature(out);
        writeCpDescr(out);
        writeCpMethodOrField(cp_Field, out, "cp_Field");
        writeCpMethodOrField(cp_Method, out, "cp_Method");
        writeCpMethodOrField(cp_Imethod, out, "cp_Imethod");
    }

    private void removeCpUtf8(final String string) {
        final CPUTF8 utf8 = stringsToCpUtf8.get(string);
        if ((utf8 != null) && (stringsToCpClass.get(string) == null)) { // don't remove if strings are also in cpclass
            stringsToCpUtf8.remove(string);
            cp_Utf8.remove(utf8);
        }
    }

    private void removeSignaturesFromCpUTF8() {
        cp_Signature.forEach(signature -> {
            final String sigStr = signature.getUnderlyingString();
            final CPUTF8 utf8 = signature.getSignatureForm();
            final String form = utf8.getUnderlyingString();
            if (!sigStr.equals(form)) {
                removeCpUtf8(sigStr);
            }
        });
    }

    private void writeCpClass(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Class.size() + " Class entries...");
        final int[] cpClass = new int[cp_Class.size()];
        int i = 0;
        for (final CPClass cpCl : cp_Class) {
            cpClass[i] = cpCl.getIndexInCpUtf8();
            i++;
        }
        final byte[] encodedBand = encodeBandInt("cpClass", cpClass, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpClass[" + cpClass.length + "]");
    }

    private void writeCpDescr(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Descr.size() + " Descriptor entries...");
        final int[] cpDescrName = new int[cp_Descr.size()];
        final int[] cpDescrType = new int[cp_Descr.size()];
        int i = 0;
        for (final CPNameAndType nameAndType : cp_Descr) {
            cpDescrName[i] = nameAndType.getNameIndex();
            cpDescrType[i] = nameAndType.getTypeIndex();
            i++;
        }

        byte[] encodedBand = encodeBandInt("cp_Descr_Name", cpDescrName, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Descr_Name[" + cpDescrName.length + "]");

        encodedBand = encodeBandInt("cp_Descr_Type", cpDescrType, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Descr_Type[" + cpDescrType.length + "]");
    }

    private void writeCpDouble(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Double.size() + " Double entries...");
        final int[] highBits = new int[cp_Double.size()];
        final int[] loBits = new int[cp_Double.size()];
        int i = 0;
        for (final CPDouble dbl : cp_Double) {
            final long l = Double.doubleToLongBits(dbl.getDouble());
            highBits[i] = (int) (l >> 32);
            loBits[i] = (int) l;
            i++;
        }
        byte[] encodedBand = encodeBandInt("cp_Double_hi", highBits, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Double_hi[" + highBits.length + "]");

        encodedBand = encodeBandInt("cp_Double_lo", loBits, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Double_lo[" + loBits.length + "]");
    }

    private void writeCpFloat(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Float.size() + " Float entries...");
        final int[] cpFloat = new int[cp_Float.size()];
        int i = 0;
        for (final CPFloat fl : cp_Float) {
            cpFloat[i] = Float.floatToIntBits(fl.getFloat());
            i++;
        }
        final byte[] encodedBand = encodeBandInt("cp_Float", cpFloat, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Float[" + cpFloat.length + "]");
    }

    private void writeCpInt(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Int.size() + " Integer entries...");
        final int[] cpInt = new int[cp_Int.size()];
        int i = 0;
        for (final CPInt integer : cp_Int) {
            cpInt[i] = integer.getInt();
            i++;
        }
        final byte[] encodedBand = encodeBandInt("cp_Int", cpInt, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Int[" + cpInt.length + "]");
    }

    private void writeCpLong(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Long.size() + " Long entries...");
        final int[] highBits = new int[cp_Long.size()];
        final int[] loBits = new int[cp_Long.size()];
        int i = 0;
        for (final CPLong lng : cp_Long) {
            final long l = lng.getLong();
            highBits[i] = (int) (l >> 32);
            loBits[i] = (int) l;
            i++;
        }
        byte[] encodedBand = encodeBandInt("cp_Long_hi", highBits, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Long_hi[" + highBits.length + "]");

        encodedBand = encodeBandInt("cp_Long_lo", loBits, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cp_Long_lo[" + loBits.length + "]");
    }

    private void writeCpMethodOrField(final Set<CPMethodOrField> cp, final OutputStream out, final String name)
        throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp.size() + " Method and Field entries...");
        final int[] cp_methodOrField_class = new int[cp.size()];
        final int[] cp_methodOrField_desc = new int[cp.size()];
        int i = 0;
        for (final CPMethodOrField mOrF : cp) {
            cp_methodOrField_class[i] = mOrF.getClassIndex();
            cp_methodOrField_desc[i] = mOrF.getDescIndex();
            i++;
        }
        byte[] encodedBand = encodeBandInt(name + "_class", cp_methodOrField_class, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log(
            "Wrote " + encodedBand.length + " bytes from " + name + "_class[" + cp_methodOrField_class.length + "]");

        encodedBand = encodeBandInt(name + "_desc", cp_methodOrField_desc, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils
            .log("Wrote " + encodedBand.length + " bytes from " + name + "_desc[" + cp_methodOrField_desc.length + "]");
    }

    private void writeCpSignature(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Signature.size() + " Signature entries...");
        final int[] cpSignatureForm = new int[cp_Signature.size()];
        final List<CPClass> classes = new ArrayList<>();
        int i = 0;
        for (final CPSignature cpS : cp_Signature) {
            classes.addAll(cpS.getClasses());
            cpSignatureForm[i] = cpS.getIndexInCpUtf8();
            i++;
        }
        final int[] cpSignatureClasses = new int[classes.size()];
        Arrays.setAll(cpSignatureClasses, j -> classes.get(j).getIndex());

        byte[] encodedBand = encodeBandInt("cpSignatureForm", cpSignatureForm, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpSignatureForm[" + cpSignatureForm.length + "]");

        encodedBand = encodeBandInt("cpSignatureClasses", cpSignatureClasses, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils
            .log("Wrote " + encodedBand.length + " bytes from cpSignatureClasses[" + cpSignatureClasses.length + "]");
    }

    private void writeCpString(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_String.size() + " String entries...");
        final int[] cpString = new int[cp_String.size()];
        int i = 0;
        for (final CPString cpStr : cp_String) {
            cpString[i] = cpStr.getIndexInCpUtf8();
            i++;
        }
        final byte[] encodedBand = encodeBandInt("cpString", cpString, Codec.UDELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpString[" + cpString.length + "]");
    }

    private void writeCpUtf8(final OutputStream out) throws IOException, Pack200Exception {
        PackingUtils.log("Writing " + cp_Utf8.size() + " UTF8 entries...");
        final int[] cpUtf8Prefix = new int[cp_Utf8.size() - 2];
        final int[] cpUtf8Suffix = new int[cp_Utf8.size() - 1];
        final List<Character> chars = new ArrayList<>();
        final List<Integer> bigSuffix = new ArrayList<>();
        final List<Character> bigChars = new ArrayList<>();
        final Object[] cpUtf8Array = cp_Utf8.toArray();
        final String first = ((CPUTF8) cpUtf8Array[1]).getUnderlyingString();
        cpUtf8Suffix[0] = first.length();
        addCharacters(chars, first.toCharArray());
        for (int i = 2; i < cpUtf8Array.length; i++) {
            final char[] previous = ((CPUTF8) cpUtf8Array[i - 1]).getUnderlyingString().toCharArray();
            String currentStr = ((CPUTF8) cpUtf8Array[i]).getUnderlyingString();
            final char[] current = currentStr.toCharArray();
            int prefix = 0;
            for (int j = 0; j < previous.length; j++) {
                if (previous[j] != current[j]) {
                    break;
                }
                prefix++;
            }
            cpUtf8Prefix[i - 2] = prefix;
            currentStr = currentStr.substring(prefix);
            final char[] suffix = currentStr.toCharArray();
            if (suffix.length > 1000) { // big suffix (1000 is arbitrary - can we
                // do better?)
                cpUtf8Suffix[i - 1] = 0;
                bigSuffix.add(Integer.valueOf(suffix.length));
                addCharacters(bigChars, suffix);
            } else {
                cpUtf8Suffix[i - 1] = suffix.length;
                addCharacters(chars, suffix);
            }
        }
        final int[] cpUtf8Chars = new int[chars.size()];
        final int[] cpUtf8BigSuffix = new int[bigSuffix.size()];
        final int[][] cpUtf8BigChars = new int[bigSuffix.size()][];
        Arrays.setAll(cpUtf8Chars, i -> chars.get(i).charValue());
        for (int i = 0; i < cpUtf8BigSuffix.length; i++) {
            final int numBigChars = bigSuffix.get(i).intValue();
            cpUtf8BigSuffix[i] = numBigChars;
            cpUtf8BigChars[i] = new int[numBigChars];
            Arrays.setAll(cpUtf8BigChars[i], j -> bigChars.remove(0).charValue());
        }

        byte[] encodedBand = encodeBandInt("cpUtf8Prefix", cpUtf8Prefix, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpUtf8Prefix[" + cpUtf8Prefix.length + "]");

        encodedBand = encodeBandInt("cpUtf8Suffix", cpUtf8Suffix, Codec.UNSIGNED5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpUtf8Suffix[" + cpUtf8Suffix.length + "]");

        encodedBand = encodeBandInt("cpUtf8Chars", cpUtf8Chars, Codec.CHAR3);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpUtf8Chars[" + cpUtf8Chars.length + "]");

        encodedBand = encodeBandInt("cpUtf8BigSuffix", cpUtf8BigSuffix, Codec.DELTA5);
        out.write(encodedBand);
        PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpUtf8BigSuffix[" + cpUtf8BigSuffix.length + "]");

        for (int i = 0; i < cpUtf8BigChars.length; i++) {
            encodedBand = encodeBandInt("cpUtf8BigChars " + i, cpUtf8BigChars[i], Codec.DELTA5);
            out.write(encodedBand);
            PackingUtils.log("Wrote " + encodedBand.length + " bytes from cpUtf8BigChars" + i + "["
                + cpUtf8BigChars[i].length + "]");
        }
    }

}
