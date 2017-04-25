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
package org.apache.commons.compress;

/**
 *
 * During initialization, some streams compute expected memory use.
 * They should check this value and throw a MemoryLimitException if the
 * estimated memory use is greater that {@link MemoryLimit#MEMORY_LIMIT_IN_KB}.
 * <p/>
 * During compression/archiving, streams can allocate byte arrays based
 * on a value read in from the stream.  Corrupt files can cause compressors/archivers
 * to cause {@link OutOfMemoryError}s.  Compressors/archivers should check
 * this maximum threshold before allocating memory and throw a {@link MemoryLimitException}
 * if the allocation would exceed this limit.
 * <p/>
 * To avoid changes in legacy behavior, {@link MemoryLimit#MEMORY_LIMIT_IN_KB}
 * is set to {@link MemoryLimit#NO_LIMIT}.  However, in applications that might
 * encounter untrusted/corrupt files, we encourage setting the limit to something
 * reasonable for the application.
 * <p/>
 * As of 1.14, this limit should be observed when instantiating CompressorStreams.
 * Work remains to propagate memory limit checks throughout the codebase.
 *
 * @since 1.14
 */
public class MemoryLimit {

    public static final int NO_LIMIT = -1;
    public static volatile int MEMORY_LIMIT_IN_KB = NO_LIMIT;

    /**
     * Sets {@link MemoryLimit#MEMORY_LIMIT_IN_KB}.
     * @param memoryLimitInKb limit in kilobytes
     *
     * @throws IllegalArgumentException if value is &lt; -1
     */
    public static void setMemoryLimitInKb(int memoryLimitInKb) {
        if (memoryLimitInKb < -1) {
            throw new IllegalArgumentException("MemoryLimit must be > -2");
        }
        //TODO: do we want to set an absolute upper limit?!
        MEMORY_LIMIT_IN_KB = memoryLimitInKb;
    }

    public static int getMemoryLimitInKb() {
        return MEMORY_LIMIT_IN_KB;
    }

    public static void checkLimitInKb(long memoryNeeded) throws MemoryLimitException {
        if (memoryNeeded < 0) {
            throw new IllegalArgumentException("MemoryLimit must be > -1");
        }

        if (memoryNeeded >> 10 > Integer.MAX_VALUE) {
            throw new MemoryLimitException(memoryNeeded,
                    (MEMORY_LIMIT_IN_KB < 0) ? Integer.MAX_VALUE : MEMORY_LIMIT_IN_KB);
        }

        if (MEMORY_LIMIT_IN_KB > -1 && memoryNeeded > MEMORY_LIMIT_IN_KB) {
                throw new MemoryLimitException(memoryNeeded, MEMORY_LIMIT_IN_KB);
        }
    }
}
