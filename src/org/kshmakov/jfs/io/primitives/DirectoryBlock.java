package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DirectoryBlock {
    private final static short HEADER_SIZE = 2;

    private final short myTotalSize;
    private short myUnusedSize;

    public final ArrayList<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();

    public DirectoryBlock(short size) {
        myTotalSize = size;
        myUnusedSize = (short) (size - HEADER_SIZE);
    }

    public DirectoryBlock(ByteBuffer buffer) throws UnsupportedEncodingException {
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
        ByteBuffer buffer = FileAccessor.newBuffer(myTotalSize);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putShort(myUnusedSize);

        for (DirectoryEntry entry: entries) {
            buffer.put(entry.toBuffer());
        }

        buffer.rewind();
        return buffer;
    }
}
