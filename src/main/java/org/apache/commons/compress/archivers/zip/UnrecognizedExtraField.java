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
 * Simple placeholder for all those extra fields we don't want to deal with. <p>
 *
 * Assumes local file data and central directory entries are identical - unless
 * told the opposite.</p>
 */
public class UnrecognizedExtraField
    implements ZipExtraField
{
    /**
     * Extra field data in central directory - without Header-ID or length
     * specifier.
     *
     * @since 1.1
     */
    private byte[] m_centralData;

    /**
     * The Header-ID.
     *
     * @since 1.1
     */
    private ZipShort m_headerID;

    /**
     * Extra field data in local file data - without Header-ID or length
     * specifier.
     *
     * @since 1.1
     */
    private byte[] m_localData;

    /**
     * Set the central directory data
     *
     * @param centralData the central directory data
     */
    public void setCentralDirectoryData( final byte[] centralData )
    {
        m_centralData = centralData;
    }

       /**
     * Set the header ID.
     *
     * @param headerID the header ID
     */
    public void setHeaderID( final ZipShort headerID )
    {
        m_headerID = headerID;
    }

    /**
     * Set the local file data.
     *
     * @param localData the local file data
     */
    public void setLocalFileDataData( final byte[] localData )
    {
        m_localData = localData;
    }

    /**
     * Get the central directory data.
     *
     * @return the central directory data.
     */
    public byte[] getCentralDirectoryData()
    {
        if( m_centralData != null )
        {
            return m_centralData;
        }
        return getLocalFileDataData();
    }

    /**
     * Get the length of the central directory in bytes.
     *
     * @return the length of the central directory in bytes.
     */
    public ZipShort getCentralDirectoryLength()
    {
        if( m_centralData != null )
        {
            return new ZipShort( m_centralData.length );
        }
        return getLocalFileDataLength();
    }

    /**
     * Get the HeaderID.
     *
     * @return the HeaderID
     */
    public ZipShort getHeaderID()
    {
        return m_headerID;
    }

    /**
     * Get the local file data.
     *
     * @return the local file data
     */
    public byte[] getLocalFileDataData()
    {
        return m_localData;
    }

    /**
     * Get the length of local file data in bytes.
     *
     * @return the length of local file data in bytes
     */
    public ZipShort getLocalFileDataLength()
    {
        return new ZipShort( m_localData.length );
    }

    /**
     * Parse LocalFiledata out of supplied buffer.
     *
     * @param buffer the buffer to use
     * @param offset the offset into buffer
     * @param length then length of data
     */
    public void parseFromLocalFileData( final byte[] buffer,
                                        final int offset,
                                        final int length )
    {
        final byte[] fileData = new byte[ length ];
        System.arraycopy( buffer, offset, fileData, 0, length );
        setLocalFileDataData( fileData );
    }
}
