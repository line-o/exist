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
package org.exist.http.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.http.RESTServer;
import org.exist.http.NotFoundException;
import org.exist.http.BadRequestException;
import org.exist.http.MethodNotAllowedException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.validation.XmlLibraryChecker;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Optional;

/**
 * Implements the REST-style interface if eXist is running within a Servlet
 * engine. The real work is done by class {@link org.exist.http.RESTServer}.
 *
 * @author wolf
 */
public class EXistServlet extends AbstractExistHttpServlet {

    private static final long serialVersionUID = -3563999345725645647L;
    private final static Logger LOG = LogManager.getLogger(EXistServlet.class);
    private RESTServer srvREST;

    public enum FeatureEnabled {
        FALSE,
        TRUE,
        AUTHENTICATED_USERS_ONLY
    }

    @Override
    public Logger getLog() {
        return LOG;
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        String useDynamicContentType = config.getInitParameter("dynamic-content-type");
        if (useDynamicContentType == null) {
            useDynamicContentType = "no";
        }

        final FeatureEnabled xquerySubmission = parseFeatureEnabled(config, "xquery-submission", FeatureEnabled.TRUE);
        final FeatureEnabled xupdateSubmission = parseFeatureEnabled(config, "xupdate-submission", FeatureEnabled.TRUE);

        // Instantiate REST Server
        srvREST = new RESTServer(getPool(), getFormEncoding(), getContainerEncoding(), useDynamicContentType.equalsIgnoreCase("yes")
                || useDynamicContentType.equalsIgnoreCase("true"), isInternalOnly(), xquerySubmission, xupdateSubmission);

        // XML lib checks....
        XmlLibraryChecker.check();
    }

    private FeatureEnabled parseFeatureEnabled(final ServletConfig config, final String paramName, final FeatureEnabled defaultValue) {
        final String paramValue = config.getInitParameter(paramName);
        if (paramValue != null) {
            switch (paramValue) {
                case "disabled":
                    return FeatureEnabled.FALSE;
                case "enabled":
                    return FeatureEnabled.TRUE;
                case "authenticated":
                    return FeatureEnabled.AUTHENTICATED_USERS_ONLY;
            }
        }

        return defaultValue;
    }

    /**
     * Returns an adjusted the URL path of the request.
     *
     * @param request the http servlet request
     * @return the adjusted path of the request
     */
    private String adjustPath(final HttpServletRequest request) throws ServletException {
        String path = request.getPathInfo();

        if (path == null) {
            return "";
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(" In: " + path);
        }

        // path contains both required and superficial escapes,
        // as different user agents use different conventions;
        // for the sake of interoperability, remove any unnecessary escapes
        try {
            // URI.create undoes _all_ escaping, so protect slashes first
            final URI u = URI.create("file://" + path.replaceAll("%2F", "%252F"));
            // URI four-argument constructor recreates all the necessary ones
            final URI v = new URI("http", "host", u.getPath(), null).normalize();
            // unprotect slashes in now normalized path
            path = v.getRawPath().replaceAll("%252F", "%2F");
        } catch (final URISyntaxException e) {
            throw new ServletException(e.getMessage(), e);
        }
        // eat trailing slashes, else collections might not be found
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // path now is in proper canonical encoded form

        if (LOG.isDebugEnabled()) {
            LOG.debug("Out: " + path);
        }

        return path;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String method = request.getMethod();

        // first, adjust the path
        String path = adjustPath(request);

        // third, authenticate the user
        final Subject user = authenticate(request, response);
        if (user == null) {
            // You now get a HTTP Authentication challenge if there is no user
            return;
        }

        // fourth, process the request
        try (final DBBroker broker = getPool().get(Optional.of(user));
            final Txn transaction = getPool().getTransactionManager().beginTransaction()) {
            try {
                switch (method) {
                    case "GET":
                        srvREST.doGet(broker, transaction, request, response, path);
                        break;
                    case "HEAD":
                        srvREST.doHead(broker, transaction, request, response, path);
                        break;
                    case "POST":
                        srvREST.doPost(broker, transaction, request, response, path);
                        break;
                    case "PATCH":
                        srvREST.doPatch(broker, transaction, path, request, response);
                        break;
                    case "PUT":
                        srvREST.doPut(broker, transaction, path, request, response);
                        break;
                    case "DELETE":
                        srvREST.doDelete(broker, transaction, path, request, response);
                        break;
                    case "OPTIONS":
                        this.doOptions(request, response);
                        break;
    //                    case "TRACE":
    //                    case "CONNECT":
                    default:
                        String errMsg = "http.method_not_implemented: %s";
                        Object[] errArgs = new Object[]{method};
                        errMsg = MessageFormat.format(errMsg, errArgs);
                        throw new MethodNotAllowedException(errMsg);
                }
                transaction.commit();

            } catch (final Throwable t) {
                transaction.abort();
                throw t;
            }

        } catch (final BadRequestException e) {
            if (response.isCommitted()) {
                throw new ServletException(e.getMessage());
            }
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

        } catch (final PermissionDeniedException e) {
            // If the current user is the Default User and they do not have permission
            // then send a challenge request to prompt the client for a username/password.
            // Else return a FORBIDDEN Error
            if (user.equals(getDefaultUser())) {
                getAuthenticator().sendChallenge(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        } catch (final NotFoundException e) {
            if (response.isCommitted()) {
                throw new ServletException(e.getMessage());
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (final MethodNotAllowedException e) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getMessage());
        } catch (final EXistException e) {
            if (response.isCommitted()) {
                throw new ServletException(e.getMessage(), e);
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (final EOFException ee) {
            String errMsg = "%s Connection has been interrupted";
            Object[] errArgs = new Object[]{method};
            errMsg = MessageFormat.format(errMsg, errArgs);

            LOG.error(errMsg, ee);
            throw new ServletException(errMsg, ee);
        } catch (final Throwable e) {
            LOG.error(e.getMessage(), e);
            throw new ServletException("An error occurred: " + e.getMessage(), e);
        }

    }
}
