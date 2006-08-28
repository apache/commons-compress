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

package org.apache.commons.compress.bzip2.example;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.bzip2.CBZip2InputStream;

/**
 * This simple example shows how to use the Bzip2 classes to uncompress a file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:nicolaken@apache.org">Nicola Ken Barozzi</a>
 * @version $Revision$ $Date$
 */
public class Bzip2Uncompress
{
    public static void main( final String[] args )
    {
      try
      {
        if( 2 != args.length )
        {
            System.out.println( "java Bzip2Uncompress <input> <output>" );
            System.exit( 1 );
        }
        final File source = new File( args[ 0 ] );
        final File destination = new File( args[ 1 ] );
        final FileOutputStream output =
            new FileOutputStream( destination );
        final CBZip2InputStream input = new CBZip2InputStream( new FileInputStream( source ) );
        copy( input, output );
        input.close();
        output.close();
      }catch(Exception e){
        e.printStackTrace();
        System.exit(1);       
      
      }
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
