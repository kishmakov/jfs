package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.Header;

import java.nio.ByteBuffer;

@NotThreadSafe
public class FileSystemLocator {
    private final int inodesTotal;
    private final int blocksTotal;

    public FileSystemLocator(FileSystemAccessor accessor) throws JFSBadFileException {
        Header header = accessor.header();

        inodesTotal = header.inodesTotal;
        blocksTotal = header.blocksTotal;

        System.out.printf("inodes total = %d\n", inodesTotal);
        System.out.printf("blocks total = %d\n", blocksTotal);
    }

    public int inodeOffset(int inodeId) throws JFSException {

        if (inodeId <= 0 || inodeId > inodesTotal) {
            String range = "[1; " + Integer.toString(inodesTotal) + "]";
            throw new JFSException("inodeId=" + Integer.toString(inodeId) + " not in " + range);
        }

        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    public int blockOffset(int blockId) throws JFSException {
        if (blockId <= 0 || blockId > blocksTotal) {
            String range = "[1; " + Integer.toString(blocksTotal) + "]";
            throw new JFSException("blockId=" + Integer.toString(blockId) + " not in " + range);
        }
        return Parameters.HEADER_SIZE
                + inodesTotal * Parameters.INODE_SIZE
                + (blockId - 1) * Header.DATA_BLOCK_SIZE;
    }
}
