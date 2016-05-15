package org.kshmakov.jfs.driver.tools;

import org.kshmakov.jfs.driver.DataFrame;
import org.kshmakov.jfs.driver.JFSException;
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

    static DataFrame toDataFrame(ArrayList<DirectoryEntry> entries) throws JFSException {
        ArrayList<byte[]> frames = new ArrayList<byte[]>();

        DirectoryBlock block = new DirectoryBlock();
        for (DirectoryEntry entry : entries) {
            if (!block.tryInsert(entry)) {
                frames.add(block.toBytes());
                block = new DirectoryBlock();
                boolean result = block.tryInsert(entry);
                assert result;
            }
        }

        frames.add(block.toBytes());

        int totalCapacity = 0;
        for (byte[] frame : frames) {
            totalCapacity += frame.length;
        }

        ByteBuffer result = ByteBuffer.allocate(totalCapacity);

        for (byte[] frame : frames) {
            result.put(frame);
        }

        return new DataFrame(result.array());
    }
}
