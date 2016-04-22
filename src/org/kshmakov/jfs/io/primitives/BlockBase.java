package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;

public class BlockBase {
    private final byte[] bytes;

    public BlockBase(ByteBuffer buffer) {
        bytes = new byte[Parameters.DATA_BLOCK_SIZE];
        int maxLength = Math.min(bytes.length, buffer.capacity() - buffer.position());
        buffer.get(bytes, 0, maxLength);
    }

    public BlockBase(int nextId) {
        bytes = new byte[Parameters.DATA_BLOCK_SIZE];
        FileAccessor.newBuffer(bytes).putInt(nextId);
    }

    public ByteBuffer toBuffer() {
        return FileAccessor.newBuffer(bytes);
    }
}
