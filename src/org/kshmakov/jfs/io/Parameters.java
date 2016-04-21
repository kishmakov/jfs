package org.kshmakov.jfs.io;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 10000000;

    int HEADER_SIZE = 32;
    int INODE_SIZE = 64;

    int MAGIC_NUMBER = 0xAABBCCDD;

    int ROOT_INODE_ID = 1;

    enum EntryType {
        DIRECTORY,
        FILE
    }

    static byte typeToByte(EntryType type) {
        return (byte) type.ordinal();
    }

    static EntryType byteToType(byte typeByte) {
        return typeByte == 0 ? EntryType.DIRECTORY : EntryType.FILE;
    }
}
