package org.kshmakov.jfs.io;

public interface Offsets {
    long TOTAL_INODES = 8;
    long TOTAL_BLOCKS = 12;

    long TOTAL_UNALLOCATED_INODES = 16;
    long TOTAL_UNALLOCATED_BLOCKS = 20;

    long FIRST_UNALLOCATED_INODE_ID = 24;
    long FIRST_UNALLOCATED_BLOCK_ID = 28;
}
