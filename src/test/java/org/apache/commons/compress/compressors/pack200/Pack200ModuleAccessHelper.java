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
package org.apache.commons.compress.compressors.pack200;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Not a unit test: a tiny program launched in a <em>separate</em> JVM by {@link Pack200ModuleAccessTest}, deliberately <strong>without</strong>
 * {@code --add-opens java.base/java.io=ALL-UNNAMED}.
 * <p>
 * Decompressing a Pack200 stream must not require reflective access to {@code java.io} internals. When it did (see the history of
 * {@code Pack200UnpackerAdapter}), this program threw {@link java.lang.reflect.InaccessibleObjectException} on Java 17 and later for any input that was a
 * {@link FileInputStream} or a {@link java.io.FilterInputStream} (such as a {@link BufferedInputStream}). This is the exact situation real callers hit, because
 * the project's own test suite hides it by adding {@code --add-opens} to Surefire.
 * </p>
 * <p>
 * Usage: {@code java -cp <classpath> org.apache.commons.compress.compressors.pack200.Pack200ModuleAccessHelper <packFile>}. Prints one line per input form and
 * exits with status {@code 0} only if every form decompressed; otherwise it prints the failure and exits non-zero.
 * </p>
 */
public final class Pack200ModuleAccessHelper {

    /**
     * Decompresses {@code packFile} once for each interesting input-stream form.
     *
     * @param args {@code args[0]} is the path to a Pack200 file.
     */
    public static void main(final String[] args) throws Exception {
        final String packFile = args[0];
        int failures = 0;
        // A raw FileInputStream used to trigger reflection on java.io.FileInputStream.path.
        failures += attempt("FileInputStream", new FileInputStream(packFile));
        // A BufferedInputStream (a FilterInputStream) used to trigger reflection on java.io.FilterInputStream.in.
        failures += attempt("BufferedInputStream", new BufferedInputStream(new FileInputStream(packFile)));
        // Files.newInputStream returns a non-FilterInputStream that never needed reflection; included as a control.
        failures += attempt("Files.newInputStream", Files.newInputStream(Paths.get(packFile)));
        if (failures > 0) {
            System.out.println("FAILED: " + failures + " input form(s) could not be decompressed without --add-opens.");
            System.exit(1);
        }
        System.out.println("SUCCESS: all input forms decompressed without --add-opens.");
    }

    private static int attempt(final String label, final InputStream rawInput) {
        try (InputStream in = rawInput;
                Pack200CompressorInputStream pack200 = new Pack200CompressorInputStream(in)) {
            final byte[] buffer = new byte[8192];
            long total = 0;
            int n;
            while ((n = pack200.read(buffer)) != -1) {
                total += n;
            }
            System.out.println("  OK   " + label + " -> " + total + " bytes");
            return 0;
        } catch (final Throwable t) {
            System.out.println("  FAIL " + label + " -> " + t);
            return 1;
        }
    }

    private Pack200ModuleAccessHelper() {
        // Utility class launched via main(String[]).
    }
}
