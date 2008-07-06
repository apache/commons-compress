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

package org.apache.commons.compress.compressors.bzip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.CompressUtils;
import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

/**
 * A test the stress tested the BZip implementation to verify
 * that it behaves correctly.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision: 280140 $ $Date$
 */
public class BzipTestCase extends TestCase
{
    private static final byte[] HEADER = new byte[]{(byte)'B', (byte)'Z'};

    public void testBzipOutputStream()
        throws Exception
    {
        final InputStream input = getInputStream( "asf-logo-huge.tar" );
        final File outputFile = getOutputFile( ".tar.bz2" );
        final OutputStream output = new FileOutputStream( outputFile );
        final BZip2OutputStream packedOutput = getPackedOutput( output );
        CompressUtils.copy( input, packedOutput );
        IOUtils.closeQuietly( input );
        IOUtils.closeQuietly( packedOutput );
        IOUtils.closeQuietly( output );
        compareContents( "asf-logo-huge.tar.bz2", outputFile );
        forceDelete( outputFile );
    }

    private void forceDelete( final File outputFile ) throws IOException
    {
        if( !outputFile.delete() )
        {
            final String message = "File " + outputFile + " unable to be deleted.";
            throw new IOException( message );
        }
    }

    public void testBzipInputStream()
        throws Exception
    {
        final InputStream input = getInputStream( "asf-logo-huge.tar.bz2" );
        final File outputFile = getOutputFile( ".tar" );
        final OutputStream output = new FileOutputStream( outputFile );
        final BZip2InputStream packedInput = getPackedInput( input );
        CompressUtils.copy( packedInput, output );
        IOUtils.closeQuietly( input );
        IOUtils.closeQuietly( packedInput );
        IOUtils.closeQuietly( output );
        compareContents( "asf-logo-huge.tar", outputFile );
        forceDelete( outputFile );
    }

    public void testCBZip2InputStreamClose()
        throws Exception
    {
        final InputStream input = getInputStream( "asf-logo-huge.tar.bz2" );
        final File outputFile = getOutputFile( ".tar.bz2" );
        final OutputStream output = new FileOutputStream( outputFile );
        CompressUtils.copy( input, output );
        IOUtils.closeQuietly( input );
        IOUtils.closeQuietly( output );
        assertTrue( "Check output file exists." , outputFile.exists() );
        final InputStream input2 = new FileInputStream( outputFile );
        final InputStream packedInput = getPackedInput( input2 );
        IOUtils.closeQuietly( packedInput );
        try
        {
            input2.read();
            assertTrue("Source input stream is still opened.", false);
        } catch ( Exception e )
        {
            // Read closed stream.
        }
        forceDelete( outputFile );
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     */
//    private void copy( final InputStream input,
//                       final OutputStream output )
//        throws IOException
//    {
//        final byte[] buffer = new byte[ 8024 ];
//        int n = 0;
//        while( -1 != ( n = input.read( buffer ) ) )
//        {
//            output.write( buffer, 0, n );
//        }
//    }

    private void compareContents( final String initial, final File generated )
        throws Exception
    {
        final InputStream input1 = getInputStream( initial );
        final InputStream input2 = new FileInputStream( generated );
        final boolean test = contentEquals( input1, input2 );
        IOUtils.closeQuietly( input1 );
        IOUtils.closeQuietly( input2 );
        assertTrue( "Contents of " + initial + " matches generated version " + generated, test );
    }

    private BZip2InputStream getPackedInput( final InputStream input )
        throws IOException
    {
        final int b1 = input.read();
        final int b2 = input.read();
        assertEquals( "Equal header byte1", b1, 'B' );
        assertEquals( "Equal header byte2", b2, 'Z' );
        return new BZip2InputStream( input );
    }

    private BZip2OutputStream getPackedOutput( final OutputStream output )
        throws IOException
    {
        output.write( HEADER );
        return new BZip2OutputStream( output );
    }

    private File getOutputFile( final String postfix )
        throws IOException
    {
        final File cwd = new File( "." );
        return File.createTempFile( "ant-test", postfix, cwd );
    }

    private InputStream getInputStream( final String resource )
        throws Exception
    {
        final String filename =
            "src" + File.separator + "test" + File.separator +
            getClass().getName().replace( '.', File.separatorChar );
        final String path = getPath( filename );
        final File input = new File( path, resource );
        return new FileInputStream( input );
//        final ClassLoader loader = getClass().getClassLoader();
//        return loader.getResourceAsStream( resource );
    }

    /**
     * Compare the contents of two Streams to determine if they are equal or not.
     *
     * @param input1 the first stream
     * @param input2 the second stream
     * @return true if the content of the streams are equal or they both don't exist, false otherwise
     */
    private boolean contentEquals( final InputStream input1,
                                   final InputStream input2 )
        throws IOException
    {
        final InputStream bufferedInput1 = new BufferedInputStream( input1 );
        final InputStream bufferedInput2 = new BufferedInputStream( input2 );

        int ch = bufferedInput1.read();
        while( -1 != ch )
        {
            final int ch2 = bufferedInput2.read();
            if( ch != ch2 )
            {
                return false;
            }
            ch = bufferedInput1.read();
        }

        final int ch2 = bufferedInput2.read();
        if( -1 != ch2 )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private String getPath( final String filepath )
    {
        final int index = filepath.lastIndexOf( File.separatorChar );
        if( -1 == index )
        {
            return "";
        }
        else
        {
            return filepath.substring( 0, index );
        }
    }
}
