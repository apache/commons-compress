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
package org.apache.commons.compress.archivers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.Test;

public class ArchiveStreamFactoryTest {

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-171
     */
    @Test
    public void shortTextFilesAreNoTARs() throws Exception {
        try {
            new ArchiveStreamFactory()
                .createArchiveInputStream(new ByteArrayInputStream("This certainly is not a tar archive, really, no kidding".getBytes()));
            fail("created an input stream for a non-archive");
        } catch (ArchiveException ae) {
            assertTrue(ae.getMessage().startsWith("No Archiver found"));
        }
    }

    /**
     * see https://issues.apache.org/jira/browse/COMPRESS-191
     */
    @Test
    public void aiffFilesAreNoTARs() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/testAIFF.aif");
    	try {
            InputStream is = new BufferedInputStream(fis);
            try {
                new ArchiveStreamFactory().createArchiveInputStream(is);
                fail("created an input stream for a non-archive");
            } catch (ArchiveException ae) {
                assertTrue(ae.getMessage().startsWith("No Archiver found"));
            } finally {
                is.close();
            }
    	} finally {
            fis.close();
    	}
    }

    @Test
    public void testCOMPRESS209() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/testCompress209.doc");
    	try {
            InputStream bis = new BufferedInputStream(fis);
            try {
                new ArchiveStreamFactory().createArchiveInputStream(bis);
                fail("created an input stream for a non-archive");
            } catch (ArchiveException ae) {
                assertTrue(ae.getMessage().startsWith("No Archiver found"));
            } finally {
                bis.close();
            }
    	} finally {
            fis.close();
    	}
    }

    /**
     * Test case for 
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-208"
     * >COMPRESS-208</a>.
     */
    @Test
    public void skipsPK00Prefix() throws Exception {
    	FileInputStream fis = new FileInputStream("src/test/resources/COMPRESS-208.zip");
    	try {
            InputStream bis = new BufferedInputStream(fis);
            try {
                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
                try {
                    assertTrue(ais instanceof ZipArchiveInputStream);
                } finally {
                    ais.close();
                }
            } finally {
                bis.close();
            }
    	} finally {
            fis.close();
    	}
    }
}
