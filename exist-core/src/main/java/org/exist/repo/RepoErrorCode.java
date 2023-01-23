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
package org.exist.repo;

import org.exist.dom.QName;
import org.exist.xquery.ErrorCodes.ErrorCode;

/**
 * Repo module specific error codes
 * @author <a href="mailto:aretter">Adam Retter</a>
 * @author <a href="mailto:github@line-o.de">Juri Leino</a>
 */
public class RepoErrorCode extends ErrorCode {
    
    /**
     * insufficient permissions
     */
    public final static ErrorCode PERMISSION_DENIED = new RepoErrorCode("PERMISSION_DENIED", "Permission denied.");

    /**
     * repository error states
     */
    public final static ErrorCode BAD_REPO_URL = new RepoErrorCode("BAD_REPO_URL", "Invalid repository URL");
    public final static ErrorCode REPO_CONNECTION = new RepoErrorCode("REPO_CONNECTION", "Failed to connect to package repository");

    /**
     * package metadata errors
     */
    public final static ErrorCode NOT_FOUND = new RepoErrorCode("NOT_FOUND", "Package not found.");
    public final static ErrorCode DEP_NOT_FOUND = new RepoErrorCode("DEP_NOT_FOUND", "Could not resolve dependency.");
    public final static ErrorCode BAD_COLLECTION_URI = new RepoErrorCode("BAD_COLLECTION_URI", "Bad collection URI.");
    public final static ErrorCode PARSE_DESCRIPTOR = new RepoErrorCode("PARSE_DESCRIPTOR", "Error in descriptor found.");
    public final static ErrorCode DESCRIPTOR_MISSING = new RepoErrorCode("DESCRIPTOR_MISSING", "A descriptor is missing.");

    /**
     * unspecific error thrown from repo module if installation failed
     */
    public final static ErrorCode INSTALLATION = new RepoErrorCode("INSTALLATION", "Package installation failed.");
    
    public final static String REPO_ERROR_NS = Deployment.REPO_NAMESPACE;
    public final static String REPO_ERROR_PREFIX = "repo";
    
    private RepoErrorCode(String code, String description) {
        super(new QName(code, REPO_ERROR_NS, REPO_ERROR_PREFIX), description);
    }
}