package org.kshmakov.jfs.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 100500;

    int HEADER_SIZE = 32;
    int INODE_SIZE = 64;

    int MAGIC_NUMBER = 0xAABBCCDD;

    int ROOT_INODE_ID = 1;

    char SEPARATOR = '/';

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
