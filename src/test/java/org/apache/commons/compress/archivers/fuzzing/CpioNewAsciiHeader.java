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

public class CpioNewAsciiHeader extends AbstractCpioHeader {

    private final int devMajor = 0; // Major device number
    private final int devMinor = 0; // Minor device number
    private final int rdevMajor = 0; // Major device number (if special file)
    private final int rdevMinor = 0; // Minor device number (if special file)
    private final int check = 0; // Checksum (empty file has zero checksum)

    public CpioNewAsciiHeader(Charset charset, String fileName, int fileSize) {
        super(charset, fileName, fileSize);
    }

    public CpioNewAsciiHeader(Charset charset, String fileName, long fileNameSize, int fileSize) {
        super(charset, fileName, fileNameSize, fileSize);
    }

    public void writeTo(PrintWriter output, boolean includeCrc) throws IOException {
        output.append(includeCrc ? "070702" : "070701");
        output.printf(
                "%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x%08x",
                (int) ino & 0xffff,
                (int) mode & 0xffff,
                (int) uid & 0xffff,
                (int) gid & 0xffff,
                (int) nlink & 0xffff,
                mtime,
                fileSize,
                devMajor,
                devMinor,
                rdevMajor,
                rdevMinor,
                getNameSize(),
                check);

        // Name + NUL
        writeNameWithNull(output);

        // Pad so the next thing (data) starts on a 4-byte boundary.
        // newc header is 110 bytes long.
        final long afterHeaderAndName = 110 + getNameSize();
        pad(output, pad4(afterHeaderAndName));
    }
}
