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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.pack200.Pack200Adapter;
import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.java.util.jar.Pack200.Unpacker;

/**
 * This class provides the binding between the standard Pack200 interface and the internal interface for (un)packing. As
 * this uses generics for the SortedMap, this class must be compiled and run on a Java 1.5 system. However, Java 1.5 is
 * not necessary to use the internal libraries for unpacking.
 */
public class Pack200UnpackerAdapter extends Pack200Adapter implements Unpacker {
    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.java.util.jar.Pack200.Unpacker#unpack(java.io.File,
     * java.util.jar.JarOutputStream)
     */
    @Override
    public void unpack(final File file, final JarOutputStream out) throws IOException {
        if (file == null || out == null) {
            throw new IllegalArgumentException("Must specify both input and output streams");
        }
        final int size = (int) file.length();
        final int bufferSize = size > 0 && size < DEFAULT_BUFFER_SIZE ? size : DEFAULT_BUFFER_SIZE;
        try (final InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), bufferSize)) {
            unpack(in, out);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.compress.java.util.jar.Pack200.Unpacker#unpack(java.io.InputStream,
     * java.util.jar.JarOutputStream)
     */
    @Override
    public void unpack(final InputStream in, final JarOutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException("Must specify both input and output streams");
        }
        completed(0);
        try {
            new Archive(in, out).unpack();
        } catch (final Pack200Exception e) {
            throw new IOException("Failed to unpack Jar:" + e);
        }
        completed(1);
    }
}
