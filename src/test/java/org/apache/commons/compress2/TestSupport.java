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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

public class TestSupport {
    public static File mkdir(String name) throws IOException {
        File f = File.createTempFile(name, "");
        f.delete();
        f.mkdir();
        return f;
    }

    public static File getFile(String path) throws IOException {
        URL url = TestSupport.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new FileNotFoundException("couldn't find " + path);
        }
        URI uri = null;
        try {
            uri = url.toURI();
        } catch (java.net.URISyntaxException ex) {
            throw new IOException(ex);
        }
        return new File(uri);
    }

    public static void rmdir(File f) {
        String[] s = f.list();
        if (s != null) {
            for (String element : s) {
                final File file = new File(f, element);
                if (file.isDirectory()){
                    rmdir(file);
                }
                boolean ok = tryHardToDelete(file);
                if (!ok && file.exists()){
                    System.out.println("Failed to delete "+element+" in "+f.getPath());
                }
            }
        }
        tryHardToDelete(f); // safer to delete and check
        if (f.exists()){
            throw new Error("Failed to delete "+f.getPath());
        }
    }

    private static final boolean ON_WINDOWS =
        System.getProperty("os.name").toLowerCase(Locale.ENGLISH)
        .indexOf("windows") > -1;

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     *
     * @return whether deletion was successful
     * @since Stolen from FileUtils in Ant 1.8.0
     */
    public static boolean tryHardToDelete(File f) {
        if (f != null && f.exists() && !f.delete()) {
            if (ON_WINDOWS) {
                System.gc();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            return f.delete();
        }
        return true;
    }
}
