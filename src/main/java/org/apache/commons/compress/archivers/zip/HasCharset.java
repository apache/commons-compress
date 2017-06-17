package org.apache.commons.compress.archivers.zip;

import java.nio.charset.Charset;

public interface HasCharset {

    /**
     *
     * @return the character set associated with this object
     */
    Charset getCharset();
}
