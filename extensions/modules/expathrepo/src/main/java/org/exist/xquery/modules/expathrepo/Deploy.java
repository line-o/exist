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
package org.exist.xquery.modules.expathrepo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.SystemProperties;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.QName;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.persistent.LockedDocument;
import org.exist.repo.Deployment;
import org.exist.repo.PackageLoader;
import org.exist.repo.RepoErrorCode;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.Txn;
import org.exist.util.io.TemporaryFileManager;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.value.*;
import org.expath.pkg.repo.PackageException;
import org.expath.pkg.repo.XarFileSource;
import org.expath.pkg.repo.XarSource;
import org.xml.sax.helpers.AttributesImpl;
import sun.net.ConnectionResetException;

public class Deploy extends BasicFunction {

	protected static final Logger logger = LogManager.getLogger(Deploy.class);
	
	public final static FunctionSignature signatures[] = {
		new FunctionSignature(
			new QName("deploy", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
			"Deploy an application package. Installs package contents to the specified target collection, using the permissions " +
			"defined by the &lt;permissions&gt; element in repo.xml. Pre- and post-install XQuery scripts can be specified " +
			"via the &lt;prepare&gt; and &lt;finish&gt; elements.",
			new SequenceType[] { new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE, "package name")},
			new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE, 
					"<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
		new FunctionSignature(
				new QName("deploy", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
				"Deploy an application package. Installs package contents to the specified target collection, using the permissions " +
				"defined by the &lt;permissions&gt; element in repo.xml. Pre- and post-install XQuery scripts can be specified " +
				"via the &lt;prepare&gt; and &lt;finish&gt; elements.",
				new SequenceType[] { 
					new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE, "package name"),
					new FunctionParameterSequenceType("targetCollection", Type.STRING, Cardinality.EXACTLY_ONE, "the target " +
							"collection into which the package will be stored")
				},
				new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE, 
						"<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
        new FunctionSignature(
            new QName("install-and-deploy", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
            "Downloads, installs and deploys a package from the public repository at $publicRepoURL. Dependencies are resolved " +
            "automatically. For downloading the package, the package name is appended to the repository URL as " +
            "parameter 'name'.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE,
                            "Unique name of the package to install."),
                    new FunctionParameterSequenceType("publicRepoURL", Type.STRING, Cardinality.EXACTLY_ONE,
                            "The URL of the public repo.")
            },
            new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                    "<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
        new FunctionSignature(
            new QName("install-and-deploy", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
            "Downloads, installs and deploys a package from the public repository at $publicRepoURL. Dependencies are resolved " +
            "automatically. For downloading the package, the package name and version are appended to the repository URL as " +
            "parameters 'name' and 'version'.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE,
                            "Unique name of the package to install."),
                    new FunctionParameterSequenceType("version", Type.STRING, Cardinality.ZERO_OR_ONE,
                            "Version to install."),
                    new FunctionParameterSequenceType("publicRepoURL", Type.STRING, Cardinality.EXACTLY_ONE,
                            "The URL of the public repo.")
            },
            new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                    "<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
        new FunctionSignature(
                new QName("install-and-deploy-from-db", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
                "Installs and deploys a package from a .xar archive file stored in the database. Dependencies are not " +
                "resolved and will just be ignored.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE,
                        "Database path to the package archive (.xar file)")
                },
                new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                        "<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
        new FunctionSignature(
            new QName("install-and-deploy-from-db", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
            "Installs and deploys a package from a .xar archive file stored in the database. Dependencies will be downloaded " +
            "from the public repo and installed automatically.",
            new SequenceType[] {
                new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE,
                        "Database path to the package archive (.xar file)"),
                new FunctionParameterSequenceType("publicRepoURL", Type.STRING, Cardinality.EXACTLY_ONE,
                        "The URL of the public repo.")
            },
            new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                    "<status result=\"ok\"/> if deployment was ok. Throws an error otherwise.")),
		new FunctionSignature(
				new QName("undeploy", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
				"Uninstall the resources belonging to a package from the db. Calls cleanup scripts if defined.",
				new SequenceType[] { new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE, "package name")},
				new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE, 
						"<status result=\"ok\"/> if deployment was ok. Throws an error otherwise."))
	};

	private static final QName STATUS_ELEMENT = new QName("status", ExpathPackageModule.NAMESPACE_URI);
	
	public Deploy(final XQueryContext context, final FunctionSignature signature) {
		super(context, signature);
	}

