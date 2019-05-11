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

import static java.util.Collections.synchronizedList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;
import org.apache.commons.compress.parallel.ScatterGatherBackingStoreSupplier;
import org.apache.commons.compress.utils.BoundedInputStream;

/**
 * Creates a zip using internally parallel thread executor, but ensuring consistent zip entries order
 *
 * Public method of this class are expected to be called from a single client thread
 *
 * creating instances:
 * either use externally defined ThreadPool executor (must have at least 2 threads, 1 for compressing + 1 for writing)
 * <PRE>
 * ExecutorService es = ..
 * OrderedParallelScatterZipCreator zipCreator = new OrderedParallelScatterZipCreator(es, false);
 * </PRE>
 * or use auto-created (and shutdown) ThreadPool executor
 * <PRE>
 * OrderedParallelScatterZipCreator zipCreator = new OrderedParallelScatterZipCreator();
 * </PRE>
 *
 * Adding entries, then writing result zip output
 * <PRE>
 * zipCreator.addArchiveEntry(..)
 * zipCreator.addArchiveEntry(..)
 *
 * ZipArchiveOutputStream zipOutputStream = ...
 * zipCreator.finishWrite(zipOutputStream);
 * </PRE>
 *
 * or (better parallelization, writing compressed results on the fly)
 *
 * <PRE>
 * zipCreator.addArchiveEntry(..)
 * zipCreator.addArchiveEntry(..)
 *
 * ZipArchiveOutputStream zipOutputStream = ...
 * zipCreator.startWriteTo(zipOutputStream);
 *
 * zipCreator.addArchiveEntry(..)
 * zipCreator.addArchiveEntry(..)
 *
 * zipCreator.waitFinishWriteTo();
 * </PRE>
 */
public class ParallelScatterZipCreator {

    private static final ParallelScatterZipEntry EOF_MARKER = new ParallelScatterZipEntry(null, 0, null);

    private final ExecutorService es;
    private final boolean closeExecutorService;
    private final ScatterGatherBackingStoreSupplier backingStoreSupplier;

    private final List<ThreadBackingStore> threadBackingStores = synchronizedList(new ArrayList<ThreadBackingStore>());

    private BlockingQueue<Future<ParallelScatterZipEntry>> futureWriteQueue = new LinkedBlockingDeque<>();
    private Future<?> mainWriter;
    private Exception mainWriterEx;

    private final AtomicLong compressionElapsed = new AtomicLong();
    private final AtomicLong scatterElapsed = new AtomicLong();

    public static class ParallelScatterZipEntry {
        private final ZipArchiveEntry zipArchiveEntry;
        private final long len;
        private final ThreadBackingStore threadBackingStore;

        ParallelScatterZipEntry(ZipArchiveEntry zipArchiveEntry,
                long len, ThreadBackingStore threadBackingStore) {
            this.zipArchiveEntry = zipArchiveEntry;
            this.len = len;
            this.threadBackingStore = threadBackingStore;
        }

        InputStream createInputStream() {
            return new BoundedInputStream(threadBackingStore.inputStream, len);
        }

    }

    /** inner input/output Streams backing store, per Thread */
    private class ThreadBackingStore {
        ScatterGatherBackingStore backingStore;
        OutputStream ouputStream;
        InputStream inputStream;

        ThreadBackingStore(ScatterGatherBackingStore backingStore) throws IOException {
            this.backingStore = backingStore;
            this.ouputStream = createOutputStream();
            this.inputStream = backingStore.getInputStream();
        }

