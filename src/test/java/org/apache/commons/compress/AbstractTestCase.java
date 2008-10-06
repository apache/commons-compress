package org.apache.commons.compress;

import java.io.File;

import junit.framework.TestCase;

public abstract class AbstractTestCase extends TestCase {

	protected File dir;
	
	protected void setUp() throws Exception {
		dir = File.createTempFile("dir", "");
		dir.delete();
		dir.mkdir();
	}

	protected void tearDown() throws Exception {
		dir.delete();
		dir = null;
	}


}
