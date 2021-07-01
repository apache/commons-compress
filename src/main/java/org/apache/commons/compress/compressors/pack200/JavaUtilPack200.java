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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedMap;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

/**
 * Class factory for {@link Pack200Facade.Packer} and {@link Pack200Facade.Unpacker} based on the {@code
 * org.apache.commons.compress.harmony} package.
 */
public abstract class JavaUtilPack200 {

    /**
     * Prevent this class from being instantiated.
     */
    private JavaUtilPack200() {
        // do nothing
    }

    /**
     * Returns a new instance of a packer engine.
     *
     * @return an instance of {@code Pack200Facade.Packer}
     */
    public static Pack200Facade.Packer newPacker() {
        return new Pack200PackerAdapter(Pack200.newPacker());
    }

    /**
     * Returns a new instance of a unpacker engine.
     *
     * @return a instance of {@code Pack200Facade.Unpacker}.
     */
    public static Pack200Facade.Unpacker newUnpacker() {
        return new Pack200UnpackerAdapter(Pack200.newUnpacker());
    }

    private static class Pack200UnpackerAdapter implements Pack200Facade.Unpacker {
        private final Pack200.Unpacker unpacker;

        private Pack200UnpackerAdapter(final Pack200.Unpacker unpacker) {
            this.unpacker = unpacker;
        }

        @Override
	public void unpack(InputStream in, JarOutputStream out) throws IOException {
            unpacker.unpack(in, out);
	}

        @Override
	public void unpack(File file, JarOutputStream out) throws IOException {
            unpacker.unpack(file, out);
	}

        @Override
        public SortedMap<String, String> properties() {
            return unpacker.properties();
        }
    }

    private static class Pack200PackerAdapter implements Pack200Facade.Packer {
        private final Pack200.Packer packer;

        private Pack200PackerAdapter(final Pack200.Packer packer) {
            this.packer = packer;
        }

        @Override
        public void pack(JarFile file, OutputStream out) throws IOException {
            packer.pack(file, out);
        }

        @Override
        public void pack(JarInputStream in, OutputStream out) throws IOException {
            packer.pack(in, out);
        }

        @Override
        public SortedMap<String, String> properties() {
            return packer.properties();
        }
    }
}
