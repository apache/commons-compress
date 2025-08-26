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
package org.apache.commons.compress.harmony.unpack200;

import org.apache.commons.compress.harmony.pack200.Codec;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.unpack200.bytecode.ClassFileEntry;
import org.apache.commons.lang3.StringUtils;

/**
 * Defines a layout that describes how an attribute will be transmitted.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class AttributeLayout implements IMatcher {

    /**
     * {@value}
     */
    public static final String ACC_ABSTRACT = "ACC_ABSTRACT"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_ANNOTATION = "ACC_ANNOTATION"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_ENUM = "ACC_ENUM"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_FINAL = "ACC_FINAL"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_INTERFACE = "ACC_INTERFACE"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_NATIVE = "ACC_NATIVE"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_PRIVATE = "ACC_PRIVATE"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_PROTECTED = "ACC_PROTECTED"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_PUBLIC = "ACC_PUBLIC"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_STATIC = "ACC_STATIC"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_STRICT = "ACC_STRICT"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_SYNCHRONIZED = "ACC_SYNCHRONIZED"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_SYNTHETIC = "ACC_SYNTHETIC"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_TRANSIENT = "ACC_TRANSIENT"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ACC_VOLATILE = "ACC_VOLATILE"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_ANNOTATION_DEFAULT = "AnnotationDefault"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_CLASS_FILE_VERSION = "class-file version"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_CODE = "Code"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_CONSTANT_VALUE = "ConstantValue"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_DEPRECATED = "Deprecated"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_ENCLOSING_METHOD = "EnclosingMethod"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_EXCEPTIONS = "Exceptions"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_INNER_CLASSES = "InnerClasses"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_LINE_NUMBER_TABLE = "LineNumberTable"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_LOCAL_VARIABLE_TABLE = "LocalVariableTable"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_SIGNATURE = "Signature"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final String ATTRIBUTE_SOURCE_FILE = "SourceFile"; //$NON-NLS-1$

    /**
     * {@value}
     */
    public static final int CONTEXT_CLASS = 0;

    /**
     * {@value}
     */
    public static final int CONTEXT_CODE = 3;

    /**
     * {@value}
     */
    public static final int CONTEXT_FIELD = 1;

    /**
     * {@value}
     */
    public static final int CONTEXT_METHOD = 2;

    /**
     * Context names.
     */
    public static final String[] contextNames = { "Class", "Field", "Method", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "Code", }; //$NON-NLS-1$

    private static ClassFileEntry getValue(final String layout, long longIndex, final SegmentConstantPool pool) throws Pack200Exception {
        if (layout.startsWith("R")) { //$NON-NLS-1$
            // references
            if (layout.indexOf('N') != -1) {
                longIndex--;
            }
            if (layout.startsWith("RU")) { //$NON-NLS-1$
                return pool.getValue(SegmentConstantPool.UTF_8, longIndex);
            }
            if (layout.startsWith("RS")) { //$NON-NLS-1$
                return pool.getValue(SegmentConstantPool.SIGNATURE, longIndex);
            }
        } else if (layout.startsWith("K")) { //$NON-NLS-1$
            final char type = layout.charAt(1);
            switch (type) {
            case 'S': // String
                return pool.getValue(SegmentConstantPool.CP_STRING, longIndex);
            case 'I': // Int (or byte or short)
            case 'C': // Char
                return pool.getValue(SegmentConstantPool.CP_INT, longIndex);
            case 'F': // Float
                return pool.getValue(SegmentConstantPool.CP_FLOAT, longIndex);
            case 'J': // Long
                return pool.getValue(SegmentConstantPool.CP_LONG, longIndex);
            case 'D': // Double
                return pool.getValue(SegmentConstantPool.CP_DOUBLE, longIndex);
            }
        }
        throw new Pack200Exception("Unknown layout encoding: '%s'", layout);
    }

    private final int context;

    private final int index;

    private final String layout;

    private long mask;

    private final String name;
    private final boolean isDefault;
    private int backwardsCallCount;

    /**
     * Constructs a default AttributeLayout (equivalent to {@code new AttributeLayout(name, context, layout, index, true);})
     *
     * @param name    The layout name.
     * @param context One of {@link #CONTEXT_CLASS}, {@link #CONTEXT_CODE}, {@link #CONTEXT_FIELD}, {@link #CONTEXT_METHOD}.
     * @param layout  The layout.
     * @param index   The index, currently used as part of computing the hash code.
     * @throws Pack200Exception Attribute context out of range.
     * @throws Pack200Exception Cannot have a null layout.
     * @throws Pack200Exception Cannot have an unnamed layout.
     */
    public AttributeLayout(final String name, final int context, final String layout, final int index) throws Pack200Exception {
        this(name, context, layout, index, true);
    }

    /**
     * Constructs a default AttributeLayout (equivalent to {@code new AttributeLayout(name, context, layout, index, true);})
     *
     * @param name    The layout name.
     * @param context One of {@link #CONTEXT_CLASS}, {@link #CONTEXT_CODE}, {@link #CONTEXT_FIELD}, {@link #CONTEXT_METHOD}.
     * @param layout  The layout.
     * @param index   The index, currently used as part of computing the hash code.
     * @param isDefault Whether this is the default layout.
     * @throws Pack200Exception Attribute context out of range.
     * @throws Pack200Exception Cannot have a null layout.
     * @throws Pack200Exception Cannot have an unnamed layout.
     */
    public AttributeLayout(final String name, final int context, final String layout, final int index, final boolean isDefault) throws Pack200Exception {
        Pack200Exception.requireNonNull(layout, "layout");
        this.index = index;
        this.context = context;
        if (index >= 0) {
            this.mask = 1L << index;
        } else {
            this.mask = 0;
        }
        if (context != CONTEXT_CLASS && context != CONTEXT_CODE && context != CONTEXT_FIELD && context != CONTEXT_METHOD) {
            throw new Pack200Exception("Attribute context out of range: %d", context);
        }
        if (StringUtils.isEmpty(name)) {
            throw new Pack200Exception("Cannot have an unnamed layout");
        }
        this.name = name;
        this.layout = layout;
        this.isDefault = isDefault;
    }

    /**
     * Gets the Codec based on the layout.
     *
     * @return the Codec.
     */
    public Codec getCodec() {
        if (layout.indexOf('O') >= 0) {
            return Codec.BRANCH5;
        }
        if (layout.indexOf('P') >= 0) {
            return Codec.BCI5;
        }
        if (layout.indexOf('S') >= 0 && !layout.contains("KS") //$NON-NLS-1$
                && !layout.contains("RS")) { //$NON-NLS-1$
            return Codec.SIGNED5;
        }
        if (layout.indexOf('B') >= 0) {
            return Codec.BYTE1;
        }
        return Codec.UNSIGNED5;
    }

    /**
     * Gets the context.
     *
     * @return the context.
     */
    public int getContext() {
        return context;
    }

    /**
     * Gets the index.
     *
     * @return the index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the layout.
     *
     * @return the layout.
     */
    public String getLayout() {
        return layout;
    }

    /**
     * Gets the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ClassFileEntry for the given input.
     *
     * @param longIndex An index into the segment constant pool.
     * @param pool the segment constant pool.
     * @return the matching ClassFileEntry.
     * @throws Pack200Exception if the input is invalid.
     */
    public ClassFileEntry getValue(final long longIndex, final SegmentConstantPool pool) throws Pack200Exception {
        return getValue(layout, longIndex, pool);
    }

    /**
     * Gets the ClassFileEntry for the given input.
     *
     * @param longIndex An index into the segment constant pool.
     * @param type the Java type signature.
     * @param pool the segment constant pool.
     * @return the matching ClassFileEntry.
     * @throws Pack200Exception if the input is invalid.
     */
    public ClassFileEntry getValue(final long longIndex, final String type, final SegmentConstantPool pool) throws Pack200Exception {
        // TODO This really needs to be better tested, esp. the different types
        // TODO This should have the ability to deal with RUN stuff too, and
        // unions
        if (!layout.startsWith("KQ")) {
            return getValue(layout, longIndex, pool);
        }
        if (type.equals("Ljava/lang/String;")) { //$NON-NLS-1$
            return getValue("KS", longIndex, pool);
        }
        return getValue("K" + type + layout.substring(2), longIndex, //$NON-NLS-1$
                pool);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int r = 1;
        if (name != null) {
            r = r * prime + name.hashCode();
        }
        if (layout != null) {
            r = r * prime + layout.hashCode();
        }
        r = r * prime + index;
        r = r * prime + context;
        return r;
    }

    /**
     * Tests whether this is the default layout.
     *
     * @return whether this is the default layout.
     */
    public boolean isDefaultLayout() {
        return isDefault;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.harmony.unpack200.IMatches#matches(long)
     */
    @Override
    public boolean matches(final long value) {
        return (value & mask) != 0;
    }

    /**
     * Gets the backward call count.
     *
     * @return the backward call count.
     */
    public int numBackwardsCallables() {
        if ("*".equals(layout)) {
            return 1;
        }
        return backwardsCallCount;
    }

    /**
     * Sets the backward call count.
     *
     * @param backwardsCallCount the backward call count.
     */
    public void setBackwardsCallCount(final int backwardsCallCount) {
        this.backwardsCallCount = backwardsCallCount;
    }

    @Override
    public String toString() {
        return contextNames[context] + ": " + name;
    }

}
