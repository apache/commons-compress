/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.compress.bzip2.example;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.bzip2.CBZip2OutputStream;

/**
 * This simple example shows how to use the Bzip2 classes to compress a file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision: 1.2 $ $Date: 2004/02/27 03:41:37 $
 */
public class Bzip2Compress
{
    public static void main( final String[] args )
        throws Exception
    {
        if( 2 != args.length )
        {
            System.out.println( "java Bzip2Compress <input> <output>" );
            System.exit( 1 );
        }

        final File source = new File( args[ 0 ] );
        final File destination = new File( args[ 1 ] );
        final CBZip2OutputStream output =
            new CBZip2OutputStream( new FileOutputStream( destination ) );
        final FileInputStream input = new FileInputStream( source );
        copy( input, output );
        input.close();
        output.close();
    }

    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     */
    private static void copy( final InputStream input,
                              final OutputStream output )
        throws IOException
    {
        final byte[] buffer = new byte[ 8024 ];
        int n = 0;
        while( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
        }
    }
}
