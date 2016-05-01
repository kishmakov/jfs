package org.kshmakov.jfs.driver;

import com.sun.istack.internal.NotNull;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.driver.tools.ByteBufferHelper;
import org.kshmakov.jfs.driver.tools.EntriesHelper;
import org.kshmakov.jfs.driver.tools.InodeHelper;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.primitives.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
        final int inodeId;

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

        final int firstId = InodeHelper.blocksForSize(size);
        final int lastId = firstId + newBlocksIds.size() - 1;
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
        final int lastId = InodeHelper.blocksForSize(size) - 1;
        final int firstId = lastId - blocksNumber + 1;
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

    @NotNull
    @GuardedBy("myInodesLocks")
    private ByteBuffer readFromFile(int inodeId, int offset, int length) throws JFSException {
        ByteBuffer buffer = ByteBuffer.allocate(length);

        for (int directId = 0; directId < Parameters.DIRECT_POINTERS_NUMBER && buffer.hasRemaining(); ++directId) {
            if (offset >= Parameters.DATA_BLOCK_SIZE) {
                offset -= Parameters.DATA_BLOCK_SIZE;
                continue;
            }

            int blockId = myAccessor.readInodeInt(inodeId, InodeOffsets.DIRECT_POINTERS[directId]);
            ByteBuffer blockBuffer = myAccessor.readBlock(blockId);
            blockBuffer.position(offset);
            offset = 0;
            buffer.put(ByteBufferHelper.advance(blockBuffer, buffer.remaining()));
        }

        // TODO: support doubly and triply indirect
        assert !buffer.hasRemaining();

        return buffer;
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

    @NotNull
    @GuardedBy("myInodesLocks")
    private ByteBuffer tryReadFromFile(int inodeId, int offset, int maxLength) throws JFSException {
        final int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        refuseIf(currentSize < offset, "requested offset is bigger than file size");
        maxLength = Math.min(maxLength, currentSize - offset);
        return readFromFile(inodeId, offset, maxLength);
    }

    @GuardedBy("myInodesLocks")
    private void tryWriteIntoFile(int inodeId, ByteBuffer buffer, int offset) throws JFSException {
        final int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);

        refuseIf(currentSize < offset, "requested offset is bigger than file size");
        refuseIf(offset + (long) buffer.capacity() > Parameters.MAX_FILE_SIZE, "operation will produce too big file");

        final int newSize = Math.max(currentSize, offset + buffer.capacity());

        final int blocksHave = InodeHelper.blocksForSize(currentSize);
        final int blocksNeed = InodeHelper.blocksForSize(newSize);

        if (blocksNeed > blocksHave) {
            growInode(inodeId, blocksNeed - blocksHave);
        }

        myAccessor.writeInodeInt(newSize, inodeId, InodeOffsets.OBJECT_SIZE);
        writeIntoFile(inodeId, buffer, offset);
    }

    @GuardedBy("myInodesLocks")
    private void tryRewriteFile(int inodeId, ByteBuffer buffer) throws JFSException {
        final int currentSize = myAccessor.readInodeInt(inodeId, InodeOffsets.OBJECT_SIZE);
        final int newSize = buffer.capacity();

        refuseIf(newSize > Parameters.MAX_FILE_SIZE, "operation will produce too big file");

        final int blocksHave = InodeHelper.blocksForSize(currentSize);
        final int blocksNeed = InodeHelper.blocksForSize(newSize);

        if (blocksNeed > blocksHave) {
            growInode(inodeId, blocksNeed - blocksHave);
        } else if (blocksNeed < blocksHave) {
            truncateInode(inodeId, blocksHave - blocksNeed);
        }

        writeIntoFile(inodeId, buffer, 0);
    }

    @NotNull
    @GuardedBy("myInodesLocks")
    private ArrayList<DirectoryEntry> getEntries(int inodeId) throws JFSException {
        assert (byte) (myAccessor.readInodeInt(inodeId, InodeOffsets.INODE_DESCRIPTION) >> 24) ==
                Parameters.typeToByte(Parameters.EntryType.DIRECTORY);
        AllocatedInode inode = new AllocatedInode(myAccessor.readInode(inodeId));
        ArrayList<DirectoryEntry> result = new ArrayList<DirectoryEntry>();

        for (int blockId : inode.directPointers) {
            if (blockId != 0) {
                result.addAll(new DirectoryBlock(myAccessor.readBlock(blockId)).entries);
            }
        }

        // TODO: indirect blocks
        return result;
    }

    @GuardedBy("myInodesLocks")
    private void removeFile(int inodeId, ArrayList<DirectoryEntry> siblings, DirectoryEntry entry) throws JFSException {
        assert entry.type == Parameters.EntryType.FILE;
        siblings.removeIf(e -> e.equals(entry));
        tryRewriteFile(inodeId, ByteBufferHelper.toBuffer(siblings));
        tryRewriteFile(entry.inodeId, ByteBuffer.allocate(0));
        myInodesStack.push(entry.inodeId);
    }

    @GuardedBy("myInodesLocks")
    private void removeDirectory(int inodeId, ArrayList<DirectoryEntry> siblings, DirectoryEntry entry) throws JFSException {
        assert entry.type == Parameters.EntryType.DIRECTORY;
        siblings.removeIf(e -> e.equals(entry));
        tryRewriteFile(inodeId, ByteBufferHelper.toBuffer(siblings));

        ArrayList<DirectoryEntry> entries = new ArrayList<DirectoryEntry>(1);
        entries.add(entry);

        while (!entries.isEmpty()) {
            DirectoryEntry lastEntry = entries.remove(entries.size() - 1);
            if (lastEntry.type == Parameters.EntryType.DIRECTORY) {
                if (lastEntry.name.equals(".") || lastEntry.name.equals("..")) {
                    continue;
                }

                entries.addAll(getEntries(lastEntry.inodeId));
            }

            tryRewriteFile(lastEntry.inodeId, ByteBuffer.allocate(0));
            myInodesStack.push(lastEntry.inodeId);
        }
    }

    @NotNull
    public DirectoryDescriptor rootInode() {
        return new DirectoryDescriptor(Parameters.ROOT_INODE_ID);
    }

    public void tryAddDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        String resolution = NameHelper.inspect(name);
        refuseIf(!resolution.isEmpty(), resolution);

        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            refuseIf(EntriesHelper.find(entries, name) != null, name + " is already in use");
            int newInodeId = allocateInode(new AllocatedInode(Parameters.EntryType.DIRECTORY, descriptor.inodeId));
            DirectoryBlock newDirectory = DirectoryBlock.emptyDirectoryBlock(newInodeId, descriptor.inodeId);
            tryRewriteFile(newInodeId, newDirectory.toBuffer());
            entries.add(new DirectoryEntry(newInodeId, Parameters.EntryType.DIRECTORY, name));
            tryRewriteFile(descriptor.inodeId, ByteBufferHelper.toBuffer(entries));
        } finally {
            writeLock.unlock();
        }
    }

    public void tryRemoveDirectory(DirectoryDescriptor descriptor, String name) throws JFSException {
        refuseIf(name.equals(".") || name.equals(".."), "cannot remove system directory");

        for (ReadWriteLock lock : myInodesLocks) {
            lock.writeLock().lock();
        }

        try {
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            DirectoryEntry toRemove = EntriesHelper.find(entries, name);
            refuseIf(toRemove == null, "no such file or directory");
            if (toRemove.type == Parameters.EntryType.FILE) {
                removeFile(descriptor.inodeId, entries, toRemove);
            } else {
                removeDirectory(descriptor.inodeId, entries, toRemove);
            }
        } finally {
            for (ReadWriteLock lock : myInodesLocks) {
                lock.writeLock().unlock();
            }
        }
    }

    public void tryAddFile(DirectoryDescriptor descriptor, String name) throws JFSException {
        String resolution = NameHelper.inspect(name);
        refuseIf(!resolution.isEmpty(), resolution);

        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            refuseIf(EntriesHelper.find(entries, name) != null, name + " is already in use");
            int newInodeId = allocateInode(new AllocatedInode(Parameters.EntryType.FILE, descriptor.inodeId));
            tryRewriteFile(newInodeId, ByteBuffer.allocate(0));
            entries.add(new DirectoryEntry(newInodeId, Parameters.EntryType.FILE, name));
            tryRewriteFile(descriptor.inodeId, ByteBufferHelper.toBuffer(entries));
        } finally {
            writeLock.unlock();
        }
    }

    public void tryRemoveFile(DirectoryDescriptor descriptor, String name) throws JFSException {
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            DirectoryEntry toRemove = EntriesHelper.find(entries, name);
            refuseIf(toRemove == null, "no such file");
            refuseIf(toRemove.type == Parameters.EntryType.DIRECTORY, "is a directory");
            removeFile(descriptor.inodeId, entries, toRemove);
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    public ByteBuffer tryReadFromFile(FileDescriptor descriptor, int offset, int maxLength) throws JFSException {
        Lock readLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].readLock();
        readLock.lock();

        try {
            assert (byte) (myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.INODE_DESCRIPTION) >> 24) ==
                    Parameters.typeToByte(Parameters.EntryType.FILE);
            return tryReadFromFile(descriptor.inodeId, offset, maxLength);
        } finally {
            readLock.unlock();
        }
    }

    public void tryWriteIntoFile(FileDescriptor descriptor, ByteBuffer buffer, int offset) throws JFSException {
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            assert (byte) (myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.INODE_DESCRIPTION) >> 24) ==
                    Parameters.typeToByte(Parameters.EntryType.FILE);
            tryWriteIntoFile(descriptor.inodeId, buffer, offset);
        } finally {
            writeLock.unlock();
        }
    }

    public int getFileSize(FileDescriptor descriptor) throws JFSException {
        Lock readLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].readLock();
        readLock.lock();

        try {
            assert (byte) (myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.INODE_DESCRIPTION) >> 24) ==
                    Parameters.typeToByte(Parameters.EntryType.FILE);
            return myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.OBJECT_SIZE);
        } finally {
            readLock.unlock();
        }
    }

    public void tryAppendToFile(FileDescriptor descriptor, ByteBuffer buffer) throws JFSException {
        Lock writeLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].writeLock();
        writeLock.lock();

        try {
            assert (byte) (myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.INODE_DESCRIPTION) >> 24) ==
                    Parameters.typeToByte(Parameters.EntryType.FILE);
            int offset = myAccessor.readInodeInt(descriptor.inodeId, InodeOffsets.OBJECT_SIZE);
            tryWriteIntoFile(descriptor.inodeId, buffer, offset);
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    public Map<String, DirectoryDescriptor>  getDirectories(DirectoryDescriptor descriptor) throws JFSException {
        Lock readLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].readLock();
        readLock.lock();

        try {
            Map<String, DirectoryDescriptor> result = new HashMap<String, DirectoryDescriptor>();
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            entries.forEach(entry -> {
                if (entry.type == Parameters.EntryType.DIRECTORY) {
                    result.put(entry.name, new DirectoryDescriptor(entry.inodeId));
                }
            });

            return result;
        } finally {
            readLock.unlock();
        }
    }

    @NotNull
    public Map<String, FileDescriptor> getFiles(DirectoryDescriptor descriptor) throws JFSException {
        Lock readLock = myInodesLocks[descriptor.inodeId % myInodesLocks.length].readLock();
        readLock.lock();

        try {
            Map<String, FileDescriptor> result = new HashMap<String, FileDescriptor>();
            ArrayList<DirectoryEntry> entries = getEntries(descriptor.inodeId);
            entries.forEach(entry -> {
                if (entry.type == Parameters.EntryType.FILE) {
                    result.put(entry.name, new FileDescriptor(entry.inodeId));
                }
            });

            return result;
        } finally {
            readLock.unlock();
        }
    }
}
