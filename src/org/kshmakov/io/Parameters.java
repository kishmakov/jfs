package org.kshmakov.io;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 100500;

    int HEADER_SIZE = 18;
    int INODE_SIZE = 64;
    int BLOCK_SIZE = 4096;

    int MAGIC_NUMBER = 0xAABBCCDD;
}
