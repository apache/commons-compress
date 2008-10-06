/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.ReflectionUtils;

public class ArchiveStreamFactory {

	final Map inputStreamClasses = new HashMap();
	final Map outputStreamClasses = new HashMap();
	
	public ArchiveStreamFactory() throws ArchiveException {
		registerArchiveInputStream("zip", ZipArchiveInputStream.class);
		registerArchiveOutputStream("zip", ZipArchiveOutputStream.class);

        registerArchiveInputStream("tar", TarArchiveInputStream.class);
        registerArchiveOutputStream("tar", TarArchiveOutputStream.class);

        registerArchiveInputStream("ar", ArArchiveInputStream.class);
        registerArchiveOutputStream("ar", ArArchiveOutputStream.class);

        registerArchiveInputStream("jar", JarArchiveInputStream.class);		
        registerArchiveOutputStream("jar", JarArchiveOutputStream.class);
	}
	
	
	public void registerArchiveInputStream( final String name, final Class stream ) throws ArchiveException {
		if (ArchiveInputStream.class.isAssignableFrom(stream) && !(stream.isInterface())) {
			inputStreamClasses.put(name, stream);
        } else {
            throw new ArchiveException("Archive does not implement the ArchiveInputStream interface.");
        }	
	}

	public void registerArchiveOutputStream( final String name, final Class stream ) throws ArchiveException {
		ReflectionUtils.registerClazz(outputStreamClasses, name, ArchiveOutputStream.class, stream);		
		if (ArchiveOutputStream.class.isAssignableFrom(stream) && !(stream.isInterface())) {
			outputStreamClasses.put(name, stream);
        } else {
            throw new ArchiveException("Archive does not implement the ArchiveOutputStream interface.");
        }
	}
	
    public ArchiveInputStream createArchiveInputStream( final String archiverName, final InputStream out ) throws ArchiveException {
        try {
            final Class clazz = (Class) inputStreamClasses.get(archiverName);

            if (clazz == null) {
            	throw new ArchiveException("ArchiverFactory could not create instance");
            }

            final Class[] params = { InputStream.class };
            final Constructor constructor = clazz.getConstructor(params);
            final Object[] initargs = { out };
            return (ArchiveInputStream) constructor.newInstance(initargs);
        } catch (InstantiationException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (IllegalAccessException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (SecurityException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (NoSuchMethodException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (IllegalArgumentException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (InvocationTargetException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        }
    }

    public ArchiveOutputStream createArchiveOutputStream( final String archiverName, final OutputStream out ) throws ArchiveException {
        try {
            final Class clazz = (Class) outputStreamClasses.get(archiverName);
            
            if (clazz == null) {
            	throw new ArchiveException("ArchiverFactory could not create instance");
            }
            
            final Class[] params = { OutputStream.class };
            final Constructor constructor = clazz.getConstructor(params);
            final Object[] initargs = { out };
            return (ArchiveOutputStream) constructor.newInstance(initargs);
        } catch (InstantiationException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (IllegalAccessException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (SecurityException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (NoSuchMethodException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (IllegalArgumentException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        } catch (InvocationTargetException e) {
            throw new ArchiveException("ArchiverFactory could not create instance", e);
        }
    }

    public ArchiveInputStream createArchiveInputStream( final InputStream input ) throws IOException {

		final byte[] signature = new byte[12];
		input.mark(signature.length);
		input.read(signature);
		// reset not supported exception?
		input.reset();

//		for (int i = 0; i < signature.length; i++) {
//			System.out.print(Integer.toHexString(signature[i]));
//			System.out.print(",");
//		}
//		System.out.println("");
		
		for (Iterator it = inputStreamClasses.values().iterator(); it.hasNext();) {
			final Class clazz = (Class) it.next();
			try {
				final Method method = clazz.getMethod("matches", new Class[] { byte[].class });
				
				final Object result = method.invoke(null, new Object[] { signature } );
				
				if (result.equals(Boolean.TRUE)) {
		            final Class[] params = { InputStream.class };
		            final Constructor constructor = clazz.getConstructor(params);
		            final Object[] initargs = { input };
		            return (ArchiveInputStream) constructor.newInstance(initargs);					
				}
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			} catch (InstantiationException e) {
			}
		}
		return null;
	}
}
