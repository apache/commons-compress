/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons-sandbox//compress/src/test/org/apache/commons/compress/zip/ZipEntryTestCase.java,v 1.2 2003/12/02 20:38:13 dirkv Exp $
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

import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 * JUnit testcases ZipEntry.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class ZipEntryTestCase
    extends TestCase
{
    public ZipEntryTestCase( final String name )
    {
        super( name );
    }

    /**
     * test handling of extra fields
     */
    public void testExtraFields()
    {
        final AsiExtraField field = createField();
        final UnrecognizedExtraField extraField = createExtraField();

        final ZipEntry entry = new ZipEntry( "test/" );
        entry.setExtraFields( new ZipExtraField[]{field, extraField} );
        final byte[] data1 = entry.getExtra();
        ZipExtraField[] result = entry.getExtraFields();
        assertEquals( "first pass", 2, result.length );
        assertSame( field, result[ 0 ] );
        assertSame( extraField, result[ 1 ] );

        UnrecognizedExtraField u2 = new UnrecognizedExtraField();
        u2.setHeaderId( new ZipShort( 1 ) );
        u2.setLocalFileDataData( new byte[]{1} );

        entry.addExtraField( u2 );
        byte[] data2 = entry.getExtra();
        result = entry.getExtraFields();
        assertEquals( "second pass", 2, result.length );
        assertSame( field, result[ 0 ] );
        assertSame( u2, result[ 1 ] );
        assertEquals( "length second pass", data1.length + 1, data2.length );

        UnrecognizedExtraField u3 = new UnrecognizedExtraField();
        u3.setHeaderId( new ZipShort( 2 ) );
        u3.setLocalFileDataData( new byte[]{1} );
        entry.addExtraField( u3 );
        result = entry.getExtraFields();
        assertEquals( "third pass", 3, result.length );

        entry.removeExtraField( new ZipShort( 1 ) );
        byte[] data3 = entry.getExtra();
        result = entry.getExtraFields();
        assertEquals( "fourth pass", 2, result.length );
        assertSame( field, result[ 0 ] );
        assertSame( u3, result[ 1 ] );
        assertEquals( "length fourth pass", data2.length, data3.length );

        try
        {
            entry.removeExtraField( new ZipShort( 1 ) );
            fail( "should be no such element" );
        }
        catch( final NoSuchElementException nse )
        {
        }
    }

    private UnrecognizedExtraField createExtraField()
    {
        UnrecognizedExtraField extraField = new UnrecognizedExtraField();
        extraField.setHeaderId( new ZipShort( 1 ) );
        extraField.setLocalFileDataData( new byte[ 0 ] );
        return extraField;
    }

    private AsiExtraField createField()
    {
        final AsiExtraField field = new AsiExtraField();
        field.setDirectory( true );
        field.setMode( 0755 );
        return field;
    }
}
