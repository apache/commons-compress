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
package org.apache.commons.compress.archivers.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

/**
 * Race-safe {@link Extractor} for platforms whose file system provider exposes a {@link SecureDirectoryStream}. The parent of
 * each regular file is re-walked component by component from the root directory handle at write time, every component opened
 * with {@link LinkOption#NOFOLLOW_LINKS}, and the file is created relative to the innermost directory handle (file
 * descriptor). A third party therefore cannot win a time-of-check-to-time-of-use race by swapping a parent component for a
 * symbolic link between resolution and write: the write targets the pinned directory inode, not a re-resolved path.
 * <p>
 * Directory, symbolic-link, and hard-link creation remain path-based (the {@code java.nio.file} API exposes no
 * descriptor-relative {@code mkdirat}/{@code symlinkat}/{@code linkat}); the component verification still uses no-follow
 * semantics, but creation of those node types carries the documented residual TOCTOU window. Regular-file writes, the common
 * and highest-volume case, are fully race-safe.
 * </p>
 * <p>
 * Instances are created by {@link Extractor#newExtractor(Path)}; this type is an implementation detail and is not part of the
 * public API surface.
 * </p>
 */
final class SecureExtractor extends Extractor {

    @SuppressWarnings("unchecked")
    private static SecureDirectoryStream<Path> asSecure(final DirectoryStream<Path> stream) {
        return (SecureDirectoryStream<Path>) stream;
    }

    SecureExtractor(final Path rootDirectory) {
        super(rootDirectory);
    }

    @Override
    void writeFile(final Path leaf, final InputStream content) throws IOException {
        final Path relative = rootDirectory.relativize(leaf);
        final int count = relative.getNameCount();
        final Path fileName = relative.getFileName();
        final Deque<SecureDirectoryStream<Path>> handles = new ArrayDeque<>();
        try {
            SecureDirectoryStream<Path> dir = asSecure(Files.newDirectoryStream(rootDirectory));
            handles.push(dir);
            for (int i = 0; i < count - 1; i++) {
                dir = asSecure(dir.newDirectoryStream(relative.getName(i), LinkOption.NOFOLLOW_LINKS));
                handles.push(dir);
            }
            runBeforeLeafWrite();
            final Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            options.add(LinkOption.NOFOLLOW_LINKS);
            if (isOverwrite()) {
                options.add(StandardOpenOption.CREATE);
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                options.add(StandardOpenOption.CREATE_NEW);
            }
            try (SeekableByteChannel channel = dir.newByteChannel(fileName, options);
                    OutputStream out = Channels.newOutputStream(channel)) {
                IOUtils.copy(content, out);
            }
        } finally {
            while (!handles.isEmpty()) {
                handles.pop().close();
            }
        }
    }
}
