package org.kshmakov.io.primitives;

import org.kshmakov.io.Parameters;

import java.nio.ByteBuffer;

public class Header {
    public static final short FILE_SYSTEM_VERSION = 0x0000;
    public static final short DATA_BLOCK_SIZE = 0x1000;

    public int inodesTotal;
    public int blocksTotal;

    public int inodesUnallocated;
    public int blocksUnallocated;

    public int firstUnallocatedInode;
    public int firstUnallocatedBlock;

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(Parameters.HEADER_SIZE);
        buffer.order(Parameters.ENDIANNESS);

        buffer.putInt(Parameters.MAGIC_NUMBER);
        buffer.putShort(FILE_SYSTEM_VERSION);
        buffer.putShort(DATA_BLOCK_SIZE);

        buffer.putInt(inodesTotal);
        buffer.putInt(blocksTotal);

        buffer.putInt(inodesUnallocated);
        buffer.putInt(blocksUnallocated);

        buffer.putInt(firstUnallocatedInode);
        buffer.putInt(firstUnallocatedBlock);

        return buffer;
    }
}
