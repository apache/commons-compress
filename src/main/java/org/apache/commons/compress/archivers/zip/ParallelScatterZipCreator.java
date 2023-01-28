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

import static org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest.createZipArchiveEntryRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;

/**
 * Creates a ZIP in parallel by using multiple threadlocal {@link ScatterZipOutputStream} instances.
 * <p>
 * Note that until 1.18, this class generally made no guarantees about the order of things written to the output file. Things that needed to come in a specific
 * order (manifests, directories) had to be handled by the client of this class, usually by writing these things to the {@link ZipArchiveOutputStream}
 * <em>before</em> calling {@link #writeTo writeTo} on this class.
 * </p>
 * <p>
 * The client can supply an {@link java.util.concurrent.ExecutorService}, but for reasons of memory model consistency, this will be shut down by this class
 * prior to completion.
 * </p>
 *
 * @since 1.10
 */
public class ParallelScatterZipCreator {

    private final Deque<ScatterZipOutputStream> streams = new ConcurrentLinkedDeque<>();
    private final ExecutorService executorService;
    private final ScatterGatherBackingStoreSupplier backingStoreSupplier;

    private final Deque<Future<? extends ScatterZipOutputStream>> futures = new ConcurrentLinkedDeque<>();
    private final long startedAt = System.currentTimeMillis();
    private long compressionDoneAt;
    private long scatterDoneAt;

    private final int compressionLevel;

    private final ThreadLocal<ScatterZipOutputStream> tlScatterStreams = new ThreadLocal<ScatterZipOutputStream>() {
        @Override
        protected ScatterZipOutputStream initialValue() {
            try {
                final ScatterZipOutputStream scatterStream = createDeferred(backingStoreSupplier);
                streams.add(scatterStream);
                return scatterStream;
            } catch (final IOException e) {
                throw new UncheckedIOException(e); //NOSONAR
            }
        }
    };

    /**
     * Constructs a ParallelScatterZipCreator with default threads, which is set to the number of available
     * processors, as defined by {@link java.lang.Runtime#availableProcessors}
     */
    public ParallelScatterZipCreator() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Constructs a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use for parallel scheduling. For technical reasons,
     *                        this will be shut down by this class.
     */
    public ParallelScatterZipCreator(final ExecutorService executorService) {
        this(executorService, new DefaultBackingStoreSupplier(null));
    }

