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

package org.apache.commons.compress.archivers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestArchiveStreamProvider implements ArchiveStreamProvider {

    public static final class ArchiveInvocationConfirmationException extends ArchiveException {

        private static final long serialVersionUID = 1L;

        public ArchiveInvocationConfirmationException(final String message) {
            super(message);
        }
    }

    @Override
    public <T extends ArchiveInputStream<? extends ArchiveEntry>> T createArchiveInputStream(final String name, final InputStream in,
            final String encoding) throws ArchiveException {
        throw new ArchiveInvocationConfirmationException(name);
    }

    @Override
    public <T extends ArchiveOutputStream<? extends ArchiveEntry>> T createArchiveOutputStream(final String name, final OutputStream out,
            final String encoding) throws ArchiveException {
        throw new ArchiveInvocationConfirmationException(name);
    }

    @Override
    public Set<String> getInputStreamArchiveNames() {
        final HashSet<String> set = new HashSet<>();
        Collections.addAll(set, "ArchiveTestInput1");
        return set;
    }

    @Override
    public Set<String> getOutputStreamArchiveNames() {
        final HashSet<String> set = new HashSet<>();
        Collections.addAll(set, "ArchiveTestOutput1");
        return set;
    }

}
