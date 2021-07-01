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

package org.apache.commons.compress.compressors.pack200;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Pack200FacadeTest {

    @Test
    public void loadsTheExpectedPacker() {
        try {
            Class.forName("java.util.jar.Pack200");
            assertFalse(Pack200Facade.newPacker() instanceof HarmonyPack200.Pack200PackerAdapter);
        } catch (ClassNotFoundException ex) {
            assertTrue(Pack200Facade.newPacker() instanceof HarmonyPack200.Pack200PackerAdapter);
        }
    }

    @Test
    public void loadsTheExpectedUnpacker() {
        try {
            Class.forName("java.util.jar.Pack200");
            assertFalse(Pack200Facade.newUnpacker() instanceof HarmonyPack200.Pack200UnpackerAdapter);
        } catch (ClassNotFoundException ex) {
            assertTrue(Pack200Facade.newUnpacker() instanceof HarmonyPack200.Pack200UnpackerAdapter);
        }
    }


}
