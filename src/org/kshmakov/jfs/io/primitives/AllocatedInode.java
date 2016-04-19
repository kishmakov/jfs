package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AllocatedInode {

    public enum Type {
        DIRECTORY,
        FILE
    }

    public Type type;
    public int parentId;
    public int objectSize;

    public static final int DIRECT_POINTERS_NUMBER = 12;

    public int directPointers[];
    public int singlyIndirectPointer;
    public int doublyIndirectPointer;

    public AllocatedInode() {
        directPointers = new int[DIRECT_POINTERS_NUMBER];
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(Parameters.INODE_SIZE);
        assert buffer.order() == ByteOrder.BIG_ENDIAN;

        buffer.put((byte) type.ordinal());
        buffer.put((byte) (parentId >> 16));
        buffer.putShort((short) (parentId & 0xFFFF));
        buffer.putInt(objectSize);
        buffer.asIntBuffer().put(directPointers);
        buffer.position(buffer.position() + DIRECT_POINTERS_NUMBER * 4);
        buffer.putInt(singlyIndirectPointer);
        buffer.putInt(doublyIndirectPointer);

        buffer.rewind();
        return buffer;
    }
}
