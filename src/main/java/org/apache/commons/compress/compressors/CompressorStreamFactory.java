package org.apache.commons.compress.compressors;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class CompressorStreamFactory {
	final Map inputStreamClasses = new HashMap();
	final Map outputStreamClasses = new HashMap();
	
	public CompressorStreamFactory() throws CompressorException {
		registerInputStream("gz", GzipCompressorInputStream.class);
		registerOutputStream("gz", GzipCompressorOutputStream.class);
		registerInputStream("bzip2", BZip2CompressorInputStream.class);
		registerOutputStream("bzip2", BZip2CompressorOutputStream.class);
		
	}
	
	public void registerInputStream( final String name, final Class stream ) throws CompressorException {
		if (CompressorInputStream.class.isAssignableFrom(stream) && !(stream.isInterface())) {
			inputStreamClasses.put(name, stream);
        } else {
            throw new CompressorException("Compressor does not implement the CompressorInputStream interface.");
        }	
	}

	public void registerOutputStream( final String name, final Class stream ) throws CompressorException {
		if (CompressorOutputStream.class.isAssignableFrom(stream) && !(stream.isInterface())) {
			outputStreamClasses.put(name, stream);
        } else {
            throw new CompressorException("Compressor does not implement the CompressorOutputStream interface.");
        }
	}
	
	public CompressorInputStream createCompressorInputStream( final String name, final InputStream out ) throws CompressorException {
        try {
            final Class clazz = (Class) inputStreamClasses.get(name);

            if (clazz == null) {
            	throw new CompressorException("CompressorFactory could not create instance");
            }

            final Class[] params = { InputStream.class };
            final Constructor constructor = clazz.getConstructor(params);
            final Object[] initargs = { out };
            return (CompressorInputStream) constructor.newInstance(initargs);
        } catch (InstantiationException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        } catch (IllegalAccessException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        } catch (SecurityException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        } catch (NoSuchMethodException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        } catch (IllegalArgumentException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        } catch (InvocationTargetException e) {
            throw new CompressorException("CompressorFactory could not create instance", e);
        }
    }

    public CompressorOutputStream createCompressorOutputStream( final String name, final OutputStream out ) throws ArchiveException {
        try {
            final Class clazz = (Class) outputStreamClasses.get(name);
            
            if (clazz == null) {
            	throw new ArchiveException("CompressorFactory could not create instance");
            }
            
            final Class[] params = { OutputStream.class };
            final Constructor constructor = clazz.getConstructor(params);
            final Object[] initargs = { out };
            return (CompressorOutputStream) constructor.newInstance(initargs);
        } catch (InstantiationException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        } catch (IllegalAccessException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        } catch (SecurityException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        } catch (NoSuchMethodException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        } catch (IllegalArgumentException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        } catch (InvocationTargetException e) {
            throw new ArchiveException("CompressorFactory could not create instance", e);
        }
    }
}
