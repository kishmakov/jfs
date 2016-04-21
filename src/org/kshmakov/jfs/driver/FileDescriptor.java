package org.kshmakov.jfs.driver;

public class FileDescriptor {
    public final int inodeId;
    public final String name;

    FileDescriptor(int inodeId, String name) {
        this.inodeId = inodeId;
        this.name = name;
    }
}
