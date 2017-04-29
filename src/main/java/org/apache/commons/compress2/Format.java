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
package org.apache.commons.compress2;

import java.nio.ByteBuffer;
import java.util.Comparator;

/**
 * Common meta-data for archive and compression formats.
 */
public interface Format {
    /**
     * The name by which this format is known.
     * @return the name by which this format is known
     */
    String getName();

    /**
     * Does the format support writing?
     * @return whether writing is supported
     */
    boolean supportsWriting();

    /**
     * Does the format support content-based detection?
     * @return whether the format supports content-based detection.
     */
    boolean supportsAutoDetection();
    /**
     * If this format supports content-based detection, how many bytes does it need to read to know a channel is
     * readable by this format?
     * @return the minimal number of bytes needed
     * @throws UnsupportedOperationException if this format doesn't support content based detection.
     */
    int getNumberOfBytesRequiredForAutodetection() throws UnsupportedOperationException;
    /**
     * Verifies the given input is readable by this format.
     * @param probe a buffer holding at least {@link #getNumberOfBytesRequiredForAutodetection} bytes
     * @return whether the input is readable by this format
     * @throws UnsupportedOperationException if this format doesn't support content based detection.
     */
    boolean matches(ByteBuffer probe) throws UnsupportedOperationException;

    /**
     * Comparator that sorts {@link Format}s in ascending order of number of bytes required for aut-detection.
     *
     * <p>Formats that don't support auto-detection at all are sorted last.</p>
     */
    public static final Comparator<Format> AUTO_DETECTION_ORDER = new Comparator<Format>() {
        @Override
        public int compare(Format f1, Format f2) {
            if (f1.supportsAutoDetection() && f2.supportsAutoDetection()) {
                return f1.getNumberOfBytesRequiredForAutodetection() - f2.getNumberOfBytesRequiredForAutodetection();
            }
            if (!f1.supportsAutoDetection() && !f2.supportsAutoDetection()) {
                return 0;
            }
            if (f1.supportsAutoDetection()) {
                return -1;
            }
            return 1;
        }
    };

}
