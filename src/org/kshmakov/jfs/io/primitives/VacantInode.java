package org.kshmakov.jfs.io.primitives;

import java.nio.ByteBuffer;

public class VacantInode extends Inode {

    public int nextId;

    @Override
    protected void serializeTo(ByteBuffer buffer) {
        buffer.putInt(nextId);
    }
}
