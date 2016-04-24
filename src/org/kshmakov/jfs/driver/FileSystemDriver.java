package org.kshmakov.jfs.driver;

import com.sun.istack.internal.NotNull;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.driver.tools.ByteBufferHelper;
import org.kshmakov.jfs.driver.tools.InodeHelper;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.primitives.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public final class FileSystemDriver {
    private final FileAccessor myAccessor;

    private final ReadWriteLock[] myInodesLocks = new ReadWriteLock[16];
    private final Lock myHeaderLock = new ReentrantLock();

    @GuardedBy("myHeaderLock")
    private final InodesStack myInodesStack;

    @GuardedBy("myHeaderLock")
    private final BlocksStack myBlocksStack;

    public FileSystemDriver(String name) throws JFSException {
        myAccessor = new FileAccessor(name);
        myInodesStack = new InodesStack(myAccessor);
        myBlocksStack = new BlocksStack(myAccessor);

        for (int i = 0; i < myInodesLocks.length; ++i) {
            myInodesLocks[i] = new ReentrantReadWriteLock();
        }
    }

    @GuardedBy("myInodesLocks")
    private int allocateInode(InodeBase inode) throws JFSException {
        int inodeId;

        myHeaderLock.lock();
        try {
            if (myInodesStack.empty()) {
                throw new JFSRefuseException("no unallocated inodes left");
            }

            inodeId = myInodesStack.pop();
        } finally {
            myHeaderLock.unlock();
        }

        myAccessor.writeInode(inode, inodeId);
        return inodeId;
    }

    private ArrayList<Integer> allocateBlocks(int amount) throws JFSException {
        ArrayList<Integer> result = new ArrayList<Integer>(amount);

        if (amount > 0) {
            myHeaderLock.lock();
            try {
                if (myBlocksStack.size() < amount) {
                    throw new JFSRefuseException("not enough unallocated blocks for requested operation");
                }

                while (amount-- > 0) {
                    result.add(myBlocksStack.pop());
                }
            } finally {
                myHeaderLock.unlock();
            }
        }

        return result;
    }

    @GuardedBy("myInodesLocks")
    private void appendBlocks(int inodeId, ArrayList<Integer> newBlocksIds) throws JFSException {
        final int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        int currentOffset = 0;

        for (int directId = 0; directId < Parameters.DIRECT_POINTERS_NUMBER && !newBlocksIds.isEmpty(); ++directId) {
            if (currentOffset < size) {
                currentOffset += Parameters.DATA_BLOCK_SIZE;
                continue;
            }
            int blockId = newBlocksIds.remove(newBlocksIds.size() - 1);
            myAccessor.writeInodeInt(blockId, inodeId, InodeOffsets.DIRECT_POINTERS[directId]);
        }

        if (newBlocksIds.isEmpty()) {
            return;
        }

        // TODO: support doubly and triply indirect
        assert newBlocksIds.isEmpty();
    }

    @GuardedBy("myInodesLocks")
    private void writeFile(int inodeId, ByteBuffer buffer, int offset) throws JFSException {
        buffer.rewind();
        int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        if (size < offset + buffer.capacity()) {
            size = offset + buffer.capacity();
            myAccessor.writeInodeInt(size, inodeId, InodeOffsets.OBJECT_SIZE);
        }

        for (int directId = 0; directId < Parameters.DIRECT_POINTERS_NUMBER && buffer.hasRemaining(); ++directId) {
            if (offset >= Parameters.DATA_BLOCK_SIZE) {
                offset -= Parameters.DATA_BLOCK_SIZE;
                continue;
            }

            int blockId = myAccessor.readInodeInt(inodeId, InodeOffsets.DIRECT_POINTERS[directId]);
            ByteBuffer blockBuffer = myAccessor.readBlock(blockId);
            blockBuffer.position(offset);
            offset = 0;

            blockBuffer.put(ByteBufferHelper.advance(buffer, blockBuffer.remaining()));
            myAccessor.writeBlock(new BlockBase(blockBuffer), blockId);
        }

        // TODO: support doubly and triply indirect

        assert !buffer.hasRemaining();
    }

    @GuardedBy("myInodesLocks")
    private void tryWriteFile(int inodeId, ByteBuffer buffer, int offset) throws JFSException {
        int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);

        if (currentSize < offset) {
            throw new JFSRefuseException("requested offset is bigger than file size");
        }

        int newBlocksNeeded = InodeHelper.newBlocksNeeded(currentSize, offset, buffer.capacity());
        ArrayList<Integer> newBlocksIds = allocateBlocks(newBlocksNeeded);
        appendBlocks(inodeId, newBlocksIds);
        writeFile(inodeId, buffer, offset);
    }

    @NotNull
    @GuardedBy("myInodesLocks")
    private Directory getEntries(int inodeId) throws JFSException {
        AllocatedInode inode = new AllocatedInode(myAccessor.readInode(inodeId));
        Directory directory = new Directory();

        for (int blockId : inode.directPointers) {
            if (blockId == 0)
                continue;

            DirectoryBlock block = new DirectoryBlock(myAccessor.readBlock(blockId));

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

    @NotNull
    public DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    public void tryAddDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        String resolution = NameHelper.inspect(name);
        if (!resolution.isEmpty()) {
            throw new JFSRefuseException(resolution);
        }

        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            if (directory.getDirectory(name) != null || directory.getFile(name) != null) {
                throw new JFSRefuseException(name + " is already in use");
            }

            int newInodeId = allocateInode(new AllocatedInode(Parameters.EntryType.DIRECTORY, descriptor.inodeId));
            DirectoryBlock newDirectory = DirectoryBlock.emptyDirectoryBlock(newInodeId, descriptor.inodeId);
            tryWriteFile(newInodeId, newDirectory.toBuffer(), 0);

            directory.directories.add(new DirectoryDescriptor(newInodeId, name));
            tryWriteFile(descriptor.inodeId, ByteBufferHelper.toBuffer(directory), 0);
        } finally {
            writeLock.unlock();
        }
    }

    public void tryRemoveDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            if (directory.getDirectory(name) == null) {
                if (directory.getFile(name) != null) {
                    // TODO: remove file here
                    return;
                }

                throw new JFSRefuseException("cannot remove " + name + ": no such directory");
            }
            // TODO: remove directory here
        } finally {
            writeLock.unlock();
        }
    }

    public void tryRemoveFile(DirectoryDescriptor descriptor, String name) throws JFSException {
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            if (directory.getFile(name) == null) {
                if (directory.getDirectory(name) != null) {
                    throw new JFSRefuseException("cannot remove " + name + ": is a directory");
                }

                throw new JFSRefuseException("cannot remove " + name + ": no such file");
            }

            // TODO: remove directory here
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    public Directory getEntries(DirectoryDescriptor descriptor) throws JFSException {
        Lock readLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].readLock();
        readLock.lock();

        try {
            return getEntries(descriptor.inodeId);
        } finally {
            readLock.unlock();
        }
    }
}
