package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;

public abstract class Inode {
    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(Parameters.INODE_SIZE);
        serializeTo(buffer);
        return buffer;
    }

    protected abstract void serializeTo(ByteBuffer buffer);
}
