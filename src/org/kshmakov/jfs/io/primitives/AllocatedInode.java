package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AllocatedInode extends InodeBase {

    public final Parameters.EntryType type;
    public int parentId;
    public int objectSize;

    public int directPointers[] = new int[Parameters.DIRECT_POINTERS_NUMBER];
    public int singlyIndirectPointer;
    public int doublyIndirectPointer;

    public AllocatedInode(Parameters.EntryType type) {
        super(0);
        this.type = type;
    }

    public AllocatedInode(ByteBuffer buffer) {
        super(0);

        assert buffer.position() + Parameters.INODE_SIZE <= buffer.capacity();

        type = Parameters.byteToType(buffer.get());
        parentId = buffer.get();
        parentId = (parentId << 16) + buffer.getShort();

        objectSize = buffer.getInt();
        buffer.asIntBuffer().get(directPointers);
        buffer.position(buffer.position() + Parameters.DIRECT_POINTERS_NUMBER * 4);
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
        buffer.position(buffer.position() + Parameters.DIRECT_POINTERS_NUMBER * 4);
        buffer.putInt(singlyIndirectPointer);
        buffer.putInt(doublyIndirectPointer);

        buffer.rewind();
        return buffer;
    }
}
