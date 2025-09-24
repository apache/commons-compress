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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class CpioOldAsciiHeader extends AbstractCpioHeader {

    public CpioOldAsciiHeader(Charset charset, String fileName, int fileSize) {
        super(charset, fileName, fileSize);
    }

    public CpioOldAsciiHeader(Charset charset, String fileName, int fileNameSize, int fileSize) {
        super(charset, fileName, fileNameSize, fileSize);
    }

    public void writeTo(PrintWriter output) throws IOException {
        output.printf(
                "070707%06o%06o%06o%06o%06o%06o%06o%011o%06o%011o",
                (int) dev & 0xffff,
                (int) ino & 0xffff,
                (int) mode & 0xffff,
                (int) uid & 0xffff,
                (int) gid & 0xffff,
                (int) nlink & 0xffff,
                (int) rdev & 0xffff,
                mtime,
                getNameSize(), // **6** octal digits
                fileSize // **11** octal digits
                );

        // Name + NUL, then even-byte padding
        writeNameWithNull(output);
        // header length for odc is 76 bytes before the name
        final long afterHeaderAndName = 76 + getNameSize();
        pad(output, pad2(afterHeaderAndName));
    }
}
