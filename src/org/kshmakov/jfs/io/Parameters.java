package org.kshmakov.jfs.io;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 10000000;

    short HEADER_SIZE = 32;
    short DATA_BLOCK_SIZE = 0x1000;
    short INODE_SIZE = 64;

    int MAGIC_NUMBER = 0xAABBCCDD;

    short FILE_SYSTEM_VERSION = 0x0000;

    int ROOT_INODE_ID = 1;
    int DIRECT_POINTERS_NUMBER = 12;

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
