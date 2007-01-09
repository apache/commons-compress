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
package org.apache.commons.compress;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchive;
import org.apache.commons.compress.archivers.zip.ZipArchive;


/**
 * Provides implementations for all ArchiverFactory methods.
 */
public class ArchiverFactory {
	/* internal archiver list */
	private static List archives;
	
	// pre-register archives 
	static {
		archives = new ArrayList();
		try {
			registerArchiver(TarArchive.class);
			registerArchiver(ZipArchive.class);
		} catch (ArchiveException e) {
			e.printStackTrace();
		}
	}
	
	private ArchiverFactory() { 
	}

	/**
	 * Registers a new archiver in the factory.
	 * The archiver must implement the archiver interface.
	 * 
	 * @param className full qualified archiver implementation
	 * @throws ClassNotFoundException if the new archiver class could not be found
	 * @throws ArchiveException if the new archiver does not implement the archiver interface
	 */
	public static void registerArchiver(String className) 
		throws ClassNotFoundException, ArchiveException {
		Class clazz = Class.forName(className);
		registerArchiver(clazz);
	}
	
	/**
	 * Registers a new archiver in the factory.
	 * The archiver must implement the archiver interface and must
	 * be an concrete implementation
	 * 
	 * @param className full qualified archiver implementation
	 * @throws ArchiveException if the new archiver does not implement the archiver interface
	 */
	public static void registerArchiver(Class clazz) 
		throws ArchiveException {
		// register only, if the class is assignable and not an interface
		if(Archive.class.isAssignableFrom(clazz) && !(clazz.isInterface())) {
			try {
				archives.add(clazz.newInstance());
			} catch (InstantiationException e) {
				throw new ArchiveException("Archive could not be instantiated", e);
			} catch (IllegalAccessException e) {
				throw new ArchiveException("Archive could not be instantiated", e);
			}
		} else {
			throw new ArchiveException("Archive does not implement the Archive.class interface.");
		}
	}
	
	/**
	 * Returns an empty Archive, if an archiver could be found for this factory.
	 * If two Archive-implementations with the same name are registered,
	 * the first matching archiver will be instanciated.
	 * 
	 * @return the archiver, or null, if no matching archiver could be found
	 * @throws ArchiveException if the archiver could not be created
	 */
	public static Archive getInstance(String archiverName) 
		throws ArchiveException {
		try {
			if(archiverName == null) {
				throw new ArchiveException("ArchiverFactory could not create instance");
			}
			Iterator it = archives.iterator();
			while(it.hasNext()) {
				PackableObject po = (PackableObject)it.next();
				if(po.isPackableWith(archiverName, PackableObject.CHOOSE_NAME)) {
					return (Archive)po.getClass().newInstance();
				}
			}
			return null;
		} catch (InstantiationException e) {
			throw new ArchiveException("ArchiverFactory could not create instance", e);
		} catch (IllegalAccessException e) {
			throw new ArchiveException("ArchiverFactory could not create instance", e);
		}
	}

	/**
	 * Returns an archiver, filled with an existing archive.
	 * Uses the byte header to identify the archiver. If no corresponding
	 * archiver could be found, a filename extension check will be done.
	 * @param archivedFile an existing archive
	 * @return an archiver, filled with the archive
	 */
	public static Archive getInstance(File file) 
		throws ArchiveException {
		if(file == null && !file.isFile()) {
			throw new ArchiveException("ArchiverFactory could not create instance for this file");
		}
		
		
		/* Archive result */
		PackableObject packable = null; 
		
		try {
			packable = PackableObject.identifyByHeader(file, archives);
			
			if(packable == null) {
				return null;
			}
			Archive archive = (Archive)packable.getClass().newInstance();
			archive.setArchive(file);
			return archive;
		} catch (SecurityException e) {
			throw new ArchiveException("A security violation occured while reading the field ARCHIVER_NAME", e);
		} catch (IllegalArgumentException e) {
			throw new ArchiveException("Internal factory exception", e);
		} catch (InstantiationException e) {
			throw new ArchiveException("ArchiverFactory could not create instance", e);
		} catch (IllegalAccessException e) {
			throw new ArchiveException("ArchiverFactory could not create instance", e);
		} catch (PackableObjectException e) {
			throw new ArchiveException("ArchiverFactory could not create instance", e);
		} 
	}
}
