package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.Header;

import java.nio.ByteBuffer;

@NotThreadSafe
public class FileSystemLocator {
    private final Header myHeader;

    public FileSystemLocator(FileSystemAccessor accessor) throws JFSBadFileException {
        ByteBuffer headerBuffer = accessor.readBuffer(0, Parameters.HEADER_SIZE);
        myHeader = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", myHeader.inodesTotal);
        System.out.printf("blocks total = %d\n", myHeader.blocksTotal);
    }

    public int inodeOffset(int inodeId) throws JFSException {

        if (inodeId <= 0 || inodeId > myHeader.inodesTotal) {
            String range = "[1; " + Integer.toString(myHeader.inodesTotal) + "]";
            throw new JFSException("inodeId=" + Integer.toString(inodeId) + " not in " + range);
        }

        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    public int blockOffset(int blockId) throws JFSException {
        if (blockId <= 0 || blockId > myHeader.blocksTotal) {
            String range = "[1; " + Integer.toString(myHeader.blocksTotal) + "]";
            throw new JFSException("blockId=" + Integer.toString(blockId) + " not in " + range);
        }
        return Parameters.HEADER_SIZE
                + myHeader.inodesTotal * Parameters.INODE_SIZE
                + (blockId - 1) * Header.DATA_BLOCK_SIZE;
    }
}
