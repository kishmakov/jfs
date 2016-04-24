package org.kshmakov.jfs.driver;

// details are supposed to be inaccessible from outside of the package
final public class DirectoryDescriptor {
    final int inodeId;

    DirectoryDescriptor(int inodeId) {
        this.inodeId = inodeId;
    }
}
