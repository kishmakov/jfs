package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileSystemAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InodeBase {
    public final int nextId;

    public InodeBase(int nextId) {
        this.nextId = nextId;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(Parameters.INODE_SIZE);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.putInt(nextId);

        buffer.rewind();
        return buffer;
    }
}
