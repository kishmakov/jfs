package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.FileSystemAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DirectoryBlock extends Block {
    private final static short HEADER_SIZE = 2;

    private short myUnusedSize;

    public final ArrayList<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();

    public static DirectoryBlock emptyDirectoryBlock() throws JFSException {
        DirectoryBlock block = new DirectoryBlock(Header.DATA_BLOCK_SIZE);
        block.tryInsert(new DirectoryEntry(1, Parameters.EntryType.DIRECTORY, "."));
        block.tryInsert(new DirectoryEntry(1, Parameters.EntryType.DIRECTORY, ".."));
        return block;
    }

    public DirectoryBlock(int size) {
        super(size);
        myUnusedSize = (short) (size - HEADER_SIZE);
    }

    public DirectoryBlock(ByteBuffer buffer) throws JFSException {
        super(buffer.capacity());
        myUnusedSize = buffer.getShort();

        while (buffer.position() + myUnusedSize < mySize) {
            entries.add(new DirectoryEntry(buffer));
        }
    }

    public boolean tryInsert(DirectoryEntry entry) {
        if (entry.size() > myUnusedSize)
            return false;

        entries.add(entry);
        myUnusedSize -= entry.size();

        return true;
    }

    @Override
    public ByteBuffer toBuffer() {
        ByteBuffer buffer = super.toBuffer();

        buffer.putShort(myUnusedSize);
        entries.forEach(item -> buffer.put(item.toBuffer()));

        buffer.rewind();
        return buffer;
    }
}
