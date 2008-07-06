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

/**
 * Exception occurs when a exception within 
 * the compress actions occurs.
 */
public class CompressException extends PackableObjectException {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257005449554507057L;

	/**
	 * Calls the IOException constructor
	 */
	public CompressException() {
		super();
	}

	/**
	 * Calls the IOException constructor with a message
	 * @param message the message
	 */
	public CompressException(String message) {
		super(message);
	}
	
	/**
	 * Calls the IOException constructor with a message
	 * and fills the stacktrace with the stacktrace of 
	 * an exception
	 * 
	 * @param message the message
	 * @param e the exception 
	 */
	public CompressException(String message, Exception e) {
		super(message);
		this.initCause(e);
	}
}
