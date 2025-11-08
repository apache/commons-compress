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
package org.apache.commons.compress.archivers.jar;

import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

/**
 * JAR archive entry.
 *
 * @NotThreadSafe (parent is not thread-safe)
 */
public class JarArchiveEntry extends ZipArchiveEntry {

    /**
     * Constructs a new instance.
     *
     * @param entry See super.
     * @throws ZipException See super.
     */
    public JarArchiveEntry(final JarEntry entry) throws ZipException {
        super(entry);
    }

    /**
     * Constructs a new instance.
     *
     * @param name See super.
     */
    public JarArchiveEntry(final String name) {
        super(name);
    }

    /**
     * Constructs a new instance.
     *
     * @param entry See super.
     * @throws ZipException See super.
     */
    public JarArchiveEntry(final ZipArchiveEntry entry) throws ZipException {
        super(entry);
    }

    /**
     * Constructs a new instance.
     *
     * @param entry See super.
     * @throws ZipException See super.
     */
    public JarArchiveEntry(final ZipEntry entry) throws ZipException {
        super(entry);
    }

    /**
     * Gets a copy of the list of certificates or null if there are none.
     *
     * @return Always returns null in the current implementation
     * @deprecated Since 1.5, not currently implemented
     */
    @Deprecated
    public Certificate[] getCertificates() {
        //
        // Note, the method
        // Certificate[] java.util.jar.JarEntry.getCertificates()
        // also returns null or the list of certificates (but not copied)
        //
        // see https://issues.apache.org/jira/browse/COMPRESS-18 for discussion
        return null;
    }

    /**
     * This method is not implemented and won't ever be. The JVM equivalent has a different name {@link java.util.jar.JarEntry#getAttributes()}
     *
     * @deprecated Since 1.5, do not use; always returns null
     * @return Always returns null.
     */
    @Deprecated
    public Attributes getManifestAttributes() {
        // see https://issues.apache.org/jira/browse/COMPRESS-18 for discussion
        return null;
    }

}
