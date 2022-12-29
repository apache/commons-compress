/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.zip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

public class ZipClassCoverageTest {

    @Test
    public void testConstantConstructor()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Class<ZipConstants> clazz = ZipConstants.class;
        final Constructor<ZipConstants> constructor = clazz.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        final Object o = constructor.newInstance();
        assertThat(o, IsInstanceOf.instanceOf(clazz));
        constructor.setAccessible(false);

    }
    @Test
    public void testConstructZip64RequiredException() {
        final Zip64RequiredException e = new Zip64RequiredException("critique of pure");
        assertNotNull(e);
    }

    @Test
    public void testMessageException() {
        final ZipArchiveEntry ze = new ZipArchiveEntry("hello");
        final String entryTooBigMessage = Zip64RequiredException.getEntryTooBigMessage(ze);
        assertEquals("hello's size exceeds the limit of 4GByte.",
            entryTooBigMessage);
    }

}
