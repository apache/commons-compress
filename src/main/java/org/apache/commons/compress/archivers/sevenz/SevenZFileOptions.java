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

/**
 * Collects options for reading 7z archives.
 *
 * @since 1.19
 * @Immutable
 * @deprecated Use {@link SevenZFile.Builder}.
 */
@Deprecated
public class SevenZFileOptions {

    /**
     * Mutable builder for the immutable {@link SevenZFileOptions}.
     *
     * @since 1.19
     */
    public static class Builder {

        private int maxMemoryLimitKb = SevenZFile.Builder.MEMORY_LIMIT_IN_KB;
        private boolean useDefaultNameForUnnamedEntries = SevenZFile.Builder.USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES;
        private boolean tryToRecoverBrokenArchives = SevenZFile.Builder.TRY_TO_RECOVER_BROKEN_ARCHIVES;

        /**
         * Builds the {@link SevenZFileOptions}.
         *
         * @return configured {@link SevenZFileOptions}.
         */
        public SevenZFileOptions build() {
            return new SevenZFileOptions(maxMemoryLimitKb, useDefaultNameForUnnamedEntries, tryToRecoverBrokenArchives);
        }

        /**
         * Sets the maximum amount of memory to use for parsing the archive and during extraction.
         * <p>
         * Not all codecs will honor this setting. Currently only LZMA and LZMA2 are supported.
         * </p>
         *
         * @param maxMemoryLimitKiB limit of the maximum amount of memory to use in kibibytes.
         * @return the reconfigured builder
         */
        public Builder withMaxMemoryLimitInKb(final int maxMemoryLimitKiB) {
            this.maxMemoryLimitKb = maxMemoryLimitKiB;
            return this;
        }

        /**
         * Sets whether {@link SevenZFile} will try to recover broken archives where the CRC of the file's metadata is 0.
         * <p>
         * This special kind of broken archive is encountered when mutli volume archives are closed prematurely. If you enable this option SevenZFile will trust
         * data that looks as if it could contain metadata of an archive and allocate big amounts of memory. It is strongly recommended to not enable this
         * option without setting {@link #withMaxMemoryLimitInKb} at the same time.
         * </p>
         *
         * @param tryToRecoverBrokenArchives if true SevenZFile will try to recover archives that are broken in the specific way
         * @return the reconfigured builder
         * @since 1.21
         */
        public Builder withTryToRecoverBrokenArchives(final boolean tryToRecoverBrokenArchives) {
            this.tryToRecoverBrokenArchives = tryToRecoverBrokenArchives;
            return this;
        }

        /**
         * Sets whether entries without a name should get their names set to the archive's default file name.
         *
         * @param useDefaultNameForUnnamedEntries if true the name of unnamed entries will be set to the archive's default name
         * @return the reconfigured builder
         */
        public Builder withUseDefaultNameForUnnamedEntries(final boolean useDefaultNameForUnnamedEntries) {
            this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
            return this;
        }
    }

    /**
     * The default options.
     * <ul>
     * <li>no memory limit</li>
     * <li>don't modify the name of unnamed entries</li>
     * </ul>
     */
    public static final SevenZFileOptions DEFAULT = new SevenZFileOptions(SevenZFile.Builder.MEMORY_LIMIT_IN_KB,
            SevenZFile.Builder.USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES, SevenZFile.Builder.TRY_TO_RECOVER_BROKEN_ARCHIVES);

    /**
     * Obtains a builder for SevenZFileOptions.
     *
     * @return a builder for SevenZFileOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final int maxMemoryLimitKiB;
    private final boolean useDefaultNameForUnnamedEntries;
    private final boolean tryToRecoverBrokenArchives;

    private SevenZFileOptions(final int maxMemoryLimitKb, final boolean useDefaultNameForUnnamedEntries, final boolean tryToRecoverBrokenArchives) {
        this.maxMemoryLimitKiB = maxMemoryLimitKb;
        this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
        this.tryToRecoverBrokenArchives = tryToRecoverBrokenArchives;
    }

    /**
     * Gets the maximum amount of memory to use for parsing the archive and during extraction.
     * <p>
     * Not all codecs will honor this setting. Currently only LZMA and LZMA2 are supported.
     * </p>
     *
     * @return the maximum amount of memory to use for extraction in kibibytes.
     */
    public int getMaxMemoryLimitInKb() {
        return maxMemoryLimitKiB;
    }

    /**
     * Gets whether {@link SevenZFile} shall try to recover from a certain type of broken archive.
     *
     * @return whether SevenZFile shall try to recover from a certain type of broken archive.
     * @since 1.21
     */
    public boolean getTryToRecoverBrokenArchives() {
        return tryToRecoverBrokenArchives;
    }

    /**
     * Gets whether entries without a name should get their names set to the archive's default file name.
     *
     * @return whether entries without a name should get their names set to the archive's default file name
     */
    public boolean getUseDefaultNameForUnnamedEntries() {
        return useDefaultNameForUnnamedEntries;
    }
}
