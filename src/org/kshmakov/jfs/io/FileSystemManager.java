package org.kshmakov.jfs.io;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.*;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class FileSystemManager {
    private final FileSystemAccessor myAccessor;
    private final Header myHeader;
    private final ReadWriteLock[] myInodeLocks = new ReadWriteLock[16];

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

    private ByteBuffer inodeBuffer(int inodeId) throws JFSBadFileException {
        int offset = inodeOffset(inodeId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Parameters.INODE_SIZE);
        buffer.rewind();
        return buffer;
    }

    private AllocatedInode inode(int inodeId) throws JFSBadFileException {
        return new AllocatedInode(inodeBuffer(inodeId));
    }

    private ByteBuffer blockBuffer(int blockId) throws JFSBadFileException {
        int offset = blockOffset(blockId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Header.DATA_BLOCK_SIZE);
        buffer.rewind();
        return buffer;
    }

    public static Descriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    public Directory directory(Descriptor descriptor) throws JFSBadFileException, UnsupportedEncodingException {
        assert descriptor.getType() == Parameters.EntryType.DIRECTORY;

        Directory directory = new Directory();

        Lock readlock = myInodeLocks[descriptor.getInodeId() % myInodeLocks.length].readLock();
        readlock.lock();

        try {
            AllocatedInode inode = inode(descriptor.getInodeId());

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
        } finally {
            readlock.unlock();
        }

        // TODO: indirect blocks

        return directory;
    }

    public FileSystemManager(String name) throws FileNotFoundException, JFSException {
        myAccessor = new FileSystemAccessor(name);

        ByteBuffer headerBuffer = myAccessor.readBuffer(0, Parameters.HEADER_SIZE);
        myHeader = new Header(headerBuffer);

        for (int i = 0; i < myInodeLocks.length; ++i) {
            myInodeLocks[i] = new ReentrantReadWriteLock();
        }

//        System.out.printf("inodes total = %d\n", myHeader.inodesTotal);
//        System.out.printf("blocks total = %d\n", myHeader.blocksTotal);

    }
}