    /**
     * Constructs a ParallelScatterZipCreator
     *
     * @param executorService The executorService to use. For technical reasons, this will be shut down
     *                        by this class.
     * @param backingStoreSupplier The supplier of backing store which shall be used
     */
    public ParallelScatterZipCreator(final ExecutorService executorService,
                                     final ScatterGatherBackingStoreSupplier backingStoreSupplier) {
        this(executorService, backingStoreSupplier, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Constructs a ParallelScatterZipCreator
     *
     * @param executorService      The executorService to use. For technical reasons, this will be shut down
     *                             by this class.
     * @param backingStoreSupplier The supplier of backing store which shall be used
     * @param compressionLevel     The compression level used in compression, this value should be
     *                             -1(default level) or between 0~9.
     * @throws IllegalArgumentException if the compression level is illegal
     * @since 1.21
     */
    public ParallelScatterZipCreator(final ExecutorService executorService,
                                     final ScatterGatherBackingStoreSupplier backingStoreSupplier,
                                     final int compressionLevel) throws IllegalArgumentException {
        if ((compressionLevel < Deflater.NO_COMPRESSION || compressionLevel > Deflater.BEST_COMPRESSION)
                && compressionLevel != Deflater.DEFAULT_COMPRESSION) {
            throw new IllegalArgumentException("Compression level is expected between -1~9");
        }

        this.backingStoreSupplier = backingStoreSupplier;
        this.executorService = executorService;
        this.compressionLevel = compressionLevel;
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
        submitStreamAwareCallable(createCallable(zipArchiveEntry, source));
    }

    /**
     * Adds an archive entry to this archive.
     * <p>
     * This method is expected to be called from a single client thread
     * </p>
     *
     * @param zipArchiveEntryRequestSupplier Should supply the entry to be added.
     * @since 1.13
     */
    public void addArchiveEntry(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        submitStreamAwareCallable(createCallable(zipArchiveEntryRequestSupplier));
    }

    private void closeAll() {
        for (final ScatterZipOutputStream scatterStream : streams) {
            try {
                scatterStream.close();
            } catch (final IOException ex) { //NOSONAR
                // no way to properly log this
            }
        }
    }

    /**
     * Creates a callable that will compress the given archive entry.
     *
     * <p>This method is expected to be called from a single client thread.</p>
     *
     * Consider using {@link #addArchiveEntry addArchiveEntry}, which wraps this method and {@link #submitStreamAwareCallable submitStreamAwareCallable}.
     * The most common use case for using {@link #createCallable createCallable} and {@link #submitStreamAwareCallable submitStreamAwareCallable} from a
     * client is if you want to wrap the callable in something that can be prioritized by the supplied
     * {@link ExecutorService}, for instance to process large or slow files first.
     * Since the creation of the {@link ExecutorService} is handled by the client, all of this is up to the client.
     *
     * @param zipArchiveEntry The entry to add.
     * @param source          The source input stream supplier
     * @return A callable that should subsequently passed to #submitStreamAwareCallable, possibly in a wrapped/adapted from. The
     * value of this callable is not used, but any exceptions happening inside the compression
     * will be propagated through the callable.
     */

    public final Callable<ScatterZipOutputStream> createCallable(final ZipArchiveEntry zipArchiveEntry,
        final InputStreamSupplier source) {
        final int method = zipArchiveEntry.getMethod();
        if (method == ZipMethod.UNKNOWN_CODE) {
            throw new IllegalArgumentException("Method must be set on zipArchiveEntry: " + zipArchiveEntry);
        }
        final ZipArchiveEntryRequest zipArchiveEntryRequest = createZipArchiveEntryRequest(zipArchiveEntry, source);
        return () -> {
            final ScatterZipOutputStream scatterStream = tlScatterStreams.get();
            scatterStream.addArchiveEntry(zipArchiveEntryRequest);
            return scatterStream;
        };
    }

    /**
     * Creates a callable that will compress archive entry supplied by {@link ZipArchiveEntryRequestSupplier}.
     *
     * <p>This method is expected to be called from a single client thread.</p>
     *
     * The same as {@link #createCallable(ZipArchiveEntry, InputStreamSupplier)}, but the archive entry
     * to be added is supplied by a {@link ZipArchiveEntryRequestSupplier}.
     *
     * @see #createCallable(ZipArchiveEntry, InputStreamSupplier)
     *
     * @param zipArchiveEntryRequestSupplier Should supply the entry to be added.
     * @return A callable that should subsequently passed to #submitStreamAwareCallable, possibly in a wrapped/adapted from. The
     * value of this callable is not used, but any exceptions happening inside the compression
     * will be propagated through the callable.
     * @since 1.13
     */
    public final Callable<ScatterZipOutputStream> createCallable(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        return () -> {
            final ScatterZipOutputStream scatterStream = tlScatterStreams.get();
            scatterStream.addArchiveEntry(zipArchiveEntryRequestSupplier.get());
            return scatterStream;
        };
    }

    private ScatterZipOutputStream createDeferred(final ScatterGatherBackingStoreSupplier scatterGatherBackingStoreSupplier)
            throws IOException {
        final ScatterGatherBackingStore bs = scatterGatherBackingStoreSupplier.get();
        // lifecycle is bound to the ScatterZipOutputStream returned
        final StreamCompressor sc = StreamCompressor.create(compressionLevel, bs); //NOSONAR
        return new ScatterZipOutputStream(bs, sc);
    }

    /**
     * Gets a message describing the overall statistics of the compression run
     *
     * @return A string
     */
    public ScatterStatistics getStatisticsMessage() {
        return new ScatterStatistics(compressionDoneAt - startedAt, scatterDoneAt - compressionDoneAt);
    }

    /**
     * Submits a callable for compression.
     *
     * @see ParallelScatterZipCreator#createCallable for details of if/when to use this.
     *
     * @param callable The callable to run, created by {@link #createCallable createCallable}, possibly wrapped by caller.
     */
    public final void submit(final Callable<? extends Object> callable) {
        submitStreamAwareCallable(() -> {
            callable.call();
            return tlScatterStreams.get();
        });
    }

    /**
     * Submits a callable for compression.
     *
     * @see ParallelScatterZipCreator#createCallable for details of if/when to use this.
     *
     * @param callable The callable to run, created by {@link #createCallable createCallable}, possibly wrapped by caller.
     * @since 1.19
     */
    public final void submitStreamAwareCallable(final Callable<? extends ScatterZipOutputStream> callable) {
        futures.add(executorService.submit(callable));
    }

    /**
     * Writes the contents this to the target {@link ZipArchiveOutputStream}.
     * <p>
     * It may be beneficial to write things like directories and manifest files to the targetStream before calling this method.
     * </p>
     * <p>
     * Calling this method will shut down the {@link ExecutorService} used by this class. If any of the {@link Callable}s {@link #submitStreamAwareCallable
     * submit}ted to this instance throws an exception, the archive can not be created properly and this method will throw an exception.
     * </p>
     *
     * @param targetStream The {@link ZipArchiveOutputStream} to receive the contents of the scatter streams
     * @throws IOException          If writing fails
     * @throws InterruptedException If we get interrupted
     * @throws ExecutionException   If something happens in the parallel execution
     */
    public void writeTo(final ZipArchiveOutputStream targetStream)
            throws IOException, InterruptedException, ExecutionException {

        try {
            // Make sure we catch any exceptions from parallel phase
            try {
                for (final Future<?> future : futures) {
                    future.get();
                }
            } finally {
                executorService.shutdown();
            }

            executorService.awaitTermination(1000 * 60L, TimeUnit.SECONDS);  // == Infinity. We really *must* wait for this to complete

            // It is important that all threads terminate before we go on, ensure happens-before relationship
            compressionDoneAt = System.currentTimeMillis();

            for (final Future<? extends ScatterZipOutputStream> future : futures) {
                final ScatterZipOutputStream scatterStream = future.get();
                scatterStream.zipEntryWriter().writeNextZipEntry(targetStream);
            }

            for (final ScatterZipOutputStream scatterStream : streams) {
                scatterStream.close();
            }

            scatterDoneAt = System.currentTimeMillis();
        } finally {
            closeAll();
        }
    }
}

