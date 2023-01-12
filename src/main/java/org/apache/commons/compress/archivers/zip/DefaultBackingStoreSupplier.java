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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;

/**
 * Implements {@link ScatterGatherBackingStoreSupplier} using a temporary folder.
 * <p>
 * For example:
 * </p>
 * <pre>
 * final Path dir = Paths.get("target/custom-temp-dir");
 * Files.createDirectories(dir);
 * final ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator(
 *     Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
 *     new DefaultBackingStoreSupplier(dir));
 * </pre>
 *
 * @since 1.23
 */
public class DefaultBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {

    private static final String PREFIX = "parallelscatter";

    /**
     * Constructs a new instance. If {@code dir} is null, then use the default temporary-file directory.
     *
     * @param dir temporary folder, may be null, must exist if non-null.
     */
    public DefaultBackingStoreSupplier(final Path dir) {
        this.dir = dir;
    }

    private final AtomicInteger storeNum = new AtomicInteger();
    private final Path dir;

    @Override
    public ScatterGatherBackingStore get() throws IOException {
        final String suffix = "n" + storeNum.incrementAndGet();
        final Path tempFile = dir == null ? Files.createTempFile(PREFIX, suffix) : Files.createTempFile(dir, PREFIX, suffix);
        return new FileBasedScatterGatherBackingStore(tempFile);
    }
}