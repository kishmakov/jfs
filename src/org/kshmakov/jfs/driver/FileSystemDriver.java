package org.kshmakov.jfs.driver;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.primitives.AllocatedInode;
import org.kshmakov.jfs.io.primitives.DirectoryBlock;
import org.kshmakov.jfs.io.primitives.DirectoryEntry;
import org.kshmakov.jfs.io.primitives.Header;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class FileSystemDriver {
    private final FileSystemAccessor myAccessor;
    private final FileSystemLocator myLocator;
    private final ReadWriteLock[] myInodeLocks = new ReadWriteLock[16];

    private ByteBuffer inodeBuffer(int inodeId) throws JFSBadFileException {
        int offset = myLocator.inodeOffset(inodeId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Parameters.INODE_SIZE);
        buffer.rewind();
        return buffer;
    }

    private AllocatedInode inode(int inodeId) throws JFSBadFileException {
        return new AllocatedInode(inodeBuffer(inodeId));
    }

    private ByteBuffer blockBuffer(int blockId) throws JFSBadFileException {
        int offset = myLocator.blockOffset(blockId);
        ByteBuffer buffer = myAccessor.readBuffer(offset, Header.DATA_BLOCK_SIZE);
        buffer.rewind();
        return buffer;
    }

    public static DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    public Directory directory(DirectoryDescriptor descriptor) throws JFSException {
        Directory directory = new Directory();

        AllocatedInode inode = null;

        Lock readLock = myInodeLocks[descriptor.inodeId % myInodeLocks.length].readLock();
        readLock.lock();

//        try {
            inode = inode(descriptor.inodeId);
//        }

//        finally {
//            readLock.unlock();
//        }

            for (int blockId : inode.directPointers) {
                if (blockId == 0)
                    continue;

                ByteBuffer buffer = blockBuffer(blockId);
                DirectoryBlock block = new DirectoryBlock(buffer);

                for (DirectoryEntry entry : block.entries) {
                    if (entry.type == Parameters.EntryType.DIRECTORY) {
                        directory.directories.put(entry.name, new DirectoryDescriptor(entry.inodeId, entry.name));
                    } else {
                        directory.files.put(entry.name, new FileDescriptor(entry.inodeId, entry.name));
                    }
                }
            }


        // TODO: indirect blocks

        return directory;
    }

    public FileSystemDriver(String name) throws FileNotFoundException, JFSException {
        myAccessor = new FileSystemAccessor(name);
        myLocator = new FileSystemLocator(myAccessor);

        for (int i = 0; i < myInodeLocks.length; ++i) {
            myInodeLocks[i] = new ReentrantReadWriteLock();
        }
    }
}
