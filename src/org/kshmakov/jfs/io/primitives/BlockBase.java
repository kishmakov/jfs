package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

public class BlockBase {
    private final byte[] bytes;

    public BlockBase(byte[] bytes) {
        assert bytes.length == Parameters.DATA_BLOCK_SIZE;
        this.bytes = bytes;
    }

    public BlockBase(int nextId) {
        bytes = new byte[Parameters.DATA_BLOCK_SIZE];
        FileAccessor.newBuffer(bytes).putInt(nextId);
    }

    public byte[] toBytes() {
        return bytes;
    }
}
