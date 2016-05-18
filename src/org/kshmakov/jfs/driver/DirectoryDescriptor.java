package org.kshmakov.jfs.driver;

// details are supposed to be inaccessible from outside of the package
final public class DirectoryDescriptor {
    final int inodeId;

    DirectoryDescriptor(int inodeId) {
        this.inodeId = inodeId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DirectoryDescriptor) {
            DirectoryDescriptor that = (DirectoryDescriptor) other;
            return this.inodeId == that.inodeId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return inodeId;
    }
}
