package org.kshmakov.io;

import java.io.IOException;
import java.nio.ByteOrder;
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

    private static int numberOfBlocks(long size) {
        assert size >= Parameters.MIN_SIZE && size <= Parameters.MAX_SIZE;
        long inodesSize = numberOfInodes(size) * Parameters.INODE_SIZE;
        long blocksSize = size - Parameters.HEADER_SIZE - inodesSize;
        return (int) (blocksSize / Parameters.BLOCK_SIZE);
    }

    private static ByteBuffer allocateBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer;
    }

    public static void formatFile(String fileName) throws IOException {
        FileAccessor accessor = new FileAccessor(fileName);

        ByteBuffer header = allocateBuffer(Parameters.HEADER_SIZE);

        header.putInt(Parameters.MAGIC_NUMBER);
        header.putShort((short) Parameters.BLOCK_SIZE);
        header.putInt(numberOfInodes(accessor.fileSize));
        header.putInt(2); // first vacant inode
        header.putInt(2); // first vacant data block

        accessor.writeBuffer(header);

        int inodesNum = numberOfInodes(accessor.fileSize);
        for (int inodeId = 0; inodeId < inodesNum; inodeId++) {
            ByteBuffer inode = allocateBuffer(Parameters.INODE_SIZE);
            if (inodeId > 0) {
                inode.putInt((inodeId + 2) % (inodesNum + 1));
            } else {
                inode.putInt((INODE_TYPE.DIRECTORY.ordinal() << 24) + 0x000001);
                inode.putInt(Parameters.BLOCK_SIZE);
                inode.putInt(0x00000001); // pointer to first data block
            }

            accessor.writeBuffer(inode);
        }

        int blocksNum = numberOfBlocks(accessor.fileSize);
        for (int blockId = 0; blockId < blocksNum; blockId++) {
            ByteBuffer block = allocateBuffer(Parameters.BLOCK_SIZE);
            if (blockId > 0) {
                block.putInt((blockId + 2) % (blocksNum + 1));
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
