package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FileSystemManager {
    private FileSystemAccessor myAccessor;
    private Header myHeader;

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

    private ByteBuffer inodeBuffer(int inodeId) throws IOException {
        int offset = inodeOffset(inodeId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Parameters.INODE_SIZE);
        buffer.rewind();
        return buffer;
    }

    private AllocatedInode inode(int inodeId) throws IOException {
        return new AllocatedInode(inodeBuffer(inodeId));
    }

    private ByteBuffer blockBuffer(int blockId) throws IOException {
        int offset = blockOffset(blockId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Header.DATA_BLOCK_SIZE);
        buffer.rewind();
        return buffer;
    }

    public static Descriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    public Directory directory(Descriptor descriptor) throws IOException {
        assert descriptor.getType() == Parameters.EntryType.DIRECTORY;

        AllocatedInode inode = inode(descriptor.getInodeId());

        Directory directory = new Directory();

        for (int blockId : inode.directPointers) {
            if (blockId == 0)
                continue;

            ByteBuffer buffer = blockBuffer(blockId);
            DirectoryBlock block = new DirectoryBlock(buffer);

            for (DirectoryEntry entry : block.entries) {
                if (entry.type == Parameters.EntryType.DIRECTORY) {
                    directory.entries.put(entry.name, new DirectoryDescriptor(entry.inodeId, entry.name));
                } else {
                    directory.entries.put(entry.name, new FileDescriptor(entry.inodeId, entry.name));
                }
            }
        }

        // TODO: indirect blocks

        return directory;
    }

    public FileSystemManager(String name) throws IOException {
        myAccessor = new FileSystemAccessor(name);

        ByteBuffer headerBuffer = myAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        myHeader = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", myHeader.inodesTotal);
        System.out.printf("blocks total = %d\n", myHeader.blocksTotal);

    }
}
