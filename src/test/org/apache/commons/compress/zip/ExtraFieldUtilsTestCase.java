/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons-sandbox//compress/src/test/org/apache/commons/compress/zip/ExtraFieldUtilsTestCase.java,v 1.2 2003/12/02 20:38:13 dirkv Exp $
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

import junit.framework.TestCase;

/**
 * JUnit testcases ExtraFieldUtils.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class ExtraFieldUtilsTestCase
    extends TestCase
    implements UnixStat
{
    private AsiExtraField m_field;
    private UnrecognizedExtraField m_dummy;
    private byte[] m_data;
    private byte[] m_local;

    public ExtraFieldUtilsTestCase( final String name )
    {
        super( name );
    }

    public void setUp()
    {
        m_field = new AsiExtraField();
        m_field.setMode( 0755 );
        m_field.setDirectory( true );
        m_dummy = new UnrecognizedExtraField();
        m_dummy.setHeaderId( new ZipShort( 1 ) );
        m_dummy.setLocalFileDataData( new byte[ 0 ] );
        m_dummy.setCentralDirectoryData( new byte[]{0} );

        m_local = m_field.getLocalFileDataData();
        final byte[] dummyLocal = m_dummy.getLocalFileDataData();
        m_data = new byte[ 4 + m_local.length + 4 + dummyLocal.length ];
        System.arraycopy( m_field.getHeaderID().getBytes(), 0, m_data, 0, 2 );
        System.arraycopy( m_field.getLocalFileDataLength().getBytes(), 0, m_data, 2, 2 );
        System.arraycopy( m_local, 0, m_data, 4, m_local.length );
        System.arraycopy( m_dummy.getHeaderID().getBytes(), 0, m_data,
                          4 + m_local.length, 2 );
        System.arraycopy( m_dummy.getLocalFileDataLength().getBytes(), 0, m_data,
                          4 + m_local.length + 2, 2 );
        System.arraycopy( dummyLocal, 0, m_data,
                          4 + m_local.length + 4, dummyLocal.length );

    }

    /**
     * test parser.
     */
    public void testParse() throws Exception
    {
        final ZipExtraField[] extraField = ExtraFieldUtils.parse( m_data );
        assertEquals( "number of fields", 2, extraField.length );
        assertTrue( "type field 1", extraField[ 0 ] instanceof AsiExtraField );
        assertEquals( "mode field 1", 040755,
                      ( (AsiExtraField)extraField[ 0 ] ).getMode() );
        assertTrue( "type field 2", extraField[ 1 ] instanceof UnrecognizedExtraField );
        assertEquals( "data length field 2", 0,
                      extraField[ 1 ].getLocalFileDataLength().getValue() );

        final byte[] data2 = new byte[ m_data.length - 1 ];
        System.arraycopy( m_data, 0, data2, 0, data2.length );
        try
        {
            ExtraFieldUtils.parse( data2 );
            fail( "data should be invalid" );
        }
        catch( Exception e )
        {
            assertEquals( "message",
                          "data starting at " + ( 4 + m_local.length ) + " is in unknown format",
                          e.getMessage() );
        }
    }

    /**
     * Test merge methods
     */
    public void testMerge()
    {
        final byte[] local =
            ExtraFieldUtils.mergeLocalFileDataData( new ZipExtraField[]{m_field, m_dummy} );
        assertEquals( "local length", m_data.length, local.length );
        for( int i = 0; i < local.length; i++ )
        {
            assertEquals( "local byte " + i, m_data[ i ], local[ i ] );
        }

        final byte[] dummyCentral = m_dummy.getCentralDirectoryData();
        final byte[] data2 = new byte[ 4 + m_local.length + 4 + dummyCentral.length ];
        System.arraycopy( m_data, 0, data2, 0, 4 + m_local.length + 2 );
        System.arraycopy( m_dummy.getCentralDirectoryLength().getBytes(), 0,
                          data2, 4 + m_local.length + 2, 2 );
        System.arraycopy( dummyCentral, 0, data2,
                          4 + m_local.length + 4, dummyCentral.length );

        final byte[] central =
            ExtraFieldUtils.mergeCentralDirectoryData( new ZipExtraField[]{m_field, m_dummy} );
        assertEquals( "central length", data2.length, central.length );
        for( int i = 0; i < central.length; i++ )
        {
            assertEquals( "central byte " + i, data2[ i ], central[ i ] );
        }
    }
}
