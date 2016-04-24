package org.kshmakov.jfs.driver.tools;

import org.kshmakov.jfs.io.Parameters;

public class InodeHelper {
//    public static int MAX_DIRECT_OFFSET = Parameters.DIRECT_POINTERS_NUMBER * Parameters.DATA_BLOCK_SIZE;
//    public static int MAX_DOUBLE_OFFSET = MAX_DIRECT_OFFSET
//            + Parameters.DATA_BLOCK_SIZE * Parameters.DATA_BLOCK_SIZE / 4;
//    public static long MAX_TRIPLE_OFFSET;
//
//    static {
//        MAX_TRIPLE_OFFSET = Parameters.DATA_BLOCK_SIZE;
//        MAX_TRIPLE_OFFSET *= Parameters.DATA_BLOCK_SIZE;
//        MAX_TRIPLE_OFFSET *= Parameters.DATA_BLOCK_SIZE;
//        MAX_TRIPLE_OFFSET /= 16;
//        MAX_TRIPLE_OFFSET += MAX_DOUBLE_OFFSET;
//    }

//    public static PointerType typeByOffset(int offset) {
//        assert offset >= 0;
//        if (offset < MAX_DIRECT_OFFSET) {
//            return PointerType.DIRECT;
//        } else if (offset < MAX_DOUBLE_OFFSET) {
//            return PointerType.DOUBLY_INDIRECT;
//        }
//
//        return PointerType.TRIPLY_INDIRECT;
//    }

//    public enum PointerType {
//        DIRECT,
//        DOUBLY_INDIRECT,
//        TRIPLY_INDIRECT
//    }

    public static int blocksForSize(int size) {
        assert size >= 0;
        return (int) (Parameters.DATA_BLOCK_SIZE - 1 + (long) size) / Parameters.DATA_BLOCK_SIZE;
    }

    public static int newBlocksNeeded(int currentSize, int offset, int bufferSize) {
        assert currentSize >= 0 && offset >= 0 && bufferSize >= 0;
        assert (long) offset + (long) bufferSize <= Integer.MAX_VALUE - Parameters.DATA_BLOCK_SIZE;
        int newSize = Math.max(currentSize, offset + bufferSize);
        int blocksUsed = (currentSize + Parameters.DATA_BLOCK_SIZE - 1) / Parameters.DATA_BLOCK_SIZE;
        int blocksNeeded = (newSize + Parameters.DATA_BLOCK_SIZE - 1) / Parameters.DATA_BLOCK_SIZE;
        // TODO: support doubly and triply indirect: calculate auxiliary blocks
        return blocksNeeded - blocksUsed;
    }

}
