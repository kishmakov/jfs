package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FileManager {
    private static int inodeOffset(int inodeId) {
        assert inodeId > 0;
        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    private FileAccessor myFileAccessor;

    private int myCurrentInodeId;
    private AllocatedInode myCurrentInode;

    private ByteBuffer inodeBuffer(int inodeId) throws IOException {
        int offset = inodeOffset(inodeId);
        ByteBuffer buffer = myFileAccessor.readBuffer(offset, Parameters.INODE_SIZE);
        buffer.rewind();
        return buffer;
    }

    public FileManager(String name) throws IOException {
        myFileAccessor = new FileAccessor(name);

        ByteBuffer headerBuffer = myFileAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        Header header = new Header(headerBuffer);

        System.out.printf("inodes total = %d\n", header.inodesTotal);
        System.out.printf("blocks total = %d\n", header.blocksTotal);

        myCurrentInodeId = Parameters.ROOT_INODE_ID;
        myCurrentInode = new AllocatedInode(inodeBuffer(myCurrentInodeId));

        System.out.printf("In first inode: blockId = %d\n", myCurrentInode.directPointers[0]);
    }
}
