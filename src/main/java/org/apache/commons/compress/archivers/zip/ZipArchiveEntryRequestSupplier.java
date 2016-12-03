/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.parallel.InputStreamSupplier;

import java.io.InputStream;

/**
 * Supplies {@link ZipArchiveEntryRequest}.
 *
 * Implementations are required to support thread-handover. While an instance will
 * not be accessed concurrently by multiple threads, it will be called by
 * a different thread than it was created on.
 *
 * @since 1.13
 */
public interface ZipArchiveEntryRequestSupplier {

    /**
     * Supply a {@link ZipArchiveEntryRequest} to be added to a parallel archive.
     * @return The {@link ZipArchiveEntryRequest} instance. Should never be null.
     */
    ZipArchiveEntryRequest get();
}
