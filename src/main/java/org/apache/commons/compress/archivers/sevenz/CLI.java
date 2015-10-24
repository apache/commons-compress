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
package org.apache.commons.compress.archivers.sevenz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CLI {

    private static final byte[] BUF = new byte[8192];

    private static enum Mode {
        LIST("Analysing") {
            @Override
            public void takeAction(SevenZFile archive, SevenZArchiveEntry entry) {
                System.out.print(entry.getName());
                if (entry.isDirectory()) {
                    System.out.print(" dir");
                } else {
                    System.out.print(" " + entry.getCompressedSize()
                                     + "/" + entry.getSize());
                }
                if (entry.getHasLastModifiedDate()) {
                    System.out.print(" " + entry.getLastModifiedDate());
                } else {
                    System.out.print(" no last modified date");
                }
                if (!entry.isDirectory()) {
                    System.out.println(" " + getContentMethods(entry));
                } else {
                    System.out.println("");
                }
            }

            private String getContentMethods(SevenZArchiveEntry entry) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (SevenZMethodConfiguration m : entry.getContentMethods()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(m.getMethod());
                    if (m.getOptions() != null) {
                        sb.append("(").append(m.getOptions()).append(")");
                    }
                }
                return sb.toString();
            }
        },
        EXTRACT("Extracting") {
            @Override
            public void takeAction(SevenZFile archive, SevenZArchiveEntry entry) 
                throws IOException {
                File outFile = new File(entry.getName());
                if (entry.isDirectory()) {
                    if (!outFile.isDirectory() && !outFile.mkdirs()) {
                        throw new IOException("Cannot create directory " + outFile);
                    }
                    System.out.println("created directory " + outFile);
                    return;
                }

                System.out.println("extracting to " + outFile);
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Cannot create " + parent);
                }
                FileOutputStream fos = new FileOutputStream(outFile);
                try {
                    final long total = entry.getSize();
                    long off = 0;
                    while (off < total) {
                        int toRead = (int) Math.min(total - off, BUF.length);
                        int bytesRead = archive.read(BUF, 0, toRead);
                        if (bytesRead < 1) {
                            throw new IOException("reached end of entry "
                                                  + entry.getName()
                                                  + " after " + off
                                                  + " bytes, expected "
                                                  + total);
                        }
                        off += bytesRead;
                        fos.write(BUF, 0, bytesRead);
                    }
                } finally {
                    fos.close();
                }
            }
        };

        private final String message;
        private Mode(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
        public abstract void takeAction(SevenZFile archive, SevenZArchiveEntry entry)
            throws IOException;
    }        

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        Mode mode = grabMode(args);
        System.out.println(mode.getMessage() + " " + args[0]);
        File f = new File(args[0]);
        if (!f.isFile()) {
            System.err.println(f + " doesn't exist or is a directory");
        }
        SevenZFile archive = new SevenZFile(f);
        try {
            SevenZArchiveEntry ae;
            while((ae=archive.getNextEntry()) != null) {
                mode.takeAction(archive, ae);
            }
        } finally {
            archive.close();
        }
    }

    private static void usage() {
        System.out.println("Parameters: archive-name [list|extract]");
    }

    private static Mode grabMode(String[] args) {
        if (args.length < 2) {
            return Mode.LIST;
        }
        return Enum.valueOf(Mode.class, args[1].toUpperCase());
    }

}
