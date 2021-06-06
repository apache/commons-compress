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

import java.io.IOException;

/**
 * Thrown if reading from an archive or compressed stream results in a RuntimeException.
 *
 * <p>Usually this means the input has been corrupt in a way Compress'
 * code didn't detect by itself. If the input is not corrupt then
 * you've found a bug in Compress and we ask you to report it.</p>
 *
 * @since 1.21
 */
public class UnhandledInputException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Wraps an unhandled RuntimeException for an input of unknown name.
     */
    public UnhandledInputException(final RuntimeException ex) {
        this(ex, null);
    }

    /**
     * Wraps an unhandled RuntimeException for an input with a known name.
     *
     * @param ex the unhandled excetion
     * @param inputName name of the input
     */
    public UnhandledInputException(final RuntimeException ex, final String inputName) {
        super(buildMessage(inputName), ex);
    }

    private static String buildMessage(final String name) {
        return "Either the input"
            + (name == null ? "" : " " + name)
            + " is corrupt or you have found a bug in Apache Commons Compress. Please report it at"
            + " https://issues.apache.org/jira/browse/COMPRESS if you think this is a bug.";
    }
}
