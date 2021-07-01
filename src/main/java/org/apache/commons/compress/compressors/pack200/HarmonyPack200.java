/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress.compressors.pack200;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.commons.compress.harmony.pack200.Pack200Exception;
import org.apache.commons.compress.harmony.pack200.PackingOptions;
import org.apache.commons.compress.harmony.unpack200.Archive;

/**
 * Class factory for {@link Pack200Facade.Packer} and {@link Pack200Facade.Unpacker} based on the {@code
 * org.apache.commons.compress.harmony} package.
 */
public abstract class HarmonyPack200 {

    /**
     * Prevent this class from being instantiated.
     */
    private HarmonyPack200() {
        // do nothing
    }

    /**
     * Returns a new instance of a packer engine.
     *
     * @return an instance of {@code Pack200Facade.Packer}
     */
    public static Pack200Facade.Packer newPacker() {
        return new Pack200PackerAdapter();
    }

    /**
     * Returns a new instance of a unpacker engine.
     *
     * @return a instance of {@code Pack200Facade.Unpacker}.
     */
    public static Pack200Facade.Unpacker newUnpacker() {
        return new Pack200UnpackerAdapter();
    }

    private static abstract class Pack200Adapter {

	protected static final int DEFAULT_BUFFER_SIZE = 8192;

	private final SortedMap<String, String> properties = new TreeMap<String, String>();

	public SortedMap<String, String> properties() {
            return properties;
	}

    }

    static class Pack200UnpackerAdapter extends Pack200Adapter implements Pack200Facade.Unpacker {
        @Override
	public void unpack(InputStream in, JarOutputStream out) throws IOException {
            if (in == null || out == null)
                throw new IllegalArgumentException("Must specify both input and output streams");
            try {
                new Archive(in, out).unpack();
            } catch (Pack200Exception e) {
                throw new IOException("Failed to unpack Jar:" + String.valueOf(e));
            }
            in.close();
	}

        @Override
	public void unpack(File file, JarOutputStream out) throws IOException {
            if (file == null || out == null)
                throw new IllegalArgumentException("Must specify both input and output streams");
            int size = (int) file.length();
            int bufferSize = (size > 0 && size < DEFAULT_BUFFER_SIZE ? size
                              : DEFAULT_BUFFER_SIZE);
            InputStream in = new BufferedInputStream(new FileInputStream(file), bufferSize);
            unpack(in, out);
	}
    }

    static class Pack200PackerAdapter extends Pack200Adapter implements Pack200Facade.Packer {

        private final PackingOptions options = new PackingOptions();

        @Override
        public void pack(JarFile file, OutputStream out) throws IOException {
            if (file == null || out == null)
                throw new IllegalArgumentException("Must specify both input and output streams");
            try {
                new org.apache.commons.compress.harmony.pack200.Archive(file, out, options).pack();
            } catch (Pack200Exception e) {
                throw new IOException("Failed to pack Jar:" + String.valueOf(e));
            }
        }

        @Override
        public void pack(JarInputStream in, OutputStream out) throws IOException {
            if (in == null || out == null)
                throw new IllegalArgumentException("Must specify both input and output streams");
            PackingOptions options = new PackingOptions();

            try {
                new org.apache.commons.compress.harmony.pack200.Archive(in, out, options).pack();
            } catch (Pack200Exception e) {
                throw new IOException("Failed to pack Jar:" + String.valueOf(e));
            }
            in.close();
        }

    }
}
