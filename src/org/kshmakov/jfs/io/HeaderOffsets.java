package org.kshmakov.jfs.io;

public interface HeaderOffsets {
    byte MAGIC_NUMBER = 0;
    byte VERSION_AND_BLOCK_SIZE = 4;
    byte TOTAL_INODES = 8;
    byte TOTAL_BLOCKS = 12;

    byte TOTAL_UNALLOCATED_INODES = 16;
    byte TOTAL_UNALLOCATED_BLOCKS = 20;

    byte FIRST_UNALLOCATED_INODE_ID = 24;
    byte FIRST_UNALLOCATED_BLOCK_ID = 28;
}
