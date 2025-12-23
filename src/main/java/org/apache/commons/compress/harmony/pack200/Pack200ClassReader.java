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
package org.apache.commons.compress.harmony.pack200;

import org.objectweb.asm.ClassReader;

/**
 * Wrapper for ClassReader that enables pack200 to obtain extra class file information.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/pack-spec.html">Pack200: A Packed Class Deployment Format For Java Applications</a>
 */
public class Pack200ClassReader extends ClassReader {

    private boolean lastConstantHadWideIndex;
    private int lastUnsignedShort;
    private boolean anySyntheticAttributes;
    private String fileName;

    /**
     * Constructs a new Pack200ClassReader.
     *
     * @param b the contents of class file in the format of bytes.
     */
    public Pack200ClassReader(final byte[] b) {
        super(b);
    }

    /**
     * Gets the file name.
     *
     * @return the file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Tests whether there are synthetic attributes.
     *
     * @return true if there are synthetic attributes.
     */
    public boolean hasSyntheticAttributes() {
        return anySyntheticAttributes;
    }

    /**
     * Tests whether the last constant had a wide index.
     *
     * @return true if the last constant had a wide index.
     */
    public boolean lastConstantHadWideIndex() {
        return lastConstantHadWideIndex;
    }

    @Override
    public Object readConst(final int item, final char[] buf) {
        lastConstantHadWideIndex = item == lastUnsignedShort;
        return super.readConst(item, buf);
    }

    @Override
    public int readUnsignedShort(final int index) {
        // Doing this to check whether last load-constant instruction was ldc (18) or ldc_w (19)
        // TODO: Assess whether this impacts on performance
        final int unsignedShort = super.readUnsignedShort(index);
        if (index > 0 && b[index - 1] == 19) {
            lastUnsignedShort = unsignedShort;
        } else {
            lastUnsignedShort = Short.MIN_VALUE;
        }
        return unsignedShort;
    }

    @Override
    public String readUTF8(final int arg0, final char[] arg1) {
        final String utf8 = super.readUTF8(arg0, arg1);
        if (!anySyntheticAttributes && "Synthetic".equals(utf8)) {
            anySyntheticAttributes = true;
        }
        return utf8;
    }

    /**
     * Sets the file name.
     *
     * @param name the file name.
     */
    public void setFileName(final String name) {
        this.fileName = name;
    }

}
