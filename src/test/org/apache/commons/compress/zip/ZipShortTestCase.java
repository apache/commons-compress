/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons-sandbox//compress/src/test/org/apache/commons/compress/zip/ZipShortTestCase.java,v 1.2 2003/12/02 20:38:13 dirkv Exp $
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
 * JUnit 3 testcases for org.apache.tools.zip.ZipShort.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class ZipShortTestCase
    extends TestCase
{
    public ZipShortTestCase( String name )
    {
        super( name );
    }

    /**
     * Test conversion to bytes.
     */
    public void testToBytes()
    {
        final ZipShort zipShort = new ZipShort( 0x1234 );
        byte[] result = zipShort.getBytes();
        assertEquals( "length getBytes", 2, result.length );
        assertEquals( "first byte getBytes", 0x34, result[ 0 ] );
        assertEquals( "second byte getBytes", 0x12, result[ 1 ] );
    }

    /**
     * Test conversion from bytes.
     */
    public void testFromBytes()
    {
        byte[] val = new byte[]{0x34, 0x12};
        final ZipShort zipShort = new ZipShort( val );
        assertEquals( "value from bytes", 0x1234, zipShort.getValue() );
    }

    /**
     * Test the contract of the equals method.
     */
    public void testEquals()
    {
        final ZipShort zipShort = new ZipShort( 0x1234 );
        final ZipShort zipShort2 = new ZipShort( 0x1234 );
        final ZipShort zipShort3 = new ZipShort( 0x5678 );

        assertTrue( "reflexive", zipShort.equals( zipShort ) );

        assertTrue( "works", zipShort.equals( zipShort2 ) );
        assertTrue( "works, part two", !zipShort.equals( zipShort3 ) );

        assertTrue( "symmetric", zipShort2.equals( zipShort ) );

        assertTrue( "null handling", !zipShort.equals( null ) );
        assertTrue( "non ZipShort handling", !zipShort.equals( new Integer( 0x1234 ) ) );
    }

    /**
     * Test sign handling.
     */
    public void testSign()
    {
        final ZipShort zipShort = new ZipShort( new byte[]{(byte)0xFF, (byte)0xFF} );
        assertEquals( 0x0000FFFF, zipShort.getValue() );
    }
}
