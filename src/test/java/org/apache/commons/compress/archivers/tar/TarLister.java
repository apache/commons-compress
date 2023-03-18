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

package org.apache.commons.compress.archivers.tar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Simple command line application that lists the contents of a tar archive.
 *
 * <p>The name of the archive must be given as a command line argument.</p>
 * <p>The optional second argument specifies the encoding to assume for file names.</p>
 *
 * @since 1.11
 */
public final class TarLister {

    private static void log(final TarArchiveEntry ae) {
        final StringBuilder sb = new StringBuilder(Integer.toOctalString(ae.getMode()))
            .append(" ");
        String name = ae.getUserName();
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(ae.getLongUserId());
        }
        sb.append("/");
        name = ae.getGroupName();
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(ae.getLongGroupId());
        }
        sb.append(" ");
        if (ae.isSparse()) {
            sb.append(ae.getRealSize());
        } else if (ae.isCharacterDevice() || ae.isBlockDevice()) {
            sb.append(ae.getDevMajor()).append(",").append(ae.getDevMinor());
        } else {
            sb.append(ae.getSize());
        }
        sb.append(" ").append(ae.getLastModifiedDate()).append(" ");
        sb.append(ae.getName());
        if (ae.isSymbolicLink() || ae.isLink()) {
            if (ae.isSymbolicLink()) {
                sb.append(" -> ");
            } else {
                sb.append(" link to ");
            }
            sb.append(ae.getLinkName());
        }
        if (ae.isSparse()) {
            sb.append(" (sparse)");
        }
        System.out.println(sb);
    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        System.out.println("Analysing " + args[0]);
        final File f = new File(args[0]);
        if (!f.isFile()) {
            System.err.println(f + " doesn't exist or is a directory");
        }
        try (InputStream fis = new BufferedInputStream(Files.newInputStream(f.toPath()));
                TarArchiveInputStream ais = args.length > 1 ? new TarArchiveInputStream(fis, args[1]) : new TarArchiveInputStream(fis)) {
            System.out.println("Created " + ais);
            TarArchiveEntry ae;
            while ((ae = ais.getNextTarEntry()) != null) {
                log(ae);
            }
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [encoding]");
    }

}
