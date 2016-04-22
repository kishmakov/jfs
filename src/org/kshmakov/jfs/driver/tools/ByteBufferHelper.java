package org.kshmakov.jfs.driver.tools;

import java.nio.ByteBuffer;

public interface ByteBufferHelper {
    static byte[] advance(ByteBuffer buffer, int maxLength) {
        int length = Math.min(buffer.remaining(), maxLength);
        byte[] result = new byte[length];
        buffer.get(result);
        return result;
    }
}
