package org.kshmakov.jfs.io.primitives;

import java.nio.ByteBuffer;

public class VacantInode extends Inode {

    public final int nextId;

    public VacantInode(int nextId) {
        this.nextId = nextId;
    }

    @Override
    protected void serializeTo(ByteBuffer buffer) {
        buffer.putInt(nextId);
    }
}
