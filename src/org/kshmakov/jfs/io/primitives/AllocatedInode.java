package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.JFSBadFileException;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AllocatedInode extends InodeBase {

    public final Parameters.EntryType type;
    public final int parentId;
    public int objectSize;

    public int directPointers[] = new int[Parameters.DIRECT_POINTERS_NUMBER];
    public int singlyIndirectPointer;
    public int doublyIndirectPointer;

    public AllocatedInode(Parameters.EntryType type, int parentId) {
        super(0);
        this.type = type;
        this.parentId = parentId;
    }

    public AllocatedInode(ByteBuffer buffer) throws JFSBadFileException {
        super(0);
        assert buffer.position() + Parameters.INODE_SIZE <= buffer.capacity();

        type = Parameters.byteToType(buffer.get());
        int hightPart = (int) buffer.get() & 0xFF;
        parentId = (hightPart << 16) + (int) buffer.getShort() & 0xFFFF;

        objectSize = buffer.getInt();
        buffer.asIntBuffer().get(directPointers);
        buffer.position(buffer.position() + Parameters.DIRECT_POINTERS_NUMBER * 4);
        buffer.getInt(singlyIndirectPointer);
        buffer.getInt(doublyIndirectPointer);
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
