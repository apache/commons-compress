/**
 * 
 */
package org.apache.commons.compress.changes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.memory.MemoryArchiveInputStream;
import org.apache.commons.compress.archivers.*;
/**
 * @author Cy
 *
 */
public class ChangeWorkerTest extends TestCase {

	final ArchiveInputStream is = null;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		final ArchiveInputStream is = new MemoryArchiveInputStream(new String[][] {
				{ "test1",      "" },
				{ "test2",      "" },
				{ "dir1/test1", "" },
				{ "dir1/test2", "" },
				{ "dir2/test1", "" },
				{ "dir2/test2", "" }
				});
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for {@link org.apache.commons.compress.changes.ChangeWorker#perform(org.apache.commons.compress.changes.ChangeSet, java.io.InputStream, java.io.OutputStream)}.
	 */
	public void testPerform() throws Exception {
		ChangeSet changes = new ChangeSet();
		changes.delete("test2.xml");
		
		final File input = new File(getClass().getClassLoader().getResource("bla.zip").getFile());
		final InputStream is = new FileInputStream(input);
		ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
		
		File temp = File.createTempFile("test", ".zip");
		ArchiveOutputStream out = new ArchiveStreamFactory().createArchiveOutputStream("zip", new FileOutputStream(temp));
		
		System.out.println(temp.getAbsolutePath());
		ChangeWorker.perform(changes, ais, out);
	}

}