        // ... adapter for non-existent method backingStore.createOutputStream();
        // TODO move to class ScatterGatherBackingStore
        OutputStream createOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    byte[] data = new byte[] { (byte) b };
                    backingStore.writeOut(data, 0, 1);
                }
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    backingStore.writeOut(b, off, len);
                }
                @Override
                public void flush() throws IOException {
                    // backingStore.flush();
                }
                @Override
                public void close() throws IOException {
                    // do not close underlyinh backingstore
                }
            };
        }

    }

    private static class DefaultBackingStoreSupplier implements ScatterGatherBackingStoreSupplier {
        final AtomicInteger storeNum = new AtomicInteger(0);

        @Override
        public ScatterGatherBackingStore get() throws IOException {
            final File tempFile = File.createTempFile("parallelscatter", "n" + storeNum.incrementAndGet());
            return new FileBasedScatterGatherBackingStore(tempFile);
        }
    }

    private final ThreadLocal<ThreadBackingStore> tlScatterStreams = new ThreadLocal<ThreadBackingStore>() {
        @Override
        protected ThreadBackingStore initialValue() {
            try {
                final ThreadBackingStore res = new ThreadBackingStore(backingStoreSupplier.get());
                threadBackingStores.add(res);
                return res;
            } catch (final IOException e) {
                throw new RuntimeException(e); //NOSONAR
            }
        }
    };

    /**
     * Create a OrderedParallelScatterZipCreator with default threads, which is set to the number of available
     * processors (or min 2), as defined by {@link java.lang.Runtime#availableProcessors}
     */
    public ParallelScatterZipCreator() {
        this(createExecutorService(), true, new DefaultBackingStoreSupplier());
    }

    /**
     * Create a OrderedParallelScatterZipCreator with default threads, which is set to the number of available
     * processors (or min 2), as defined by {@link java.lang.Runtime#availableProcessors}
     *
     * @param backingStoreSupplier The supplier of backing store which shall be used
     */
    public ParallelScatterZipCreator(
            final ScatterGatherBackingStoreSupplier backingStoreSupplier) {
        this(createExecutorService(), true, backingStoreSupplier);
    }

    private static ExecutorService createExecutorService() {
        int nThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        return Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Create a OrderedParallelScatterZipCreator
     *
     * @param executorService The executorService to use.
     * @param closeExecutorService flag to close executor service at end
     */
    public ParallelScatterZipCreator(
            final ExecutorService executorService,
            final boolean closeExecutorService) {
        this(executorService, closeExecutorService, new DefaultBackingStoreSupplier());
    }

    /**
     * Create a OrderedParallelScatterZipCreator
     *
     * @param executorService The executorService to use.
     * @param closeExecutorService flag to close executor service at end
     * @param backingStoreSupplier The supplier of backing store which shall be used
     */
    public ParallelScatterZipCreator(
            final ExecutorService executorService,
            final boolean closeExecutorService,
            final ScatterGatherBackingStoreSupplier backingStoreSupplier) {
        this.es = executorService;
        this.closeExecutorService = closeExecutorService;
        this.backingStoreSupplier = backingStoreSupplier;
    }

    /**
     * Adds an archive entry to this archive.
     *
     * @param zipArchiveEntry The entry to add.
     * @param source          The source input stream supplier
     */

    public void addArchiveEntry(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier source) {
        submit(createCallable(zipArchiveEntry, source));
    }

    /**
     * Adds an archive entry to this archive.
     *
     * @param zipArchiveEntryRequestSupplier Should supply the entry to be added.
     */
    public void addArchiveEntry(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        submit(createCallable(zipArchiveEntryRequestSupplier));
    }

    /**
     * Submit an entry for compression.
     * 
     * @param callable the entry to be added
     */
    protected final void submit(final Callable<ParallelScatterZipEntry> callable) {
        Future<ParallelScatterZipEntry> futureEntry = es.submit(callable);
        futureWriteQueue.add(futureEntry);
    }

    /**
     * Create a callable that will compress the given archive entry.
     * 
     * @param ze entry to add
     * @param inputStreamSupplier supplier for input stream of zip entry
     * @return the callable to submit for adding the entry
     */
    protected final Callable<ParallelScatterZipEntry> createCallable(final ZipArchiveEntry ze, final InputStreamSupplier inputStreamSupplier) {
        final int method = ze.getMethod();
        if (method == ZipMethod.UNKNOWN_CODE) {
            throw new IllegalArgumentException("Method must be set on zipArchiveEntry: " + ze);
        }
        return new Callable<ParallelScatterZipEntry>() {
            @Override
            public ParallelScatterZipEntry call() throws Exception {
                try (InputStream stream = inputStreamSupplier.get()) {
                    return compressEntryToAppend(ze, stream);
                }
            }
        };
    }

    /**
     * Create a callable that will compress archive entry supplied by {@link ZipArchiveEntryRequestSupplier}.
     * 
     * @param zipArchiveEntryRequestSupplier supplier for a zip entry to add
     * @return the callable to submit for adding the entry
     */
    protected final Callable<ParallelScatterZipEntry> createCallable(final ZipArchiveEntryRequestSupplier zipArchiveEntryRequestSupplier) {
        return new Callable<ParallelScatterZipEntry>() {
            @Override
            public ParallelScatterZipEntry call() throws Exception {
                ZipArchiveEntryRequest zeReq = zipArchiveEntryRequestSupplier.get();
                ZipArchiveEntry ze = zeReq.getZipArchiveEntry();
                try (InputStream stream = zeReq.getPayloadStream()) {
                    return compressEntryToAppend(ze, stream);
                }
            }
        };
    }

    protected ParallelScatterZipEntry compressEntryToAppend(
            ZipArchiveEntry ze, InputStream payloadStream
            ) throws IOException {
        long startTime = System.currentTimeMillis();

        final ThreadBackingStore threadBackingStore = tlScatterStreams.get();
        OutputStream outputStream = threadBackingStore.ouputStream;

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        StreamCompressor streamCompressor = StreamCompressor.create(
                outputStream, deflater);

        streamCompressor.deflate(payloadStream, ze.getMethod());

        ze.setCrc(streamCompressor.getCrc32());
        long bytesWrittenForLastEntry = streamCompressor.getBytesWrittenForLastEntry();
        ze.setCompressedSize(bytesWrittenForLastEntry);
        ze.setSize(streamCompressor.getBytesRead());
        ze.setMethod(ZipEntry.DEFLATED);
        // za.setUnixMode(UnixStat.FILE_FLAG | 0664);

        deflater.finish();
        streamCompressor.close();

        ParallelScatterZipEntry res = new ParallelScatterZipEntry(ze, bytesWrittenForLastEntry, threadBackingStore);

        long millis = System.currentTimeMillis() - startTime;
        scatterElapsed.addAndGet(millis);
        return res;
    }

    /**
     * alternative to <code>startWriteTo(); waitFinishWriteTo(); </code>
     * when using the caller thread directly
     * 
     * @param zipOutputStream the target to write to 
     * @throws IOException          If writing fails
     * @throws InterruptedException If we get interrupted
     * @throws ExecutionException   If something happens in the parallel execution
     */
    public void writeTo(ZipArchiveOutputStream zipOutputStream) throws IOException, InterruptedException, ExecutionException {
        addEndOfEntryMarker();
        mainWriterLoop(zipOutputStream);
    }

	private void addEndOfEntryMarker() {
		// jdk8: CompletableFuture.completedFuture(EOF_MARKER));
		Future<ParallelScatterZipEntry> completedMarker = new Future<ParallelScatterZipEntry>() {
			@Override
			public boolean isDone() {
				return true;
			}
			@Override
			public ParallelScatterZipEntry get() throws InterruptedException, ExecutionException {
				return EOF_MARKER;
			}
			@Override
			public ParallelScatterZipEntry get(long timeout, TimeUnit unit) {
				return EOF_MARKER;
			}
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}
			@Override
			public boolean isCancelled() {
				return false;
			}        			
		};
		this.futureWriteQueue.add(completedMarker);
	}

    public void startWriteTo(final ZipArchiveOutputStream zipOutputStream) {
        this.mainWriter = es.submit(new Runnable() {  // Does not work if es is FixedThreadPool(1) !!!
            public void run() {
                try {
                    mainWriterLoop(zipOutputStream);
                } catch (Exception ex) {
                    mainWriterEx = ex;
                    // no rethrow! executed within thread
                }
            }
        });
    }

    private void mainWriterLoop(ZipArchiveOutputStream zipOutputStream) throws IOException, InterruptedException, ExecutionException {
        for(;;) {
            Future<ParallelScatterZipEntry> future = futureWriteQueue.take();
            ParallelScatterZipEntry entry = future.get();

            long startTime = System.currentTimeMillis();
            if (entry == EOF_MARKER) {
                break;
            }

            ZipArchiveEntry ze = entry.zipArchiveEntry;
            try (InputStream rawInput = entry.createInputStream()) {
                zipOutputStream.addRawArchiveEntry(ze, rawInput);
            }

            long millis = System.currentTimeMillis() - startTime;
            scatterElapsed.addAndGet(millis);
        }

        // cleanup on finish
        for(ThreadBackingStore threadBackingStore : threadBackingStores) {
            try {
                threadBackingStore.backingStore.close();
            } catch (IOException ex) { //NOSONAR
                // no way to properly log this
            }
        }
    }

    /**
     * Write the contents this to the target {@link ZipArchiveOutputStream}.
     * <p>
     *
     * @throws IOException          If writing fails
     * @throws InterruptedException If we get interrupted
     * @throws ExecutionException   If something happens in the parallel execution
     */
    public void waitFinishWriteTo() throws IOException, InterruptedException, ExecutionException {
    	addEndOfEntryMarker();
        try {
            mainWriter.get();
        } catch(Exception ex) {
            mainWriterEx = ex; // ?? redundant
        } finally {
            if (closeExecutorService) {
                es.shutdown();
                es.awaitTermination(5, TimeUnit.MINUTES);
            }
        }

        if (mainWriterEx != null) {
            if (mainWriterEx instanceof IOException) {
                throw (IOException) mainWriterEx;
            }
            throw new ExecutionException(mainWriterEx);
        }
    }

    /**
     * Returns a message describing the overall statistics of the compression run
     *
     * @return A string
     */
    public ScatterStatistics getStatisticsMessage() {
        return new ScatterStatistics(compressionElapsed.get(), scatterElapsed.get());
    }

}

