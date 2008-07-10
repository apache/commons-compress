/**
 * 
 */
package org.apache.commons.compress.changes;

import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * @author Cy
 *
 */
interface Change {
	// public void perform(ArchiveInputStream input);
	public int type();
}
