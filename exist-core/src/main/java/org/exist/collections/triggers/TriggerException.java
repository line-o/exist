/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.collections.triggers;

import org.xml.sax.SAXException;

/**
 * @author wolf
 */
public class TriggerException extends SAXException {

	private static final long serialVersionUID = -6501877347817156557L;

	public TriggerException() {
		super();
	}

	/**
	 * @param message of the exception
	 */
	public TriggerException(String message) {
		super(message);
	}

	/**
	 * @param cause of the exception
	 */
	public TriggerException(Exception cause) {
		super(cause);
	}

	/**
	 * @param message of the exception
	 * @param cause of the exception
	 */
	public TriggerException(String message, Exception cause) {
		super(message, cause);
	}

}
