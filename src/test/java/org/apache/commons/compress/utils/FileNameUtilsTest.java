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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FileNameUtilsTest {

    @Test
    public void getExtensionBaseCases() {
        assertEquals("foo", FileNameUtils.getExtension("a/b/c/bar.foo"));
        assertEquals("", FileNameUtils.getExtension("foo"));
    }

    @Test
    public void getExtensionCornerCases() {
        assertNull(FileNameUtils.getExtension(null));
        assertEquals("", FileNameUtils.getExtension("foo."));
        assertEquals("foo", FileNameUtils.getExtension("bar/.foo"));
    }

    @Test
    public void getBaseNameBaseCases() {
        assertEquals("bar", FileNameUtils.getBaseName("a/b/c/bar.foo"));
        assertEquals("foo", FileNameUtils.getBaseName("foo"));
    }

    @Test
    public void getBaseNameCornerCases() {
        assertNull(FileNameUtils.getBaseName(null));
        assertEquals("foo", FileNameUtils.getBaseName("foo."));
        assertEquals("", FileNameUtils.getBaseName("bar/.foo"));
    }
}
