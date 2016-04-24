package org.kshmakov.jfs.driver;

// details are supposed to be inaccessible from outside of the package
final public class FileDescriptor {
    final int inodeId;

    FileDescriptor(int inodeId) {
        this.inodeId = inodeId;
    }
}
