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
