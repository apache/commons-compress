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
package org.apache.commons.compress.archivers.jar;

import static org.junit.Assert.fail;

import java.util.zip.ZipException;
import org.apache.commons.compress.archivers.zip.JarMarker;
import org.junit.Test;

public class JarMarkerTest {

    @Test public void testJarMarkerLengthCheck() {
        JarMarker jarMarker = JarMarker.getInstance();
        try {
            jarMarker.parseFromLocalFileData(null,0,1);
            fail("should have thrown exception due to length of 1");
        } catch (ZipException e) {

        }
    }

}
