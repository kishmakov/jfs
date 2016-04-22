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
                throw new JFSRefuseException("no unallocated blocks left");
            }

            blockId = myBlocksStack.pop();
        }
        finally {
            myHeaderLock.unlock();
        }

        myAccessor.writeBlock(block, blockId);
        return blockId;
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
                    result.add(new Integer(myBlocksStack.pop()));
                }
            }
            finally {
                myHeaderLock.unlock();
            }
        }

        return result;
    }

    @GuardedBy("myInodesLocks")
    private void appendBlock(int inodeId, int blockId) throws JFSException {
        int size = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        size += Parameters.DATA_BLOCK_SIZE;
        myAccessor.writeInodeInt(size, inodeId, InodeOffsets.OBJECT_SIZE);
        myAccessor.writeInodeInt(blockId, inodeId, InodeOffsets.DIRECT_POINTERS[0]);
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
    private DirectoryDescriptor addDirectoryEntry(int inodeId, int newInodeId, String name) throws JFSException {
        Directory directory = getEntries(inodeId);
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

    public FileSystemDriver(String name) throws JFSException {
        myAccessor = new FileAccessor(name);
        myInodesStack = new InodesStack(myAccessor);
        myBlocksStack = new BlocksStack(myAccessor);

        for (int i = 0; i < myInodesLocks.length; ++i) {
            myInodesLocks[i] = new ReentrantReadWriteLock();
        }
    }

    @NotNull
    public DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID, "");
    }

    @NotNull
    public DirectoryDescriptor tryAddDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
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

            AllocatedInode newInode = new AllocatedInode(Parameters.EntryType.DIRECTORY, descriptor.inodeId);
            int newInodeId = allocateInode(newInode);
            DirectoryBlock newDirectory = DirectoryBlock.emptyDirectoryBlock(newInodeId, descriptor.inodeId);
            tryWriteFile(newInodeId, newDirectory.toBuffer(), 0);

            return addDirectoryEntry(descriptor.inodeId, newInodeId, name);
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
