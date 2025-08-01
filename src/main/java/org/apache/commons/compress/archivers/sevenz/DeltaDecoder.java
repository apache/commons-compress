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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.tukaani.xz.DeltaOptions;
import org.tukaani.xz.FinishableWrapperOutputStream;

final class DeltaDecoder extends AbstractCoder {

    DeltaDecoder() {
        super(Number.class);
    }

    @Override
    InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength, final Coder coder, final byte[] password,
            final int maxMemoryLimitKiB) throws IOException {
        return new DeltaOptions(getOptionsFromCoder(coder)).getInputStream(in);
    }

    @SuppressWarnings("resource")
    @Override
    OutputStream encode(final OutputStream out, final Object options) throws IOException {
        return new DeltaOptions(toInt(options, 1)).getOutputStream(new FinishableWrapperOutputStream(out));
    }

    @Override
    byte[] getOptionsAsProperties(final Object options) {
        return new byte[] { (byte) (toInt(options, 1) - 1) };
    }

    private int getOptionsFromCoder(final Coder coder) {
        if (coder == null || ArrayUtils.isEmpty(coder.properties)) {
            return 1;
        }
        return (0xff & coder.properties[0]) + 1;
    }

    @Override
    Object getOptionsFromCoder(final Coder coder, final InputStream in) {
        return getOptionsFromCoder(coder);
    }
}
