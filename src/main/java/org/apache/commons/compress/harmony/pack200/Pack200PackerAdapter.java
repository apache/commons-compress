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
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.apache.commons.compress.java.util.jar.Pack200.Packer;

/**
 * This class provides the binding between the standard Pack200 interface and the internal interface for (un)packing. As
 * this uses generics for the SortedMap, this class must be compiled and run on a Java 1.5 system. However, Java 1.5 is
 * not necessary to use the internal libraries for unpacking.
 */
public class Pack200PackerAdapter extends Pack200Adapter implements Packer {

    private final PackingOptions options = new PackingOptions();

    @Override
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
        if (newValue != null && !newValue.equals(oldValue)) {
            if (propertyName.startsWith(CLASS_ATTRIBUTE_PFX)) {
                final String attributeName = propertyName.substring(CLASS_ATTRIBUTE_PFX.length());
                options.addClassAttributeAction(attributeName, (String) newValue);
            } else if (propertyName.startsWith(CODE_ATTRIBUTE_PFX)) {
                final String attributeName = propertyName.substring(CODE_ATTRIBUTE_PFX.length());
                options.addCodeAttributeAction(attributeName, (String) newValue);
            } else if (propertyName.equals(DEFLATE_HINT)) {
                options.setDeflateHint((String) newValue);
            } else if (propertyName.equals(EFFORT)) {
                options.setEffort(Integer.parseInt((String) newValue));
            } else if (propertyName.startsWith(FIELD_ATTRIBUTE_PFX)) {
                final String attributeName = propertyName.substring(FIELD_ATTRIBUTE_PFX.length());
                options.addFieldAttributeAction(attributeName, (String) newValue);
            } else if (propertyName.equals(KEEP_FILE_ORDER)) {
                options.setKeepFileOrder(Boolean.parseBoolean((String) newValue));
            } else if (propertyName.startsWith(METHOD_ATTRIBUTE_PFX)) {
                final String attributeName = propertyName.substring(METHOD_ATTRIBUTE_PFX.length());
                options.addMethodAttributeAction(attributeName, (String) newValue);
            } else if (propertyName.equals(MODIFICATION_TIME)) {
                options.setModificationTime((String) newValue);
            } else if (propertyName.startsWith(PASS_FILE_PFX)) {
                if (oldValue != null && !oldValue.equals("")) {
                    options.removePassFile((String) oldValue);
                }
                options.addPassFile((String) newValue);
            } else if (propertyName.equals(SEGMENT_LIMIT)) {
                options.setSegmentLimit(Long.parseLong((String) newValue));
            } else if (propertyName.equals(UNKNOWN_ATTRIBUTE)) {
                options.setUnknownAttributeAction((String) newValue);
            }
        }
    }

    @Override
    public void pack(final JarFile file, final OutputStream out) throws IOException {
        if (file == null || out == null) {
            throw new IllegalArgumentException("Must specify both input and output streams");
        }
        completed(0);
        try {
            new org.apache.commons.compress.harmony.pack200.Archive(file, out, options).pack();
        } catch (final Pack200Exception e) {
            throw new IOException("Failed to pack Jar:" + e);
        }
        completed(1);
    }

    @Override
    public void pack(final JarInputStream in, final OutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException("Must specify both input and output streams");
        }
        completed(0);
        final PackingOptions options = new PackingOptions();

        try {
            new org.apache.commons.compress.harmony.pack200.Archive(in, out, options).pack();
        } catch (final Pack200Exception e) {
            throw new IOException("Failed to pack Jar:" + e);
        }
        completed(1);
        in.close();
    }

}
