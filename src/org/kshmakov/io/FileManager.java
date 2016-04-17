package org.kshmakov.io;

import org.kshmakov.io.primitives.Header;

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

    private static ByteBuffer allocateBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(Parameters.ENDIANNESS);
        return buffer;
    }

    private static Header defaultHeader(long fileSize) {
        Header header = new Header();

        header.inodesTotal = numberOfInodes(fileSize);
        header.blocksTotal = numberOfBlocks(fileSize, header.DATA_BLOCK_SIZE);

        header.inodesUnallocated = header.inodesTotal - 1;
        header.blocksUnallocated = header.blocksTotal - 1;

        header.firstUnallocatedInode = 2;
        header.firstUnallocatedBlock = 2;

        return header;
    }

    public static void formatFile(String fileName) throws IOException {
        FileAccessor accessor = new FileAccessor(fileName);

        Header header = defaultHeader(accessor.fileSize);
        accessor.writeBuffer(header.toBuffer());

        for (int inodeId = 0; inodeId < header.inodesTotal; inodeId++) {
            ByteBuffer inode = allocateBuffer(Parameters.INODE_SIZE);
            if (inodeId > 0) {
                inode.putInt((inodeId + 2) % (header.inodesTotal + 1));
            } else {
                inode.putInt((INODE_TYPE.DIRECTORY.ordinal() << 24) + 0x000001);
                inode.putInt(header.DATA_BLOCK_SIZE);
                inode.putInt(0x00000001); // pointer to first data block
            }

            accessor.writeBuffer(inode);
        }

        for (int blockId = 0; blockId < header.blocksTotal; blockId++) {
            ByteBuffer block = allocateBuffer(header.DATA_BLOCK_SIZE);
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

        ByteBuffer header = myFileAccessor.readBuffer(0, Parameters.HEADER_SIZE);

        if (header.getInt() != Parameters.MAGIC_NUMBER) {
            throw new IOException("Bad file provided.");
        }
    }
}
