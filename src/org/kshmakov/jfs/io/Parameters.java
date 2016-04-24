package org.kshmakov.jfs.io;

public interface Parameters {
    short HEADER_SIZE = 32;
    short DATA_BLOCK_SIZE = 0x1000;
    short INODE_SIZE = 64;

    long MIN_FS_SIZE = HEADER_SIZE + DATA_BLOCK_SIZE + INODE_SIZE;
    long MAX_FS_SIZE = Integer.MAX_VALUE - DATA_BLOCK_SIZE;

    int MAGIC_NUMBER = 0xAABBCCDD;

    short FILE_SYSTEM_VERSION = 0x0000;

    int ROOT_INODE_ID = 1;
    int DIRECT_POINTERS_NUMBER = 12;

    int MAX_FILE_SIZE = DATA_BLOCK_SIZE * DIRECT_POINTERS_NUMBER; // TODO: support double and triple pointers

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
