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
 * Utility class that represents a four byte integer with conversion rules for
 * the big endian byte order of ZIP files.
 */
public final class ZipLong implements Cloneable
{
    private long m_value;

    /**
     * Create instance from a number.
     *
     * @param value the value
     * @since 1.1
     */
    public ZipLong( final long value )
    {
        m_value = value;
    }

    /**
     * Create instance from bytes.
     *
     * @param buffer the buffer to read data from
     * @since 1.1
     */
    public ZipLong( final byte[] buffer )
    {
        this( buffer, 0 );
    }

    /**
     * Create instance from the four bytes starting at offset.
     *
     * @param buffer buffer to read data from
     * @param offset offset into buffer
     * @since 1.1
     */
    public ZipLong( final byte[] buffer, final int offset )
    {
        m_value = ( buffer[ offset + 3 ] << 24 ) & 0xFF000000l;
        m_value += ( buffer[ offset + 2 ] << 16 ) & 0xFF0000;
        m_value += ( buffer[ offset + 1 ] << 8 ) & 0xFF00;
        m_value += ( buffer[ offset ] & 0xFF );
    }

    /**
     * Get value as two bytes in big endian byte order.
     *
     * @return The value as bytes
     * @since 1.1
     */
    public byte[] getBytes()
    {
        byte[] result = new byte[ 4 ];
        result[ 0 ] = (byte)( ( m_value & 0xFF ) );
        result[ 1 ] = (byte)( ( m_value & 0xFF00 ) >> 8 );
        result[ 2 ] = (byte)( ( m_value & 0xFF0000 ) >> 16 );
        result[ 3 ] = (byte)( ( m_value & 0xFF000000l ) >> 24 );
        return result;
    }

    /**
     * Get value as Java int.
     *
     * @return The value
     * @since 1.1
     */
    public long getValue()
    {
        return m_value;
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @param o the object to compare against
     * @return true if equyal, false otherwise
     * @since 1.1
     */
    public boolean equals( final Object o )
    {
        if( o == null || !( o instanceof ZipLong ) )
        {
            return false;
        }
        return m_value == ( (ZipLong)o ).getValue();
    }

    /**
     * Override to make two instances with same value equal.
     *
     * @return the hashcode
     * @since 1.1
     */
    public int hashCode()
    {
        return (int)m_value;
    }
}
