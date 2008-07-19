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
package org.apache.commons.compress.archivers.zip;

/**
 * Utility class that represents a two byte integer with conversion rules for
 * the big endian byte order of ZIP files.
 */
public final class ZipShort implements Cloneable
{
    private int m_value;

    /**
     * Create instance from a number.
     *
     * @param value Description of Parameter
     * @since 1.1
     */
    public ZipShort( int value )
    {
        this.m_value = value;
    }

    /**
     * Create instance from bytes.
     *
     * @param bytes Description of Parameter
     * @since 1.1
     */
    public ZipShort( byte[] bytes )
    {
        this( bytes, 0 );
    }

    /**
     * Create instance from the two bytes starting at offset.
     *
     * @param bytes Description of Parameter
     * @param offset Description of Parameter
     * @since 1.1
     */
    public ZipShort( byte[] bytes, int offset )
    {
        m_value = ( bytes[ offset + 1 ] << 8 ) & 0xFF00;
        m_value += ( bytes[ offset ] & 0xFF );
    }

    /**
     * Get value as two bytes in big endian byte order.
     *
     * @return The Bytes value
     * @since 1.1
     */
    public byte[] getBytes()
    {
        byte[] result = new byte[ 2 ];
        result[ 0 ] = (byte)( m_value & 0xFF );
        result[ 1 ] = (byte)( ( m_value & 0xFF00 ) >> 8 );
        return result;
    }

    /**
     * Get value as Java int.
     *
     * @return The Value value
     * @since 1.1
     */
    public int getValue()
    {
        return m_value;
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @param o Description of Parameter
     * @return Description of the Returned Value
     * @since 1.1
     */
    public boolean equals( Object o )
    {
        if( o == null || !( o instanceof ZipShort ) )
        {
            return false;
        }
        return m_value == ( (ZipShort)o ).getValue();
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @return Description of the Returned Value
     * @since 1.1
     */
    public int hashCode()
    {
        return m_value;
    }
}
