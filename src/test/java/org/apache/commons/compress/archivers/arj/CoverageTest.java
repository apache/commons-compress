package org.apache.commons.compress.archivers.arj;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.compress.archivers.arj.ArjArchiveEntry.HostOs;
import org.junit.Test;

public class CoverageTest {

    @Test
    public void testHostOsInstance() {
        HostOs hostOs = new HostOs();
        assertNotNull(hostOs);
    }
    @Test
    public void testHeaderInstances() {
        assertNotNull(new LocalFileHeader.FileTypes());
        assertNotNull(new LocalFileHeader.Methods());
        assertNotNull(new LocalFileHeader.Flags());
        assertNotNull(new MainHeader.Flags());
    }
    @Test
    public void testCallLFHToString() {
        LocalFileHeader lfh = new LocalFileHeader();
        assertNotNull(lfh.toString());
    }

}
