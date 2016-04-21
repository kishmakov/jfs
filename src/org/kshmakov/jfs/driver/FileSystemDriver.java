package org.kshmakov.jfs.driver;

import com.sun.istack.internal.NotNull;
import net.jcip.annotations.GuardedBy;
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
import org.kshmakov.jfs.io.tools.NameHelper;

@ThreadSafe
public final class FileSystemDriver {
    private final FileSystemAccessor myAccessor;
    private final FileSystemLocator myLocator;
    private final InodesStack myInodesStack;

    private final ReadWriteLock[] myInodeLocks = new ReadWriteLock[16];

    private ByteBuffer inodeBuffer(int inodeId) throws JFSException {
        return myAccessor.readBuffer(myLocator.inodeOffset(inodeId), Parameters.INODE_SIZE);
    }

    private ByteBuffer blockBuffer(int blockId) throws JFSException {
        return myAccessor.readBuffer(myLocator.blockOffset(blockId), Header.DATA_BLOCK_SIZE);
    }

    @NotNull @GuardedBy("myInodeLocks")
    private Directory getEntries(int inodeId) throws JFSException {
        AllocatedInode inode = new AllocatedInode(inodeBuffer(inodeId));
        Directory directory = new Directory();

        for (int blockId : inode.directPointers) {
            if (blockId == 0)
                continue;

            DirectoryBlock block = new DirectoryBlock(blockBuffer(blockId));

            for (DirectoryEntry entry : block.entries) {
                if (entry.type == Parameters.EntryType.DIRECTORY) {
                    directory.directories.add(new DirectoryDescriptor(entry.inodeId, entry.name));
                } else {
                    directory.files.add(new FileDescriptor(entry.inodeId, entry.name));
                }
            }
        }

        // TODO: indirect blocks
        return directory;
    }

    public FileSystemDriver(String name) throws FileNotFoundException, JFSException {
        myAccessor = new FileSystemAccessor(name);
        myLocator = new FileSystemLocator(myAccessor);
        myInodesStack = new InodesStack(myAccessor, myLocator);

        for (int i = 0; i < myInodeLocks.length; ++i) {
            myInodeLocks[i] = new ReentrantReadWriteLock();
        }
    }

    @NotNull
    public static DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    @NotNull
    public DirectoryDescriptor addDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        String resolution = NameHelper.inspect(name);
        if (!resolution.isEmpty()) {
            throw new JFSRefuseException(resolution);
        }

        Lock writeLock = myInodeLocks[descriptor.inodeId % myInodeLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            if (directory.getDirectory(name) != null || directory.getFile(name) != null) {
                throw new JFSRefuseException(name + " is already in use");
            }

            return new DirectoryDescriptor(0, "");
        }
        finally {
            writeLock.unlock();
        }
    }

    @NotNull
    public Directory getEntries(DirectoryDescriptor descriptor) throws JFSException {
        Lock readLock = myInodeLocks[descriptor.inodeId % myInodeLocks.length].readLock();
        readLock.lock();

        try {
            return getEntries(descriptor.inodeId);
        }
        finally {
            readLock.unlock();
        }
    }
}
