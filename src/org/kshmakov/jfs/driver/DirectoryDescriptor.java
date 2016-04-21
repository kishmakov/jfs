package org.kshmakov.jfs.driver;

public class DirectoryDescriptor {

    public final int inodeId;
    public final String name;

    public DirectoryDescriptor(int inodeId, String name) {
        this.inodeId = inodeId;
        this.name = name;
    }
}
