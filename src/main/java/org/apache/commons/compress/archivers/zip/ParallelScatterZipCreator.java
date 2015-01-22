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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

import static java.util.Collections.synchronizedList;
import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;

/**
 * Creates a zip in parallel by using multiple threadlocal #ScatterZipOutputStream instances.
 * <p>
 * Note that this class generally makes no guarantees about the order of things written to
 * the output file. Things that need to come in a specific order (manifests, directories)
 * must be handled by the client of this class, usually by writing these things to the
 * #ZipArchiveOutputStream *before* calling #writeTo on this class.</p>
 * <p>
 * The client can supply an ExecutorService, but for reasons of memory model consistency,
 * this will be shut down by this class prior to completion.
 * </p>
 */
public class ParallelScatterZipCreator {
    private final List<ScatterZipOutputStream> streams = synchronizedList(new ArrayList<ScatterZipOutputStream>());
    private final ExecutorService es;
    private final ScatterGatherBackingStoreSupplier backingStoreSupplier;
    private final List<Future> futures = new ArrayList<Future>();

    private final long startedAt = System.currentTimeMillis();
    private long compressionDoneAt = 0;
    private long scatterDoneAt;

    private static class DefaultBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {
        final AtomicInteger storeNum = new AtomicInteger(0);

        public ScatterGatherBackingStore get() throws IOException {
            File tempFile = File.createTempFile("parallelscatter", "n" + storeNum.incrementAndGet());
            return new FileBasedScatterGatherBackingStore(tempFile);
        }
    }

    private ScatterZipOutputStream createDeferred(ScatterGatherBackingStoreSupplier scatterGatherBackingStoreSupplier)
            throws IOException {
        ScatterGatherBackingStore bs = scatterGatherBackingStoreSupplier.get();
        StreamCompressor sc = StreamCompressor.create(Deflater.DEFAULT_COMPRESSION, bs);
        return new ScatterZipOutputStream(bs, sc);
    }

    private final ThreadLocal<ScatterZipOutputStream> tlScatterStreams = new ThreadLocal<ScatterZipOutputStream>() {
        @Override
        protected ScatterZipOutputStream initialValue() {
            try {
                ScatterZipOutputStream scatterStream = createDeferred(backingStoreSupplier);
                streams.add(scatterStream);
                return scatterStream;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Create a ParallelScatterZipCreator with default threads
     */
    public ParallelScatterZipCreator() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Create a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use for parallel scheduling. For technical reasons,
     *                        this will be shut down by this class.
     */
    public ParallelScatterZipCreator(ExecutorService executorService) {
        this(executorService, new DefaultBackingStoreSupplier());
    }

    /**
     * Create a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use. For technical reasons, this will be shut down
     *                        by this class.
     * @param backingStoreSupplier The supplier of backing store which shall be used
     */
    public ParallelScatterZipCreator(ExecutorService executorService,
                                     ScatterGatherBackingStoreSupplier backingStoreSupplier) {
        this.backingStoreSupplier = backingStoreSupplier;
        es = executorService;
    }

    /**
     * Adds an archive entry to this archive.
     * <p>
     * This method is expected to be called from a single client thread
     * </p>
     *
     * @param zipArchiveEntry The entry to add.
     * @param source          The source input stream supplier
     */

    public void addArchiveEntry(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier source) {
        submit(createCallable(zipArchiveEntry, source));
    }

    /**
     * Submit a callable for compression
     * @param callable The callable to run
     */
    public final void submit(Callable<Object> callable) {
        futures.add(es.submit(callable));
    }

    /**
     * Create a callable that will compress the given archive entry.
     *
     * <p>This method is expected to be called from a single client thread.</p>
     * <p>
     * This method is used by clients that want finer grained control over how the callable is
     * created, possibly wanting to wrap this callable in a different callable</p>
     *
     * @param zipArchiveEntry The entry to add.
     * @param source    The source input stream supplier
     * @return   A callable that will be used to check for errors
     */

    public final Callable<Object> createCallable(ZipArchiveEntry zipArchiveEntry, InputStreamSupplier source) {
        final int method = zipArchiveEntry.getMethod();
        if (method == ZipMethod.UNKNOWN_CODE) {
            throw new IllegalArgumentException("Method must be set on the supplied zipArchiveEntry");
        }
        final ZipArchiveEntryRequest zipArchiveEntryRequest = createZipArchiveEntryRequest(zipArchiveEntry, source);
        return new Callable<Object>() {
            public Object call() throws Exception {
                tlScatterStreams.get().addArchiveEntry(zipArchiveEntryRequest);
                return null;
            }
        };
    }


    /**
     * Write the contents this to the target #ZipArchiveOutputStream.
     * <p>
     * It may be beneficial to write things like directories and manifest files to the targetStream
     * before calling this method.
     * </p>
     *
     * @param targetStream The ZipArchiveOutputStream to receive the contents of the scatter streams
     * @throws IOException          If writing fails
     * @throws InterruptedException If we get interrupted
     * @throws ExecutionException   If something happens in the parallel execution
     */
    public void writeTo(ZipArchiveOutputStream targetStream)
            throws IOException, InterruptedException, ExecutionException {

        // Make sure we catch any exceptions from parallel phase
        for (Future future : futures) {
            future.get();
        }

        es.shutdown();
        es.awaitTermination(1000 * 60, TimeUnit.SECONDS);  // == Infinity. We really *must* wait for this to complete

        // It is important that all threads terminate before we go on, ensure happens-before relationship
        compressionDoneAt = System.currentTimeMillis();

        for (ScatterZipOutputStream scatterStream : streams) {
            scatterStream.writeTo(targetStream);
            scatterStream.close();
        }

        scatterDoneAt = System.currentTimeMillis();
    }

    /**
     * Returns a message describing the overall statistics of the compression run
     *
     * @return A string
     */
    public String getStatisticsMessage() {
        return "Compression: " + (compressionDoneAt - startedAt) + "ms," +
                "Merging files: " + (scatterDoneAt - compressionDoneAt) + "ms";
    }
}

