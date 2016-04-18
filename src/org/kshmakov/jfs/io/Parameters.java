package org.kshmakov.jfs.io;

public interface Parameters {
    long MIN_SIZE = 239;
    long MAX_SIZE = 100500;

    int HEADER_SIZE = 32;
    int INODE_SIZE = 64;

    int MAGIC_NUMBER = 0xAABBCCDD;
}