	@Override
	public Sequence eval(final Sequence[] args, final Sequence contextSequence)
			throws XPathException {
		if (!context.getSubject().hasDbaRole()) {
			throw new XPathException(this, RepoErrorCode.PERMISSION_DENIED, "Permission denied. You need to be a member " +
					"of the dba group to use repo:deploy/undeploy");
        }

		final String pkgName = args[0].getStringValue();
        try {
            Deployment deployment = new Deployment();
            final Optional<String> target;
            if (isCalledAs("deploy")) {
                String userTarget = null;
                if (getArgumentCount() == 2) {
                    userTarget = args[1].getStringValue();
                }
                try (final Txn transaction = context.getBroker().continueOrBeginTransaction()) {
                    target = deployment.deploy(context.getBroker(), transaction, pkgName, context.getRepository(), userTarget);
                    transaction.commit();
                }
            } else if (isCalledAs("install-and-deploy")) {
                String version = null;
                final String repoURI;
                if (getArgumentCount() == 3) {
                    version = args[1].getStringValue();
                    repoURI = args[2].getStringValue();
                } else {
                    repoURI = args[1].getStringValue();
                }
                try (final Txn transaction = context.getBroker().continueOrBeginTransaction()) {
                    target = installAndDeploy(transaction, pkgName, version, repoURI);
                    transaction.commit();
                }
            } else if (isCalledAs("install-and-deploy-from-db")) {
                String repoURI = null;
                if (getArgumentCount() == 2) {
                    repoURI = args[1].getStringValue();
                }
                try (final Txn transaction = context.getBroker().continueOrBeginTransaction()) {
                    target = installAndDeployFromDb(transaction, pkgName, repoURI);
                    transaction.commit();
                }
            } else {
                try (final Txn transaction = context.getBroker().continueOrBeginTransaction()) {
                    target = deployment.undeploy(context.getBroker(), transaction, pkgName, context.getRepository());
                    transaction.commit();
                }
	        }
	        target.orElseThrow(() -> new XPathException(this, RepoErrorCode.INSTALLATION, "expath repository is not available."));
            return statusReport(target);
        } catch (PackageException e) {
            throw new XPathException(this, RepoErrorCode.NOT_FOUND, e.getMessage(), args[0], e);
        } catch (IOException e) {
            throw new XPathException(this, ErrorCodes.FOER0000, "Caught IO error while deploying expath archive", args[0], e);
        } catch (TransactionException e) {
            throw new XPathException(this, ErrorCodes.FOER0000, "Caught transaction error while deploying expath archive", args[0], e);
        }
    }

    private Optional<String> installAndDeploy(final Txn transaction, final String pkgName, final String version, final String repoURI) throws XPathException {
        try {
            final RepoPackageLoader loader = new RepoPackageLoader(this, repoURI);
            final Deployment deployment = new Deployment();
            final XarSource xar = loader.load(pkgName, new PackageLoader.Version(version, false));
            if (xar != null) {
                return deployment.installAndDeploy(context.getBroker(), transaction, xar, loader);
            }
            return Optional.empty();
        } catch (final MalformedURLException | ClassCastException e) {
            throw new XPathException(this, RepoErrorCode.BAD_REPO_URL, "Malformed URL: " + repoURI);
        } catch (final PackageException | IOException e) {
            LOG.error(e.getMessage(), e);
            throw new XPathException(this, RepoErrorCode.INSTALLATION, e.getMessage());
        }
    }

    private Optional<String> installAndDeployFromDb(final Txn transaction, final String path, final String repoURI) throws XPathException {
        final XmldbURI docPath = XmldbURI.createInternal(path);
        try(final LockedDocument lockedDoc = context.getBroker().getXMLResource(docPath, LockMode.READ_LOCK)) {
            if(lockedDoc == null) {
                throw new XPathException(this, RepoErrorCode.NOT_FOUND, path + " no such .xar",
                        new StringValue(path));
            }

            final DocumentImpl doc = lockedDoc.getDocument();
            if (doc.getResourceType() != DocumentImpl.BINARY_FILE) {
                throw new XPathException(this, RepoErrorCode.NOT_FOUND, path + " is not a valid .xar",
                        new StringValue(path));
            }

            RepoPackageLoader loader = null;
            if (repoURI != null) {
                loader = new RepoPackageLoader(this, repoURI);
            }

            final XarSource xarSource =  new BinaryDocumentXarSource(context.getBroker().getBrokerPool(), transaction, (BinaryDocument)doc);
            final Deployment deployment = new Deployment();
            return deployment.installAndDeploy(context.getBroker(), transaction, xarSource, loader);
        } catch (final PermissionDeniedException e) {
            LOG.error(e.getMessage(), e);
            throw new XPathException(this, RepoErrorCode.PERMISSION_DENIED, e.getMessage());
        } catch (PackageException e) {
            final String msg = e.getMessage();
            LOG.error(msg, e);
            throw new XPathException(this, RepoErrorCode.PARSE_DESCRIPTOR, e.getMessage());
        } catch (IOException e) {
            final String msg = e.getMessage();
            LOG.error(msg, e);
            throw new XPathException(this, RepoErrorCode.INSTALLATION, e.getMessage(),
                    new StringValue(e.getMessage()));
        }
    }

