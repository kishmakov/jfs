package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.FileSystemAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DirectoryBlock {
    private final static short HEADER_SIZE = 2;

    private final short myTotalSize;
    private short myUnusedSize;

    public final ArrayList<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();

    public static DirectoryBlock emptyDirectoryBlock() throws JFSException {
        DirectoryBlock block = new DirectoryBlock(Header.DATA_BLOCK_SIZE);
        block.tryInsert(new DirectoryEntry(1, Parameters.EntryType.DIRECTORY, "."));
        block.tryInsert(new DirectoryEntry(1, Parameters.EntryType.DIRECTORY, ".."));
        return block;
    }

    public DirectoryBlock(short size) {
        myTotalSize = size;
        myUnusedSize = (short) (size - HEADER_SIZE);
    }

    public DirectoryBlock(ByteBuffer buffer) throws JFSException {
        myTotalSize = (short) buffer.capacity();
        myUnusedSize = buffer.getShort();

        while (buffer.position() + myUnusedSize < myTotalSize) {
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

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(myTotalSize);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putShort(myUnusedSize);

        for (DirectoryEntry entry: entries) {
            buffer.put(entry.toBuffer());
        }

        buffer.rewind();
        return buffer;
    }
}
