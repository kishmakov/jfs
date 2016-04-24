package org.kshmakov.jfs.driver;

public class NamedDirectoryDescriptor {
    public final DirectoryDescriptor descriptor;
    public final String name;

    NamedDirectoryDescriptor(DirectoryDescriptor descriptor, String name) {
        this.descriptor = descriptor;
        this.name = name;
    }
}
