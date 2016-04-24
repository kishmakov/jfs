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

    private static void refuseIf(boolean condition, String message) throws JFSRefuseException {
        if (condition) {
            throw new JFSRefuseException(message);
        }
    }

    public FileSystemDriver(String name) throws JFSException {
        myAccessor = new FileAccessor(name);
        myInodesStack = new InodesStack(myAccessor);
        myBlocksStack = new BlocksStack(myAccessor);

        for (int i = 0; i < myInodesLocks.length; ++i) {
            myInodesLocks[i] = new ReentrantReadWriteLock();
        }
    }

    private int allocateInode(InodeBase inode) throws JFSException {
        int inodeId;

        myHeaderLock.lock();
        try {
            refuseIf(myInodesStack.empty(), "no unallocated inodes left");
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
                refuseIf(myBlocksStack.size() < amount, "not enough unallocated blocks for requested operation");
                while (amount-- > 0) {
                    result.add(myBlocksStack.pop());
                }
            } finally {
                myHeaderLock.unlock();
            }
        }

        return result;
    }

    private void freeBlocks(ArrayList<Integer> ids) throws JFSException {
        if (ids.isEmpty()) {
            return;
        }

        myHeaderLock.lock();
        try {
            for (int id : ids) {
                myBlocksStack.push(id);
            }
        } finally {
            myHeaderLock.unlock();
        }
    }

    @GuardedBy("myInodesLocks")
    private void appendBlocks(int inodeId, ArrayList<Integer> newBlocksIds) throws JFSException {
        final int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);

        int firstId = InodeHelper.blocksForSize(size);
        int lastId = firstId + newBlocksIds.size() - 1;
        assert lastId < Parameters.DIRECT_POINTERS_NUMBER;

        for (int directId = firstId; directId <= lastId; ++directId) {
            int blockId = newBlocksIds.remove(newBlocksIds.size() - 1);
            myAccessor.writeInodeInt(blockId, inodeId, InodeOffsets.DIRECT_POINTERS[directId]);
        }

        myAccessor.writeInodeInt((lastId + 1) * Parameters.DATA_BLOCK_SIZE, inodeId, InodeOffsets.OBJECT_SIZE);
        assert newBlocksIds.isEmpty();
        // TODO: support doubly and triply indirect
    }

    @GuardedBy("myInodesLocks")
    private ArrayList<Integer> subtractBlocks(int inodeId, int blocksNumber) throws JFSException {
        final int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        int lastId = InodeHelper.blocksForSize(size) - 1;
        int firstId = lastId - blocksNumber + 1;
        assert firstId >= 0 && lastId < Parameters.DIRECT_POINTERS_NUMBER;

        ArrayList<Integer> result = new ArrayList<Integer>(blocksNumber);
        for (int directId = firstId; directId <= lastId; ++directId) {
            result.add(myAccessor.readInodeInt(inodeId, InodeOffsets.DIRECT_POINTERS[directId]));
            myAccessor.writeInodeInt(0, inodeId, InodeOffsets.DIRECT_POINTERS[directId]);
        }

        myAccessor.writeInodeInt(firstId * Parameters.DATA_BLOCK_SIZE, inodeId, InodeOffsets.OBJECT_SIZE);
        return result;
        // TODO: support doubly and triply indirect
    }

    @GuardedBy("myInodesLocks")
    private void growInode(int inodeId, int blocksNumber) throws JFSException {
        appendBlocks(inodeId, allocateBlocks(blocksNumber));
    }

    @GuardedBy("myInodesLocks")
    private void truncateInode(int inodeId, int blocksNumber) throws JFSException {
        freeBlocks(subtractBlocks(inodeId, blocksNumber));
    }

    @GuardedBy("myInodesLocks")
    private void writeIntoFile(int inodeId, ByteBuffer buffer, int offset) throws JFSException {
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
    private void tryWriteIntoFile(int inodeId, ByteBuffer buffer, int offset) throws JFSException {
        int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);

        refuseIf(currentSize < offset, "requested offset is bigger than file size");
        refuseIf(offset + (long) buffer.capacity() > Parameters.MAX_FILE_SIZE, "operation will produce too big file");

        int newSize = Math.max(currentSize, offset + buffer.capacity());

        int blocksHave = InodeHelper.blocksForSize(currentSize);
        int blocksNeed = InodeHelper.blocksForSize(newSize);

        if (blocksNeed > blocksHave) {
            growInode(inodeId, blocksNeed - blocksHave);
        }

        writeIntoFile(inodeId, buffer, offset);
    }

    @GuardedBy("myInodesLocks")
    private void tryRewriteFile(int inodeId, ByteBuffer buffer) throws JFSException {
        int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        int newSize = buffer.capacity();

        refuseIf(newSize > Parameters.MAX_FILE_SIZE, "operation will produce too big file");

        int blocksHave = InodeHelper.blocksForSize(currentSize);
        int blocksNeed = InodeHelper.blocksForSize(newSize);

        if (blocksNeed > blocksHave) {
            growInode(inodeId, blocksNeed - blocksHave);
        } else if (blocksNeed < blocksHave) {
            truncateInode(inodeId, blocksHave - blocksNeed);
        }

        writeIntoFile(inodeId, buffer, 0);
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

    @GuardedBy("myInodesLocks")
    private void removeFile(int inodeId, Directory directory, String name) throws JFSException {
        FileDescriptor descriptor = directory.getFile(name);
        assert descriptor != null;
        tryRewriteFile(descriptor.inodeId, ByteBuffer.allocate(0));
        directory.files.removeIf(file -> file.name.equals(name));
        tryRewriteFile(inodeId, ByteBufferHelper.toBuffer(directory));
    }

    @NotNull
    public DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    public void tryAddDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        String resolution = NameHelper.inspect(name);
        refuseIf(!resolution.isEmpty(), resolution);

        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            refuseIf(directory.getDirectory(name) != null || directory.getFile(name) != null, name + " is already in use");
            int newInodeId = allocateInode(new AllocatedInode(Parameters.EntryType.DIRECTORY, descriptor.inodeId));
            DirectoryBlock newDirectory = DirectoryBlock.emptyDirectoryBlock(newInodeId, descriptor.inodeId);
            tryRewriteFile(newInodeId, newDirectory.toBuffer());
            directory.directories.add(new DirectoryDescriptor(newInodeId, name));
            tryRewriteFile(descriptor.inodeId, ByteBufferHelper.toBuffer(directory));
        } finally {
            writeLock.unlock();
        }
    }

    public void tryRemoveDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        refuseIf(name.equals(".") || name.equals(".."), "cannot remove system directory");
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            Directory directory = getEntries(descriptor.inodeId);
            if (directory.getDirectory(name) == null) {
                if (directory.getFile(name) != null) {
                    removeFile(descriptor.inodeId, directory, name);
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
                refuseIf(directory.getDirectory(name) != null, "cannot remove " + name + ": is a directory");
                throw new JFSRefuseException("cannot remove " + name + ": no such file");
            }

            removeFile(descriptor.inodeId, directory, name);
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
