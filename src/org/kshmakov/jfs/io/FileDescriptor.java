package org.kshmakov.jfs.io;

public class FileDescriptor implements Descriptor {
    public final int myInodeId;
    public final String myName;

    @Override
    public Parameters.EntryType getType() {
        return Parameters.EntryType.FILE;
    }

    @Override
    public int getInodeId() {
        return myInodeId;
    }

    @Override
    public String getName() {
        return myName;
    }

    public FileDescriptor(int inodeId, String name) {
        myInodeId = inodeId;
        myName = name;
    }
}
