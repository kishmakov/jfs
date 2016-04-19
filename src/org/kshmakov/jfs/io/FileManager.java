package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FileManager {

    private static int numberOfInodes(long size) {
        assert size >= Parameters.MIN_SIZE && size <= Parameters.MAX_SIZE;
        // heuristic rule, see https://en.wikipedia.org/wiki/Inode#Details
        return (int) (size / (100 * Parameters.INODE_SIZE));
    }

    private static int numberOfBlocks(long size, short blockSize) {
        assert size >= Parameters.MIN_SIZE && size <= Parameters.MAX_SIZE;
        long inodesSize = numberOfInodes(size) * Parameters.INODE_SIZE;
        long sizeLeft = size - Parameters.HEADER_SIZE - inodesSize;
        return (int) (sizeLeft / blockSize);
    }

    private static Header emptyFileHeader(long fileSize) {
        Header header = new Header();

        header.inodesTotal = numberOfInodes(fileSize);
        header.blocksTotal = numberOfBlocks(fileSize, Header.DATA_BLOCK_SIZE);

        header.inodesUnallocated = header.inodesTotal - 1;
        header.blocksUnallocated = header.blocksTotal - 1;

        header.firstUnallocatedInodeId = 2;
        header.firstUnallocatedBlockId = 2;

        return header;
    }

    public static void formatFile(String fileName) throws IOException {
        FileAccessor accessor = new FileAccessor(fileName);

        Header header = emptyFileHeader(accessor.fileSize);
        accessor.writeBuffer(header.toBuffer());

        for (int inodeId = 0; inodeId < header.inodesTotal; inodeId++) {

            if (inodeId > 0) {
                VacantInode inode = new VacantInode((inodeId + 2) % (header.inodesTotal + 1));
                accessor.writeBuffer(inode.toBuffer());
            } else {
                AllocatedInode inode = new AllocatedInode();
                inode.type = AllocatedInode.Type.DIRECTORY;
                inode.parentId = 1;
                inode.objectSize = Header.DATA_BLOCK_SIZE;
                inode.directPointers[0] = 1;
                accessor.writeBuffer(inode.toBuffer());
            }


        }

        for (int blockId = 0; blockId < header.blocksTotal; blockId++) {
            if (blockId > 0) {
                VacantBlock block = new VacantBlock(Header.DATA_BLOCK_SIZE, (blockId + 2) % (header.blocksTotal + 1));
                accessor.writeBuffer(block.toBuffer());
            } else {
                ByteBuffer block = FileAccessor.newBuffer(Header.DATA_BLOCK_SIZE);
                block.position(2);
                block.put((new DirectoryEntry(1, ".")).toBuffer());
                block.put((new DirectoryEntry(1, "..")).toBuffer());
                char unusedSize = (char) (Header.DATA_BLOCK_SIZE - block.position());
                block.position(0);
                block.putChar(unusedSize);
                accessor.writeBuffer(block);
            }
        }
    }

    private static int inodeOffset(int inodeId) {
        return Parameters.HEADER_SIZE + inodeId * Parameters.INODE_SIZE;
    }

    private FileAccessor myFileAccessor;

    private int currentInodeId;

    public FileManager(String name) throws IOException {
        myFileAccessor = new FileAccessor(name);

        ByteBuffer headerBuffer = myFileAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        Header header = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", header.inodesTotal);
        System.out.printf("blocks total = %d\n", header.blocksTotal);
    }
}
