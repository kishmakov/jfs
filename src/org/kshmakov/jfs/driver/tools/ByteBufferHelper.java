package org.kshmakov.jfs.driver.tools;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.DirectoryBlock;
import org.kshmakov.jfs.io.primitives.DirectoryEntry;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public interface ByteBufferHelper {
    static byte[] advance(ByteBuffer buffer, int maxLength) {
        int length = Math.min(buffer.remaining(), maxLength);
        byte[] result = new byte[length];
        buffer.get(result);
        return result;
    }

    static ByteBuffer toBuffer(ArrayList<DirectoryEntry> entries) throws JFSException {
        ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>();

        DirectoryBlock block = new DirectoryBlock();
        for (DirectoryEntry entry : entries) {
            if (!block.tryInsert(entry)) {
                buffers.add(block.toBuffer());
                block = new DirectoryBlock();
                boolean result = block.tryInsert(entry);
                assert result;
            }
        }

        buffers.add(block.toBuffer());

        int totalCapacity = 0;
        for (ByteBuffer buffer : buffers) {
            totalCapacity += buffer.capacity();
        }

        ByteBuffer result = ByteBuffer.allocate(totalCapacity);
        for (ByteBuffer buffer : buffers) {
            buffer.rewind();
            result.put(buffer);
        }

        return result;
    }
}
