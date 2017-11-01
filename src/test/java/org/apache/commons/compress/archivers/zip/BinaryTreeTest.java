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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class BinaryTreeTest {

    @Test
    public void testDecode() throws IOException {
        final InputStream in = new ByteArrayInputStream(new byte[] { 0x02, 0x42, 0x01, 0x13 });

        final BinaryTree tree = BinaryTree.decode(in, 8);

        assertNotNull(tree);

        final BitStream stream = new BitStream(new ByteArrayInputStream(new byte[] { (byte) 0x8D, (byte) 0xC5, (byte) 0x11, 0x00 }));
        assertEquals(0, tree.read(stream));
        assertEquals(1, tree.read(stream));
        assertEquals(2, tree.read(stream));
        assertEquals(3, tree.read(stream));
        assertEquals(4, tree.read(stream));
        assertEquals(5, tree.read(stream));
        assertEquals(6, tree.read(stream));
        assertEquals(7, tree.read(stream));
    }
    @Test
    public void testExceptions()  {
         BinaryTree binaryFinary = new BinaryTree(4);
         binaryFinary.addLeaf(0,0,0,1);
        try {
            binaryFinary.addLeaf(0,0,0,1);
            fail("should have thrown illegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        InputStream is = new ByteArrayInputStream(new byte[]{});
        try {
            BinaryTree.decode(is,0);
            fail("should have thrown IOException");
        } catch (IOException e) {

        }
        binaryFinary = new BinaryTree(4);
        try {
            binaryFinary.read(new BitStream(new ByteArrayInputStream(new byte[] {0})));
            fail("expected read fail");
        } catch (IOException e) {
        }
    }
}
