package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DirectoryBlock {
    private final static short HEADER_SIZE = 2;

    private final short totalSize;
    private short unusedSize;

    private ArrayList<DirectoryEntry> entries;

    public DirectoryBlock(short size) {
        this.totalSize = size;
        entries = new ArrayList<DirectoryEntry>();
        unusedSize = (short) (size - HEADER_SIZE);
    }

    public boolean tryInsert(DirectoryEntry entry) {
        if (entry.size() > unusedSize)
            return false;

        entries.add(entry);
        unusedSize -= entry.size();

        return true;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(totalSize);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putShort(unusedSize);

        for (DirectoryEntry entry: entries) {
            buffer.put(entry.toBuffer());
        }

        buffer.rewind();
        return buffer;
    }
}
