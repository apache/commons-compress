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

package org.apache.commons.compress.archivers.sevenz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.jupiter.api.Test;

/**
 * Tests for class {@link Folder}.
 *
 * @see Folder
 */
class SevenZFolderTest {

    @Test
    void testFindBindPairForInStream() {
        final Folder folder = new Folder();
        final BindPair[] bindPairArray = new BindPair[1];
        final BindPair bindPair = new BindPair(0, 0);
        bindPairArray[0] = bindPair;
        folder.bindPairs = bindPairArray;
        assertEquals(0, folder.findBindPairForInStream(0));
    }

    @Test
    void testGetUnpackSizeForCoderOne() {
        final Folder folder = new Folder();
        final Coder[] coderArray = new Coder[5];
        final Coder coder = new Coder(null, 0, 0, null);
        folder.coders = coderArray;
        assertEquals(0L, folder.getUnpackSizeForCoder(coder));
    }

    @Test
    void testGetUnpackSizeOne() throws ArchiveException {
        final Folder folder = new Folder();
        folder.totalOutputStreams = 266;
        final BindPair[] bindPairArray = new BindPair[1];
        final BindPair bindPair = new BindPair(0, 0);
        bindPairArray[0] = bindPair;
        folder.bindPairs = bindPairArray;
        folder.totalOutputStreams = 1;
        assertEquals(0L, folder.getUnpackSize());
    }

    @Test
    void testGetUnpackSizeTwo() throws ArchiveException {
        final Folder folder = new Folder();
        assertEquals(0L, folder.getUnpackSize());
    }
}
