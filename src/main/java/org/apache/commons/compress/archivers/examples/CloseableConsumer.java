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
package org.apache.commons.compress.archivers.examples;

import java.io.Closeable;
import java.io.IOException;

/**
 * Callback that is informed about a closable resource that has been
 * wrapped around a passed in stream or channel by Expander or
 * Archiver when Expander or Archiver no longer need them.
 *
 * <p>This provides a way to close said resources in the calling code.</p>
 *
 * @since 1.19
 */
public interface CloseableConsumer {
    /**
     * Closes the passed in Closeable immediately.
     */
    CloseableConsumer CLOSING_CONSUMER = Closeable::close;

    /**
     * Completely ignores the passed in Closeable.
     */
    CloseableConsumer NULL_CONSUMER = c -> { };

    /**
     * Callback that is informed about a closable resource that has
     * been wrapped around a passed in stream or channel by Expander
     * or Archiver when Expander or Archiver no longer need them.
     *
     * @param c Closeable created by Expander or Archiver that is now
     * no longer used
     *
     * @throws IOException on error
     */
    void accept(Closeable c) throws IOException;
}
