/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons-sandbox//compress/src/test/org/apache/commons/compress/zip/AsiExtraFieldTestCase.java,v 1.2 2003/12/02 20:38:13 dirkv Exp $
 * $Revision: 1.2 $
 * $Date: 2003/12/02 20:38:13 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.compress.zip;

import java.util.zip.ZipException;
import junit.framework.TestCase;

/**
 * JUnit testcases AsiExtraField.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class AsiExtraFieldTestCase
    extends TestCase
    implements UnixStat
{
    public AsiExtraFieldTestCase( final String name )
    {
        super( name );
    }

    /**
     * Test file mode magic.
     */
    public void testModes()
    {
        final AsiExtraField field = new AsiExtraField();
        field.setMode( 0123 );
        assertEquals( "plain file", 0100123, field.getMode() );
        field.setDirectory( true );
        assertEquals( "directory", 040123, field.getMode() );
        field.setLinkedFile( "test" );
        assertEquals( "symbolic link", 0120123, field.getMode() );
    }

    private AsiExtraField createField()
    {
        final AsiExtraField field = new AsiExtraField();
        field.setMode( 0123 );
        field.setUserId( 5 );
        field.setGroupId( 6 );
        return field;
    }

    public void testContent1()
    {
        final AsiExtraField field = createField();
        final byte[] data = field.getLocalFileDataData();

        // CRC manually calculated, sorry
        final byte[] expect = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                               0123, (byte)0x80, // mode
                               0, 0, 0, 0, // link length
                               5, 0, 6, 0};                        // uid, gid
        assertEquals( "no link", expect.length, data.length );
        for( int i = 0; i < expect.length; i++ )
        {
            assertEquals( "no link, byte " + i, expect[ i ], data[ i ] );
        }

        field.setLinkedFile( "test" );
    }

    public void testContent2()
    {
        final AsiExtraField field = createField();
        field.setLinkedFile( "test" );

        final byte[] data = field.getLocalFileDataData();
        final byte[] expect = new byte[]{0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                                         0123, (byte)0xA0, // mode
                                         4, 0, 0, 0, // link length
                                         5, 0, 6, 0, // uid, gid
                                         (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        assertEquals( "no link", expect.length, data.length );
        for( int i = 0; i < expect.length; i++ )
        {
            assertEquals( "no link, byte " + i, expect[ i ], data[ i ] );
        }

    }

    public void testReparse1()
        throws ZipException
    {
        // CRC manually calculated, sorry
        final byte[] data = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                             0123, (byte)0x80, // mode
                             0, 0, 0, 0, // link length
                             5, 0, 6, 0};                        // uid, gid
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );

        assertEquals( "length plain file", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "plain file, no link", !field.isLink() );
        assertTrue( "plain file, no dir", !field.isDirectory() );
        assertEquals( "mode plain file", FILE_FLAG | 0123, field.getMode() );
        assertEquals( "uid plain file", 5, field.getUserId() );
        assertEquals( "gid plain file", 6, field.getGroupID() );
    }

    public void testReparse2()
        throws ZipException
    {
        final byte[] data = new byte[]{0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                                       0123, (byte)0xA0, // mode
                                       4, 0, 0, 0, // link length
                                       5, 0, 6, 0, // uid, gid
                                       (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );
        assertEquals( "length link", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "link, is link", field.isLink() );
        assertTrue( "link, no dir", !field.isDirectory() );
        assertEquals( "mode link", LINK_FLAG | 0123, field.getMode() );
        assertEquals( "uid link", 5, field.getUserId() );
        assertEquals( "gid link", 6, field.getGroupID() );
        assertEquals( "test", field.getLinkedFile() );
    }

    public void testReparse3()
        throws ZipException
    {
        final byte[] data = new byte[]{(byte)0x8E, 0x01, (byte)0xBF, (byte)0x0E, // CRC
                                       0123, (byte)0x40, // mode
                                       0, 0, 0, 0, // link
                                       5, 0, 6, 0};                          // uid, gid
        final AsiExtraField field = new AsiExtraField();
        field.parseFromLocalFileData( data, 0, data.length );
        assertEquals( "length dir", data.length,
                      field.getLocalFileDataLength().getValue() );
        assertTrue( "dir, no link", !field.isLink() );
        assertTrue( "dir, is dir", field.isDirectory() );
        assertEquals( "mode dir", DIR_FLAG | 0123, field.getMode() );
        assertEquals( "uid dir", 5, field.getUserId() );
        assertEquals( "gid dir", 6, field.getGroupID() );
    }

    public void testReparse4()
        throws Exception
    {
        final byte[] data = new byte[]{0, 0, 0, 0, // bad CRC
                                       0123, (byte)0x40, // mode
                                       0, 0, 0, 0, // link
                                       5, 0, 6, 0};                          // uid, gid
        final AsiExtraField field = new AsiExtraField();
        try
        {
            field.parseFromLocalFileData( data, 0, data.length );
            fail( "should raise bad CRC exception" );
        }
        catch( Exception e )
        {
            assertEquals( "bad CRC checksum 0 instead of ebf018e",
                          e.getMessage() );
        }
    }
}
