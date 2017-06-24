package org.apache.commons.compress.archivers.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

public class ZipClassCoverageTest {

    @Test
    public void testConstructZip64RequiredException() {
        Zip64RequiredException e = new Zip64RequiredException("critique of pure");
        assertNotNull(e);
    }
    @Test
    public void testMessageException() {
        ZipArchiveEntry ze = new ZipArchiveEntry("hello");
        String entryTooBigMessage = Zip64RequiredException.getEntryTooBigMessage(ze);
        assertEquals("hello's size exceeds the limit of 4GByte.",
            entryTooBigMessage);
    }

    @Test
    public void testConstantConstructor()
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<ZipConstants> clazz = ZipConstants.class;
        Constructor<ZipConstants> constructor = clazz.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        Object o = constructor.newInstance();
        assertThat(o, IsInstanceOf.instanceOf(clazz));
        constructor.setAccessible(false);

    }

}
