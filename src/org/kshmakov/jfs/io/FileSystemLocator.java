package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.Header;

import java.nio.ByteBuffer;

/**
 * Thread unsafe.
 */
public class FileSystemLocator {
    private final Header myHeader;

    public FileSystemLocator(FileSystemAccessor accessor) throws JFSBadFileException {
        ByteBuffer headerBuffer = accessor.readBuffer(0, Parameters.HEADER_SIZE);
        myHeader = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", myHeader.inodesTotal);
        System.out.printf("blocks total = %d\n", myHeader.blocksTotal);
    }

    public static int inodeOffset(int inodeId) {
        assert inodeId > 0;
        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    public int blockOffset(int blockId) {
        assert blockId > 0;
        return Parameters.HEADER_SIZE
                + myHeader.inodesTotal * Parameters.INODE_SIZE
                + (blockId - 1) * Header.DATA_BLOCK_SIZE;
    }
}
