package org.kshmakov.jfs.io;

public class DirectoryDescriptor implements Descriptor {

    public final int myInodeId;
    public final String myName;

    @Override
    public Parameters.EntryType getType() {
        return Parameters.EntryType.DIRECTORY;
    }

    @Override
    public int getInodeId() {
        return myInodeId;
    }

    @Override
    public String getName() {
        return myName;
    }

    DirectoryDescriptor(int inodeId, String name) {
        myInodeId = inodeId;
        myName = name;
    }
}
