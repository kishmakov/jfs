package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FileManager {
    private static int inodeOffset(int inodeId) {
        assert inodeId > 0;
        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    private int blockOffset(int blockId) {
        assert blockId > 0;
        return Parameters.HEADER_SIZE
                + myHeader.inodesTotal * Parameters.INODE_SIZE
                + (blockId - 1) * Header.DATA_BLOCK_SIZE;
    }

    private FileAccessor myFileAccessor;
    private Header myHeader;

    private ByteBuffer inodeBuffer(int inodeId) throws IOException {
        int offset = inodeOffset(inodeId);
        ByteBuffer buffer = myFileAccessor.readBuffer(offset, Parameters.INODE_SIZE);
        buffer.rewind();
        return buffer;
    }

    private ByteBuffer blockBuffer(int blockId) throws IOException {
        int offset = blockOffset(blockId);
        ByteBuffer buffer = myFileAccessor.readBuffer(offset, Header.DATA_BLOCK_SIZE);
        buffer.rewind();
        return buffer;
    }

    public AllocatedInode rootInode() throws IOException {
        return inode(Parameters.ROOT_INODE_ID);
    }

    public AllocatedInode inode(int inodeId) throws IOException {
        return new AllocatedInode(inodeBuffer(inodeId));
    }

    public Directory directory(AllocatedInode inode) throws IOException {
        assert inode.type == AllocatedInode.Type.DIRECTORY;
        Directory directory = new Directory();

        for (int blockId: inode.directPointers) {
            if (blockId == 0)
                continue;

            ByteBuffer buffer = blockBuffer(blockId);
            DirectoryBlock block = new DirectoryBlock(buffer);

            for (DirectoryEntry entry: block.entries) {
                directory.entries.put(entry.myName, Directory.description(entry.type, entry.inodeId));
            }
        }

        // TODO: indirect blocks

        return directory;
    }

    public FileManager(String name) throws IOException {
        myFileAccessor = new FileAccessor(name);

        ByteBuffer headerBuffer = myFileAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        myHeader = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", myHeader.inodesTotal);
        System.out.printf("blocks total = %d\n", myHeader.blocksTotal);

    }
}
