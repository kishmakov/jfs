package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;
import org.kshmakov.jfs.io.NameHelper;

import java.nio.ByteBuffer;

public class DirectoryEntry {
    private final static short ENTRY_HEADER_SIZE = 6;

    public final int inodeId;
    public final Parameters.EntryType type;
    public final String name;

    private final byte[] myNameBytes;

    public DirectoryEntry(int inodeId, Parameters.EntryType type, String name) throws JFSException {
        this.inodeId = inodeId;
        this.type = type;
        this.name = name;
        myNameBytes = NameHelper.toBytes(name);
    }

    public DirectoryEntry(ByteBuffer buffer) throws JFSException {
        inodeId = buffer.getInt();
        type = Parameters.byteToType(buffer.get());
        myNameBytes = new byte[(int) buffer.get() & 0xFF];
        buffer.get(myNameBytes, 0, myNameBytes.length);
        name = NameHelper.fromBytes(myNameBytes);
    }

    public short size() {
        return (short) (ENTRY_HEADER_SIZE + myNameBytes.length);
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(size());

        buffer.putInt(inodeId);
        buffer.put(Parameters.typeToByte(type));
        buffer.put((byte) myNameBytes.length);
        buffer.put(myNameBytes);
        buffer.rewind();

        return buffer;
    }
}
