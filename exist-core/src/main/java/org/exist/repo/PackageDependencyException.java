package org.exist.repo;

import org.expath.pkg.repo.PackageException;

public class PackageDependencyException extends PackageException {
    public PackageDependencyException(String msg) {
        super(msg);
    }

    public PackageDependencyException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
