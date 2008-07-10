package org.apache.commons.compress.archivers.memory;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.compress.archivers.ArchiveEntry;

public final class MemoryArchiveTestCase extends TestCase {

	public void testReading() throws IOException {
		
		final MemoryArchiveInputStream is = new MemoryArchiveInputStream(new String[][] {
				{ "test1",     "content1" },
				{ "test2",     "content2" },
				});

		final ArchiveEntry entry1 = is.getNextEntry();
		assertNotNull(entry1);
		assertEquals("test1", entry1.getName());
		final String content1 = is.readString();
		assertEquals("content1", content1);
		
		final ArchiveEntry entry2 = is.getNextEntry();
		assertNotNull(entry2);
		assertEquals("test2", entry2.getName());
		final String content2 = is.readString();
		assertEquals("content2", content2);
		
		final ArchiveEntry entry3 = is.getNextEntry();
		assertNull(entry3);
		
	}

}