	private Sequence statusReport(final Optional<String> target) {
		context.pushDocumentContext();
		try {
			final MemTreeBuilder builder = context.getDocumentBuilder();
			final AttributesImpl attrs = new AttributesImpl();
			if (target.isPresent()) {
			    attrs.addAttribute("", "result", "result", "CDATA", "ok");
			    attrs.addAttribute("", "target", "target", "CDATA", target.get());
			} else {
			    attrs.addAttribute("", "result", "result", "CDATA", "fail");
			}
			builder.startElement(STATUS_ELEMENT, attrs);
			builder.endElement();
			
			return builder.getDocument().getNode(1);
		} finally {
			context.popDocumentContext();
		}
		
	}

	@Override
	public void resetState(final boolean postOptimization) {
		super.resetState(postOptimization);
	}

    private static class RepoPackageLoader implements PackageLoader {

        private final Expression expr;
        private final String repoURL;
        private final String processorVersion;

        public RepoPackageLoader(final Expression expr, final String repoURL) {
            this.expr = expr;
            this.repoURL = repoURL;
            this.processorVersion = SystemProperties.getInstance()
                    .getSystemProperty("product-version", "2.2.0");
        }

        private String getQuery (final String name, final Version version) throws XPathException {
            try {
                final String query = "?name=" + URLEncoder.encode(name, "UTF-8") + "&processor=" + processorVersion;

                // query list of exact versions
                if (version.getVersions() != null) {
                    return query +
                            "&version=" + URLEncoder.encode(version.getVersions(), "UTF-8");
                }
                // query semver
                if (version.getSemVer() != null) {
                    return query +
                            "&semver=" + version.getSemVer();
                }
                // query range
                final String min = version.getMin() == null ? "" : "&semver-min=" + version.getMin();
                final String max = version.getMax() == null ? "" : "&semver-max=" + version.getMax();
                return query + min + max;
            } catch (UnsupportedEncodingException e) {
                throw new XPathException(expr, RepoErrorCode.BAD_REPO_URL, e.getMessage());
            }
        }

        private HttpURLConnection connect(final String query) throws XPathException {
            try {
                final URL url = new URI(repoURL + query).toURL();
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15 * 1000);
                connection.setReadTimeout(15 * 1000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "eXist-db (" + processorVersion + ")");
                connection.connect();
                return connection;
            } catch (URISyntaxException | ClassCastException e) {
                throw new XPathException(expr, RepoErrorCode.BAD_REPO_URL, e.getMessage());
            } catch (IOException e) {
                throw new XPathException(expr, RepoErrorCode.REPO_CONNECTION, "Failed to connect to " + repoURL,
                        new StringValue(query));
            }
        }

        @Override
        public XarSource load(final String name, final Version version) throws XPathException {
            final String query = getQuery(name, version);
            LOG.info("Retrieving package {} from {}", name, repoURL);
            LOG.debug("Package repository query {}", query);
            final HttpURLConnection connection = connect(query);

            // TODO(AR) we likely don't need temporary caching here! could just use UriXarSource
            try(final InputStream is = connection.getInputStream()) {
                final TemporaryFileManager temporaryFileManager = TemporaryFileManager.getInstance();
                final Path outFile = temporaryFileManager.getTemporaryFile();
                Files.copy(is, outFile, StandardCopyOption.REPLACE_EXISTING);
                return new XarFileSource(outFile);
            } catch (IOException e) {
                throw new XPathException(expr, RepoErrorCode.NOT_FOUND,
                        "Failed to resolve package " + name + " " + version + " from " + repoURL,
                        new StringValue("name:" + name + " version:" + version + " query:" + query));
            }
        }
    }
}
