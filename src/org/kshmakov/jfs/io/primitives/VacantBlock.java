package org.kshmakov.jfs.io.primitives;

import java.nio.ByteBuffer;

public class VacantBlock extends BlockBase {
    public final int nextId;

    public VacantBlock(int size, int nextId) {
        super(size);
        this.nextId = nextId;
    }

    @Override
    public ByteBuffer toBuffer() {
        ByteBuffer buffer = super.toBuffer();
        buffer.putInt(nextId);

        buffer.rewind();
        return buffer;
    }
}
