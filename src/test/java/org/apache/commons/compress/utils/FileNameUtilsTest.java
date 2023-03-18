/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.compress.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

public class FileNameUtilsTest {

    @Test
    public void getBaseNamePathBaseCases() {
        assertEquals("bar", FileNameUtils.getBaseName(Paths.get("a/b/c/bar.foo")));
        assertEquals("foo", FileNameUtils.getBaseName(Paths.get("foo")));
        assertEquals("", FileNameUtils.getBaseName(Paths.get("")));
        assertEquals("", FileNameUtils.getBaseName(Paths.get(".")));
        for (final File f : File.listRoots()) {
            assertNull(FileNameUtils.getBaseName(f.toPath()));
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            assertNull(FileNameUtils.getBaseName(Paths.get("C:\\")));
        }
    }

    @Test
    public void getBaseNamePathCornerCases() {
        assertNull(FileNameUtils.getBaseName((Path) null));
        assertEquals("foo", FileNameUtils.getBaseName(Paths.get("foo.")));
        assertEquals("", FileNameUtils.getBaseName(Paths.get("bar/.foo")));
    }

    @Test
    public void getBaseNameStringBaseCases() {
        assertEquals("bar", FileNameUtils.getBaseName("a/b/c/bar.foo"));
        assertEquals("foo", FileNameUtils.getBaseName("foo"));
    }

    @Test
    public void getBaseNameStringCornerCases() {
        assertNull(FileNameUtils.getBaseName((String) null));
        assertEquals("foo", FileNameUtils.getBaseName("foo."));
        assertEquals("", FileNameUtils.getBaseName("bar/.foo"));
    }

    @Test
    public void getExtensionPathBaseCases() {
        assertEquals("foo", FileNameUtils.getExtension(Paths.get("a/b/c/bar.foo")));
        assertEquals("", FileNameUtils.getExtension(Paths.get("foo")));
        assertEquals("", FileNameUtils.getExtension(Paths.get("")));
        assertEquals("", FileNameUtils.getExtension(Paths.get(".")));
        for (final File f : File.listRoots()) {
            assertNull(FileNameUtils.getExtension(f.toPath()));
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            assertNull(FileNameUtils.getExtension(Paths.get("C:\\")));
        }
    }

    @Test
    public void getExtensionPathCornerCases() {
        assertNull(FileNameUtils.getExtension((String) null));
        assertEquals("", FileNameUtils.getExtension(Paths.get("foo.")));
        assertEquals("foo", FileNameUtils.getExtension(Paths.get("bar/.foo")));
    }

    @Test
    public void getExtensionStringBaseCases() {
        assertEquals("foo", FileNameUtils.getExtension("a/b/c/bar.foo"));
        assertEquals("", FileNameUtils.getExtension("foo"));
    }

    @Test
    public void getExtensionStringCornerCases() {
        assertNull(FileNameUtils.getExtension((String) null));
        assertEquals("", FileNameUtils.getExtension("foo."));
        assertEquals("foo", FileNameUtils.getExtension("bar/.foo"));
    }
}
