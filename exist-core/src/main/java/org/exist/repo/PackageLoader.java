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

import org.exist.xquery.XPathException;
import org.expath.pkg.repo.PackageException;
import org.expath.pkg.repo.XarSource;
import org.expath.pkg.repo.deps.DependencyVersion;

import javax.annotation.Nullable;

/**
 * Interface for resolving package dependencies. Implementations may load
 * packages e.g. from a public server or the file system.
 */
public interface PackageLoader {

    /**
     * Wrapper for the different version schemes supported by
     * the expath spec.
     */
    class Version {

        String min = null;
        String max = null;
        String semVer = null;
        String versions = null;

        public String getMin() {
            return min;
        }

        public String getMax() {
            return max;
        }

        public String getSemVer() {
            return semVer;
        }

        public String getVersions() {
            return versions;
        }

        public DependencyVersion getDependencyVersion() throws PackageException {
            return DependencyVersion.makeVersion(versions, semVer, min, max);
        }

        public String toString() {
            if (versions != null) {
                return versions;            // space separated version list
            }
            if (semVer != null) {
                return semVer;              // equal to SemVer template
            }
            if (min != null && max != null) {
                return min + "-" + max;     // SemVer range
            }
            if (min != null) {
                return ">=" + min;          // greater than or equal to SemVer template
            }
            if (max != null) {
                return "<=" + max;          // less than or equal to SemVer template
            }

            return "*";                     // any version
        }

        /**
         * values are exclusive except min and max
         */
        public Version(
                @Nullable final String versions,
                @Nullable final String semVer,
                @Nullable final String min,
                @Nullable final String max
        ) {
            if (versions != null && !versions.isEmpty()) {
                this.versions = versions;
                return;
            }
            if (semVer != null && !semVer.isEmpty()) {
                this.semVer = semVer;
                return;
            }
            if (min != null && !min.isEmpty()) {
                this.min = min;
            }
            if (max != null && !max.isEmpty()) {
                this.max = max;
            }
        }

        public Version(String version, boolean semver) {
            if (semver) {
                this.semVer = version;
            } else {
                this.versions = version;
            }
        }

        public Version(String min, String max) {
            this.min = min;
            this.max = max;
        }
    }

    /**
     * Locate the expath package identified by name.
     *
     * @param name    unique name of the package
     * @param version the version to install
     * @return a file containing the package or null if not found
     * @throws XPathException in case the package cannot be located
     */
    @Nullable
    XarSource load(String name, Version version) throws XPathException;
}
