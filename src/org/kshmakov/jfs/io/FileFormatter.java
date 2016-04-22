package org.kshmakov.jfs.io;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.AllocatedInode;
import org.kshmakov.jfs.io.primitives.BlockBase;
import org.kshmakov.jfs.io.primitives.DirectoryBlock;
import org.kshmakov.jfs.io.primitives.InodeBase;

public class FileFormatter extends FileAccessorBase {

    @Override
    protected int getTotalInodes() {
        assert fileSize >= Parameters.MIN_SIZE && fileSize <= Parameters.MAX_SIZE;
        // heuristic rule, see https://en.wikipedia.org/wiki/Inode#Details
        return Math.max(1, (int) (fileSize / (100 * Parameters.INODE_SIZE)));
    }

    @Override
    protected int getTotalBlocks() {
        assert fileSize >= Parameters.MIN_SIZE && fileSize <= Parameters.MAX_SIZE;
        long inodesSize = getTotalInodes() * Parameters.INODE_SIZE;
        long sizeLeft = fileSize - Parameters.HEADER_SIZE - inodesSize;
        return (int) (sizeLeft / Parameters.DATA_BLOCK_SIZE);
    }

    public FileFormatter(String fileName) throws JFSBadFileException {
        super(fileName);
    }

    public void format() throws JFSException {
        resetHeader();
        resetInodes();
        resetBlocks();

        AllocatedInode inode = new AllocatedInode(Parameters.EntryType.DIRECTORY, Parameters.ROOT_INODE_ID);
        inode.objectSize = Parameters.DATA_BLOCK_SIZE;
        inode.directPointers[0] = 1;
        writeInode(inode, Parameters.ROOT_INODE_ID);

        writeBlock(DirectoryBlock.emptyDirectoryBlock(Parameters.ROOT_INODE_ID, Parameters.ROOT_INODE_ID), 1);
    }

    private void resetHeader() throws JFSBadFileException {
        writeHeaderInt(Parameters.MAGIC_NUMBER, HeaderOffsets.MAGIC_NUMBER);
        int versionAndBlockSize = ((Parameters.FILE_SYSTEM_VERSION) << 16) + Parameters.DATA_BLOCK_SIZE;
        writeHeaderInt(versionAndBlockSize, HeaderOffsets.VERSION_AND_BLOCK_SIZE);
        writeHeaderInt(myTotalInodes, HeaderOffsets.TOTAL_INODES);
        writeHeaderInt(myTotalBlocks, HeaderOffsets.TOTAL_BLOCKS);
        writeHeaderInt(myTotalInodes - 1, HeaderOffsets.TOTAL_UNALLOCATED_INODES);
        writeHeaderInt(myTotalBlocks - 1, HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        writeHeaderInt(2, HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
        writeHeaderInt(2, HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
    }

    private void resetInodes() throws JFSException {
        for (int inodeId = 1; inodeId <= myTotalInodes; ++inodeId) {
            writeInode(new InodeBase((inodeId + 1) % (myTotalInodes + 1)), inodeId);
        }
    }

    private void resetBlocks() throws JFSException {
        for (int blockId = 1; blockId <= myTotalBlocks; ++blockId) {
            writeBlock(new BlockBase((blockId + 1) % (myTotalBlocks + 1)), blockId);
        }
    }
}
