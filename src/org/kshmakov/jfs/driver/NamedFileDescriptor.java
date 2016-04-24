package org.kshmakov.jfs.driver;

public class NamedFileDescriptor {
    public final FileDescriptor descriptor;
    public final String name;

    NamedFileDescriptor(FileDescriptor descriptor, String name) {
        this.descriptor = descriptor;
        this.name = name;
    }
}
