package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileSystemAccessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Block {
    protected final int mySize;

    Block(int size) {
        this.mySize = size;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(mySize);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;
        return buffer;
    }
}
