package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VacantBlock {
    public final int nextId;
    private final int size;

    public VacantBlock(int size, int nextId) {
        this.size = size;
        this.nextId = nextId;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(size);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putInt(nextId);

        buffer.rewind();
        return buffer;
    }
}
