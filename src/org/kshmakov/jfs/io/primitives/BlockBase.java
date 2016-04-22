package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BlockBase {
    public final int nextId;

    public BlockBase(int nextId) {
        this.nextId = nextId;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(Parameters.DATA_BLOCK_SIZE);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putInt(nextId);

        buffer.rewind();
        return buffer;
    }
}
