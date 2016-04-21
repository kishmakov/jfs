package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileSystemAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Header {
    public static final short FILE_SYSTEM_VERSION = 0x0000;
    public static final short DATA_BLOCK_SIZE = 0x1000;

    public int inodesTotal;
    public int blocksTotal;

    public int inodesUnallocated;
    public int blocksUnallocated;

    public int firstUnallocatedInodeId;
    public int firstUnallocatedBlockId;

    public Header() {
    }

    public Header(ByteBuffer buffer) throws IOException {
        assert buffer.capacity() == Parameters.HEADER_SIZE;
        buffer.rewind();

        if (buffer.getInt() != Parameters.MAGIC_NUMBER) {
            throw new IOException("provided file does not correspond to jfs format");
        }

        short version = buffer.getShort();
        short blockSize = buffer.getShort();

        assert version == FILE_SYSTEM_VERSION;
        assert blockSize == DATA_BLOCK_SIZE;

        inodesTotal = buffer.getInt();
        blocksTotal = buffer.getInt();

        inodesUnallocated = buffer.getInt();
        blocksUnallocated = buffer.getInt();

        firstUnallocatedInodeId = buffer.getInt();
        firstUnallocatedBlockId = buffer.getInt();
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(Parameters.HEADER_SIZE);

        buffer.putInt(Parameters.MAGIC_NUMBER);
        buffer.putShort(FILE_SYSTEM_VERSION);
        buffer.putShort(DATA_BLOCK_SIZE);

        buffer.putInt(inodesTotal);
        buffer.putInt(blocksTotal);

        buffer.putInt(inodesUnallocated);
        buffer.putInt(blocksUnallocated);

        buffer.putInt(firstUnallocatedInodeId);
        buffer.putInt(firstUnallocatedBlockId);

        return buffer;
    }
}
