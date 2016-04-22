package org.kshmakov.jfs.io;

public interface InodeOffsets {
    byte NEXT_INODE = 0;
    byte OBJECT_SIZE = 4;
    byte[] DIRECT_POINTERS = {
        8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52
    };
}
