package org.kshmakov.io;

import java.nio.ByteOrder;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 100500;

    int HEADER_SIZE = 32;
    int INODE_SIZE = 64;

    int MAGIC_NUMBER = 0xAABBCCDD;

    ByteOrder ENDIANNESS = ByteOrder.BIG_ENDIAN;
}
