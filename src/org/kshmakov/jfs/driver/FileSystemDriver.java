package org.kshmakov.jfs.driver;

import com.sun.istack.internal.NotNull;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.primitives.*;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import org.kshmakov.jfs.io.NameHelper;

@ThreadSafe
public final class FileSystemDriver {
    private final FileSystemAccessor myAccessor;    

    private final ReadWriteLock[] myInodeLocks = new ReadWriteLock[16];
    private final Lock myHeaderLock = new ReentrantLock();

    @GuardedBy("myHeaderLock")
    private final InodesStack myInodesStack;

    @GuardedBy("myHeaderLock")
    private final BlocksStack myBlocksStack;

    @GuardedBy("myInodeLocks")
    private ByteBuffer readInode(int inodeId) throws JFSException {
        return myAccessor.readBuffer(myAccessor.inodeOffset(inodeId), Parameters.INODE_SIZE);
    }

    @GuardedBy("myInodeLocks")
    private int allocateInode(Parameters.EntryType type, int parentId) throws JFSException {
        if (myInodesStack.empty()) {
            throw new JFSRefuseException("no unallocated inodes left");
        }

        int newInodeId = myInodesStack.pop();
        AllocatedInode newInode = new AllocatedInode(type);
        newInode.parentId = parentId;
        writeInode(newInodeId, newInode.toBuffer());

        return newInodeId;
    }

    @GuardedBy("myInodeLocks")
    private void allocateBlock(int inodeId, BlockBase block) throws JFSException {
        if (myBlocksStack.size() == 0) {
            throw new JFSRefuseException("not enough unallocated blocks to perform operation");
        }

        int newBlockId = myBlocksStack.pop();
        int size = myAccessor.readInt(myAccessor.inodeOffset(inodeId) + 4);
        size += Parameters.DATA_BLOCK_SIZE;
        myAccessor.writeInt(myAccessor.inodeOffset(inodeId) + 4, size);
        myAccessor.writeInt(myAccessor.inodeOffset(inodeId) + 8, newBlockId);
        myAccessor.writeBlock(block, newBlockId);
    }

    @GuardedBy("myInodeLocks")
    private void writeInode(int inodeId, ByteBuffer buffer) throws JFSException {
        assert buffer.capacity() == Parameters.INODE_SIZE;
        myAccessor.writeBuffer(buffer, myAccessor.inodeOffset(inodeId));
    }

    @GuardedBy("myInodeLocks")
    private ByteBuffer readBlock(int blockId) throws JFSException {
        return myAccessor.readBuffer(myAccessor.blockOffset(blockId), Parameters.DATA_BLOCK_SIZE);
    }

    @GuardedBy("myHeaderLock")
    private void writeFile(int inodeId, ArrayList<DirectoryBlock> blocks) throws JFSException {
        AllocatedInode inode = new AllocatedInode(readInode(inodeId));
        assert inode.objectSize == blocks.size() * Parameters.DATA_BLOCK_SIZE;

        int minSize = Math.min(AllocatedInode.DIRECT_POINTERS_NUMBER, blocks.size());

        for (int blockId = 0; blockId < minSize && inode.directPointers[blockId] != 0; ++blockId) {
            myAccessor.writeBlock(blocks.get(blockId), inode.directPointers[blockId]);
        }
    }

    @NotNull
    @GuardedBy("myInodeLocks")
    private Directory getEntries(int inodeId) throws JFSException {
        AllocatedInode inode = new AllocatedInode(readInode(inodeId));
        Directory directory = new Directory();

        for (int blockId : inode.directPointers) {
            if (blockId == 0)
                continue;

            DirectoryBlock block = new DirectoryBlock(readBlock(blockId));

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

    private DirectoryDescriptor addDirectoryEntry(int inodeId, String name) throws JFSException {
        Directory directory = getEntries(inodeId);
        if (directory.getDirectory(name) != null || directory.getFile(name) != null) {
            throw new JFSRefuseException(name + " is already in use");
        }

        myHeaderLock.lock();

        DirectoryDescriptor newDirectory = null;

        try {
            if (myBlocksStack.size() < 2) {
                throw new JFSRefuseException("not enough unallocated blocks to perform operation");
            }

            int newInodeId = allocateInode(Parameters.EntryType.DIRECTORY, inodeId);
            allocateBlock(newInodeId, DirectoryBlock.emptyDirectoryBlock(newInodeId, inodeId));

            newDirectory = new DirectoryDescriptor(newInodeId, name);
            directory.directories.add(newDirectory);
            ArrayList<DirectoryBlock> blocks = new ArrayList<DirectoryBlock>();
            blocks.add(new DirectoryBlock(Parameters.DATA_BLOCK_SIZE));

            directory.directories.forEach(item -> {
                DirectoryBlock block = blocks.get(blocks.size() - 1);
                DirectoryEntry entry = null;
                try {
                    entry = new DirectoryEntry(item.inodeId, Parameters.EntryType.DIRECTORY, item.name);
                } catch (JFSException e) {
                    e.printStackTrace(); // TODO: patch me
                }
                if (!block.tryInsert(entry)) {
                    DirectoryBlock newBlock = new DirectoryBlock(Parameters.DATA_BLOCK_SIZE);
                    boolean result = newBlock.tryInsert(entry);
                    assert result;
                    blocks.add(newBlock);
                }
            });

            directory.files.forEach(item -> {
                DirectoryBlock block = blocks.get(blocks.size() - 1);
                DirectoryEntry entry = null;
                try {
                    entry = new DirectoryEntry(item.inodeId, Parameters.EntryType.FILE, item.name);
                } catch (JFSException e) {
                    e.printStackTrace(); // TODO: patch me
                }
                if (!block.tryInsert(entry)) {
                    DirectoryBlock newBlock = new DirectoryBlock(Parameters.DATA_BLOCK_SIZE);
                    boolean result = newBlock.tryInsert(entry);
                    assert result;
                    blocks.add(newBlock);
                }
            });

            writeFile(inodeId, blocks);

        }
        finally {
            myHeaderLock.unlock();
        }

        return newDirectory;
    }

    public FileSystemDriver(String name) throws FileNotFoundException, JFSException {
        myAccessor = new FileSystemAccessor(name);
        myInodesStack = new InodesStack(myAccessor);
        myBlocksStack = new BlocksStack(myAccessor);

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
            return addDirectoryEntry(descriptor.inodeId, name);
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    public Directory getEntries(DirectoryDescriptor descriptor) throws JFSException {
        Lock readLock = myInodeLocks[descriptor.inodeId % myInodeLocks.length].readLock();
        readLock.lock();

        try {
            return getEntries(descriptor.inodeId);
        } finally {
            readLock.unlock();
        }
    }
}
