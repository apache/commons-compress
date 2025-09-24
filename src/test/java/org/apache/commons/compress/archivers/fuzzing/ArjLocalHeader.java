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
package org.apache.commons.compress.archivers.fuzzing;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ArjLocalHeader extends AbstractArjHeader {

    public ArjLocalHeader(Charset charset, String fileName, String comment) {
        super(charset, (byte) 0 /* stored */, (byte) 1 /* text */, fileName, comment);
    }

    @Override
    protected void writeBasicHeader(ByteBuffer output) throws IOException {
        super.writeBasicHeader(output);
        output.putInt(0); // modification time
        output.putInt(0); // compressed file size
        output.putInt(0); // uncompressed file size
        output.putInt(0); // file CRC32
        output.putShort((short) 0); // zero
        output.putShort((short) 0); // file access mode
        output.put((byte) 0); // zero
        output.put((byte) 0); // zero
    }

    @Override
    protected int extraLength() {
        return 0;
    }
}
