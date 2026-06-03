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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Guarantees that decompressing a Pack200 stream works on a stock JVM, i.e. <strong>without</strong> {@code --add-opens java.base/java.io=ALL-UNNAMED}.
 * <p>
 * This cannot be checked in-process: the project's Surefire configuration adds that flag on Java 17+ (it is required by other tests that reflect into
 * {@code java.io}). With the package open, the formerly-reflective unpack code passes too, so an ordinary test could not tell the fix from the bug. This test
 * therefore launches {@link Pack200ModuleAccessHelper} in a fresh child JVM with no such flag, reproducing exactly what a real caller's JVM looks like, and
 * asserts the child succeeds.
 * </p>
 *
 * @see Pack200ModuleAccessHelper
 */
class Pack200ModuleAccessTest {

    /** Strong encapsulation (denying reflective {@code setAccessible} on non-open {@code java.base} packages by default) only applies on Java 16 and later. */
    private static final int MIN_ENFORCING_JAVA = 16;

    private static int specVersion() {
        // "1.8" on Java 8, "9"/"11"/"17"/... on Java 9+.
        final String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2));
        }
        return Integer.parseInt(version);
    }

    @Test
    void unpackDoesNotRequireAddOpens() throws Exception {
        assumeTrue(specVersion() >= MIN_ENFORCING_JAVA,
                "On Java < " + MIN_ENFORCING_JAVA + " reflective access to java.io is permitted, so a child JVM cannot reproduce the failure.");
        final Path packFile = Paths.get(getClass().getResource("/pack200/HelloWorld.pack").toURI());
        final boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        final String javaBin = Paths.get(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
        // java.class.path is enough to relaunch on the same classpath (it is honored even when Surefire uses a manifest-only booter JAR).
        final ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", System.getProperty("java.class.path"),
                Pack200ModuleAccessHelper.class.getName(), packFile.toString()).redirectErrorStream(true);
        // Make sure no ambient flags (e.g. an --add-opens smuggled in via JAVA_TOOL_OPTIONS) leak into the child and mask the failure.
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        final Process process = builder.start();
        final ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            final Future<String> outputFuture = reader.submit(() -> IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
            final boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
            }
            final String output = outputFuture.get(30, TimeUnit.SECONDS);
            assertTrue(finished, () -> "Child JVM did not finish in time. Output:\n" + output);
            assertEquals(0, process.exitValue(),
                    () -> "Decompressing a Pack200 stream must not require --add-opens java.base/java.io=ALL-UNNAMED.\nChild JVM output:\n" + output);
            // Sanity: the child really exercised the FileInputStream path that used to throw InaccessibleObjectException.
            assertTrue(output.contains("OK   FileInputStream"), () -> "Unexpected child JVM output:\n" + output);
        } finally {
            reader.shutdownNow();
        }
    }
}
