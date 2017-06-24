package org.apache.commons.compress.archivers.sevenz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class CoverageTest {

    @Test public void testNidInstance() {
        assertNotNull(new NID());
    }

    @Test public void testCLIInstance() {
        CLI foo = new CLI();
        assertNotNull(foo);
        try {
            CLI.main(new String[]{"/dev/null/not-there"});
            fail("shouldn't be able to list contents of  a file that isn't there");
        } catch (Exception ignored) {

        }
    }
}
