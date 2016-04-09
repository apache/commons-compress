package org.apache.commons.compress.archivers;

import static org.junit.Assert.*;
import org.junit.Test;

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
public class ExceptionMessageTest {

    private static final String ARCHIVER_NULL_MESSAGE = "Archivername must not be null.";

    private static final String INPUTSTREAM_NULL_MESSAGE = "InputStream must not be null.";

    private static final String OUTPUTSTREAM_NULL_MESSAGE = "OutputStream must not be null.";


    @Test
    public void testMessageWhenArchiverNameIsNull_1(){
        try{
            new ArchiveStreamFactory().createArchiveInputStream(null, System.in);
            fail("Should raise an IllegalArgumentException.");
        }catch (final IllegalArgumentException e) {
            assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
        } catch (final ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

    @Test
    public void testMessageWhenInputStreamIsNull(){
        try{
            new ArchiveStreamFactory().createArchiveInputStream("zip", null);
            fail("Should raise an IllegalArgumentException.");
        }catch (final IllegalArgumentException e) {
            assertEquals(INPUTSTREAM_NULL_MESSAGE, e.getMessage());
        } catch (final ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

    @Test
    public void testMessageWhenArchiverNameIsNull_2(){
        try{
            new ArchiveStreamFactory().createArchiveOutputStream(null, System.out);
            fail("Should raise an IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
            assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
        } catch (final ArchiveException e){
            fail("ArchiveException not expected");
        }
    }

    @Test
    public void testMessageWhenOutputStreamIsNull(){
        try{
            new ArchiveStreamFactory().createArchiveOutputStream("zip", null);
            fail("Should raise an IllegalArgumentException.");
        } catch (final IllegalArgumentException e) {
            assertEquals(OUTPUTSTREAM_NULL_MESSAGE, e.getMessage());
        } catch (final ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

}