/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.compress;

/**
 * Exception occurs when a exception within 
 * the unpack actions occurs.
 */
public class UnpackException  extends ArchiveException {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3906647522633135668L;

	/**
	 * Calls the super constructor
	 */
	public UnpackException() {
		super();
	}

	/**
	 * Calls the super constructor with a message
	 * @param message the message
	 */
	public UnpackException(String message) {
		super(message);
	}
	
	/**
	 * Calls the super constructor with a message
	 * and fills the stacktrace with the stacktrace of 
	 * an exception
	 * 
	 * @param message the message
	 * @param e the exception 
	 */
	public UnpackException(String message, Exception e) {
		super(message);
		this.initCause(e);
	}
}
