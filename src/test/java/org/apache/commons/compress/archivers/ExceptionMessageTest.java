package org.apache.commons.compress.archivers;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ExceptionMessageTest extends TestCase {

    private static final String ARCHIVER_NULL_MESSAGE = "Archivername must not be null.";
    
    private static final String INPUTSTREAM_NULL_MESSAGE = "InputStream must not be null.";
    
    private static final String OUTPUTSTREAM_NULL_MESSAGE = "OutputStream must not be null.";


    public void testMessageWhenArchiverNameIsNull_1(){
        try{
            new ArchiveStreamFactory().createArchiveInputStream(null, System.in);
            fail("Should raise an IllegalArgumentException.");
        }catch (IllegalArgumentException e) {
            Assert.assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
        } catch (ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

    public void testMessageWhenInputStreamIsNull(){
        try{
            new ArchiveStreamFactory().createArchiveInputStream("zip", null);
            fail("Should raise an IllegalArgumentException.");
        }catch (IllegalArgumentException e) {
            Assert.assertEquals(INPUTSTREAM_NULL_MESSAGE, e.getMessage());
        } catch (ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

    public void testMessageWhenArchiverNameIsNull_2(){
        try{
            new ArchiveStreamFactory().createArchiveOutputStream(null, System.out);
            fail("Should raise an IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(ARCHIVER_NULL_MESSAGE, e.getMessage());
        } catch (ArchiveException e){
            fail("ArchiveException not expected");
        }
    }

    public void testMessageWhenOutputStreamIsNull(){
        try{
            new ArchiveStreamFactory().createArchiveOutputStream("zip", null);
            fail("Should raise an IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(OUTPUTSTREAM_NULL_MESSAGE, e.getMessage());
        } catch (ArchiveException e) {
            fail("ArchiveException not expected");
        }
    }

}