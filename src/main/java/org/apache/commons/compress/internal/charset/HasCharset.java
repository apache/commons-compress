package org.apache.commons.compress.internal.charset;

import java.nio.charset.Charset;

/**
 * Interface to allow access to a character set associated with an object
 */
public interface HasCharset {
  Charset getCharset();
}
