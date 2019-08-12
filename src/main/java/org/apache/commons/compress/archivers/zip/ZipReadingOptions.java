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

import java.util.Objects;

/**
 * Collects options that control parsing of ZIP archives.
 *
 * @since 1.19
 */
public final class ZipReadingOptions {
    private static final ZipEncoding DEFAULT_ZIPENCODING = ZipEncodingHelper.UTF8_ZIP_ENCODING;
    private static final boolean DEFAULT_USEUNICODEEXTRAFIELDS = true;
    private static final ExtraFieldUtils.ParseErrorBehavior DEFAULT_EXTRAFIELDPARSEERRORBEHAVIOR =
        ExtraFieldUtils.ParseErrorBehavior.MAKE_UNRECOGNIZED;

    private final ZipEncoding zipEncoding;
    private final boolean useUnicodeExtraFields;
    private final ExtraFieldUtils.ParseErrorBehavior extraFieldParseErrorBehavior;

    /**
     * The default reading options.
     *
     * <ul>
     * <li>UTF-8 encoding</li>
     * <li>use unicode extra fields when available</li>
     * <li>convert invalid extra fields into {@link UnparseableExtraFieldData}</li>
     * </ul>
     */
    public static final ZipReadingOptions DEFAULT = new ZipReadingOptions(DEFAULT_ZIPENCODING,
        DEFAULT_USEUNICODEEXTRAFIELDS, DEFAULT_EXTRAFIELDPARSEERRORBEHAVIOR);

    private ZipReadingOptions(ZipEncoding zipEncoding, boolean useUnicodeExtraFields,
        ExtraFieldUtils.ParseErrorBehavior extraFieldParseErrorBehavior) {
        this.zipEncoding = zipEncoding;
        this.useUnicodeExtraFields = useUnicodeExtraFields;
        this.extraFieldParseErrorBehavior = extraFieldParseErrorBehavior;
    }

    /**
     * The zip encoding to use for file names and the file comment.
     */
    public ZipEncoding getZipEncoding() {
        return zipEncoding;
    }

    /**
     * Whether to look for and use Unicode extra fields.
     */
    public boolean getUseUnicodeExtraFields() {
        return useUnicodeExtraFields;
    }

    /**
     * How to handle extra fields that are generally supported by
     * Commons Compress but cannot be parsed in an archive.
     *
     * <p>The archive may contain corrupt extra fields or use a
     * version not supported by Commons Compress.</p>
     */
    public ExtraFieldUtils.ParseErrorBehavior getExtraFieldParseErrorBehavior() {
        return extraFieldParseErrorBehavior;
    }

    /**
     * Obtains a builder for {@link ZipReadingOptions}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ZipReadingOptions}.
     */
    public static class Builder {
        private ZipEncoding zipEncoding = DEFAULT_ZIPENCODING;
        private boolean useUnicodeExtraFields = DEFAULT_USEUNICODEEXTRAFIELDS;
        private ExtraFieldUtils.ParseErrorBehavior extraFieldParseErrorBehavior =
            DEFAULT_EXTRAFIELDPARSEERRORBEHAVIOR;

        /**
         * Configures the ZIP encoding.
         *
         * @throws NullPointException if {@code zipEncoding} is null
         */
        public Builder withZipEncoding(ZipEncoding zipEncoding) {
            this.zipEncoding = Objects.requireNonNull(zipEncoding, "zipEncoding must not be null");
            return this;
        }

        /**
         * Configures the encoding.
         *
         * @param encoding name of the encoding to use, {@code null}
         * means to use the platform's default encoding
         */
        public Builder withEncoding(String encoding) {
            return withZipEncoding(ZipEncodingHelper.getZipEncoding(encoding));
        }

        /**
         * Configures whether to use unicode extra fields to configure names and comments.
         */
        public Builder withUseUnicodeExtraFields(boolean useUnicodeExtraFields) {
            this.useUnicodeExtraFields = useUnicodeExtraFields;
            return this;
        }

        /**
         * Configures how to handle extra fields that are generally
         * supported by Commons Compress but cannot be parsed in an
         * archive.
         *
         * @throws NullPointException if {@code extraFieldParseErrorBehavior} is null
         */
        public Builder withextraFieldParseErrorBehavior(ExtraFieldUtils.ParseErrorBehavior extraFieldParseErrorBehavior) {
            this.extraFieldParseErrorBehavior = Objects.requireNonNull(extraFieldParseErrorBehavior,
                "extraFieldParseErrorBehavior must not be null");
            return this;
        }

        /**
         * Create the configured {@link ZipReadingOptions}.
         */
        public ZipReadingOptions build() {
            return new ZipReadingOptions(zipEncoding, useUnicodeExtraFields, extraFieldParseErrorBehavior);
        }
    }
}
