package org.kshmakov.jfs.driver;

import com.sun.istack.internal.NotNull;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.primitives.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import org.kshmakov.jfs.io.NameHelper;

@ThreadSafe
public final class FileSystemDriver {
    private final FileAccessor myAccessor;

    private final ReadWriteLock[] myInodesLocks = new ReadWriteLock[16];
    private final Lock myHeaderLock = new ReentrantLock();

    @GuardedBy("myHeaderLock")
    private final InodesStack myInodesStack;

    @GuardedBy("myHeaderLock")
    private final BlocksStack myBlocksStack;

    @GuardedBy("myInodesLocks")
    private int allocateInode(InodeBase inode) throws JFSException {
        int inodeId;

        myHeaderLock.lock();
        try {
            if (myInodesStack.empty()) {
                throw new JFSRefuseException("no unallocated inodes left");
            }

            inodeId = myInodesStack.pop();
        }
        finally {
            myHeaderLock.unlock();
        }

        myAccessor.writeInode(inode, inodeId);
        return inodeId;
    }

    @GuardedBy("myInodesLocks")
    private int allocateBlock(BlockBase block) throws JFSException {
        int blockId;

        myHeaderLock.lock();
        try {
            if (myBlocksStack.size() == 0) {
                throw new JFSRefuseException("not unallocated blocks left");
            }

            blockId = myBlocksStack.pop();
        }
        finally {
            myHeaderLock.unlock();
        }

        myAccessor.writeBlock(block, blockId);
        return blockId;
    }

    @GuardedBy("myInodesLocks")
    private void appendBlock(int inodeId, int blockId) throws JFSException {
        int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        size += Parameters.DATA_BLOCK_SIZE;
        myAccessor.writeInodeInt(size, inodeId, InodeOffsets.OBJECT_SIZE);
        myAccessor.writeInodeInt(blockId, inodeId, InodeOffsets.DIRECT_POINTERS[0]);
    }

    @GuardedBy("myHeaderLock")
    private void writeFile(int inodeId, ArrayList<DirectoryBlock> blocks) throws JFSException {
        AllocatedInode inode = new AllocatedInode(myAccessor.readInode(inodeId));
        assert inode.objectSize == blocks.size() * Parameters.DATA_BLOCK_SIZE;

        int minSize = Math.min(Parameters.DIRECT_POINTERS_NUMBER, blocks.size());

        for (int blockId = 0; blockId < minSize && inode.directPointers[blockId] != 0; ++blockId) {
            myAccessor.writeBlock(blocks.get(blockId), inode.directPointers[blockId]);
        }
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
    private DirectoryDescriptor addDirectoryEntry(int inodeId, String name) throws JFSException {
        Directory directory = getEntries(inodeId);
        if (directory.getDirectory(name) != null || directory.getFile(name) != null) {
            throw new JFSRefuseException(name + " is already in use");
        }

        if (myBlocksStack.size() < 2) {
            throw new JFSRefuseException("not enough unallocated blocks to perform operation");
        }

        AllocatedInode newInode = new AllocatedInode(Parameters.EntryType.DIRECTORY);
        newInode.parentId = inodeId;

        int newInodeId = allocateInode(newInode);
        int newBlockId = allocateBlock(DirectoryBlock.emptyDirectoryBlock(newInodeId, inodeId));
        appendBlock(newInodeId, newBlockId);

        DirectoryDescriptor newDirectory = new DirectoryDescriptor(newInodeId, name);
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



        return newDirectory;
    }

    public FileSystemDriver(String name) throws FileNotFoundException, JFSException {
        myAccessor = new FileAccessor(name);
        myInodesStack = new InodesStack(myAccessor);
        myBlocksStack = new BlocksStack(myAccessor);

        for (int i = 0; i < myInodesLocks.length; ++i) {
            myInodesLocks[i] = new ReentrantReadWriteLock();
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

        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            return addDirectoryEntry(descriptor.inodeId, name);
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
