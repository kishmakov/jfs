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

    public AllocatedInode(ByteBuffer buffer) {
        assert buffer.position() + Parameters.INODE_SIZE <= buffer.capacity();
        directPointers = new int[DIRECT_POINTERS_NUMBER];

        byte typeByte = buffer.get();
        type = typeByte == 0 ? Type.DIRECTORY : Type.FILE;
        parentId = buffer.get();
        parentId = (parentId << 16) + buffer.getShort();

        objectSize = buffer.getInt();
        buffer.asIntBuffer().get(directPointers);
        buffer.position(buffer.position() + DIRECT_POINTERS_NUMBER * 4);
        buffer.getInt(singlyIndirectPointer);
        buffer.getInt(doublyIndirectPointer);

        System.out.printf("parentId = %d\nobjectSize = %d\n", parentId, objectSize);
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
