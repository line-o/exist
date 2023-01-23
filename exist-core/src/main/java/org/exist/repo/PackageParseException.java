package org.exist.repo;

import org.expath.pkg.repo.PackageException;

public class PackageParseException extends PackageException {
    public PackageParseException(String msg) {
        super(msg);
    }

    public PackageParseException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
