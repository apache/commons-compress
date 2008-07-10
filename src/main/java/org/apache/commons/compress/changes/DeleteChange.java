/**
 * 
 */
package org.apache.commons.compress.changes;

import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * Implementation for a delete operation
 */
class DeleteChange implements Change {
	private String filename = null;
	
	/**
	 * Constructor. Takes the filename of the file to be deleted
	 * from the stream as argument.
	 * @param pFilename the filename of the file to delete
	 */
	public DeleteChange(final String pFilename) {
		if(pFilename == null) {
			throw new NullPointerException();
		}
		filename = pFilename;
	}
	
	public void perform(ArchiveInputStream input) {
		System.out.println("PERFORMING DELETE");
	}

	public String targetFile() {
		return filename;
	}
	
	public int type() {
		return ChangeSet.CHANGE_TYPE_DELETE;
	}
}
