/**
 * 
 */
package org.apache.commons.compress.compressors;

/**
 *
 */
public class CompressorException extends Exception {
	/* Serial */
	private static final long serialVersionUID = -2770299103090672278L;

	public CompressorException() {
		super();
	}

	public CompressorException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CompressorException(String arg0) {
		super(arg0);
	}

	public CompressorException(Throwable arg0) {
		super(arg0);
	}
}
