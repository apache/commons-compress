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

	protected File getFile( String path ) {
		return new File(getClass().getClassLoader().getResource(path).getFile());		
	}
	
	protected void tearDown() throws Exception {
		dir.delete();
		dir = null;
	}


}
