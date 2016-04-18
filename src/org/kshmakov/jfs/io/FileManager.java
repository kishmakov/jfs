package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.Header;
import org.kshmakov.jfs.io.primitives.Inode;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FileManager {

    private enum INODE_TYPE {
        DIRECTORY,
        FILE
    }

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

    private static Header defaultHeader(long fileSize) {
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

        Header header = defaultHeader(accessor.fileSize);
        accessor.writeBuffer(header.toBuffer());

        for (int inodeId = 0; inodeId < header.inodesTotal; inodeId++) {
            Inode inode = new Inode(inodeId == 0 ? Inode.ALLOCATED : Inode.UNALLOCATED);

            if (inodeId > 0) {
                inode.nextId = (inodeId + 2) % (header.inodesTotal + 1);
            } else {
                inode.type = Inode.Type.DIRECTORY;
                inode.parentId = 1;
                inode.directPointers[0] = 1;
            }

            accessor.writeBuffer(inode.toBuffer());
        }

        for (int blockId = 0; blockId < header.blocksTotal; blockId++) {
            ByteBuffer block = FileAccessor.newBuffer(Header.DATA_BLOCK_SIZE);
            if (blockId > 0) {
                block.putInt((blockId + 2) % (header.blocksTotal + 1));
            } else {
                // TODO: empty directory layout
            }

            accessor.writeBuffer(block);
        }
    }

    private FileAccessor myFileAccessor;

    public FileManager(String name) throws IOException {
        myFileAccessor = new FileAccessor(name);

        ByteBuffer headerBuffer = myFileAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        Header header = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", header.inodesTotal);
        System.out.printf("blocks total = %d\n", header.blocksTotal);
    }
}
