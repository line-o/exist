package org.exist.repo;

import org.expath.pkg.repo.PackageException;

public class PackageNotFoundException extends PackageException {
    public PackageNotFoundException(String msg) {
        super(msg);
    }

    public PackageNotFoundException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
