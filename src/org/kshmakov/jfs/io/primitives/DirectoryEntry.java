package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileSystemAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DirectoryEntry {
    private final static short ENTRY_HEADER_SIZE = 6;
    private final static String CHARSET = "UTF-8";

    public final static short MAX_NAME_SIZE = 255;

    public static String checkName(String name) throws IOException {
        if (name.isEmpty()) {
            return "entry name must not be empty";
        }

        if (name.indexOf(Parameters.SEPARATOR) != -1) {
            return "entry name must not contain separator character";
        }

        if (name.getBytes(CHARSET).length > MAX_NAME_SIZE) {
            return "entry name length limit exceeded";
        }

        return "";
    }

    public final int inodeId;
    public final Parameters.EntryType type;
    public final String name;

    private final byte[] myNameBytes;

    public DirectoryEntry(int inodeId, Parameters.EntryType type, String name) throws IOException {
        this.inodeId = inodeId;
        this.type = type;
        String checkResult = checkName(name);
        if (!checkResult.isEmpty()) {
            throw new IOException(checkResult);
        }

        this.name = name;
        myNameBytes = name.getBytes(CHARSET);
    }

    public DirectoryEntry(ByteBuffer buffer) throws UnsupportedEncodingException {
        inodeId = buffer.getInt();
        type = Parameters.byteToType(buffer.get());
        myNameBytes = new byte[buffer.get()];
        buffer.get(myNameBytes, 0, myNameBytes.length);
        name = new String(myNameBytes, CHARSET);
    }

    public short size() {
        return (short) (ENTRY_HEADER_SIZE + myNameBytes.length);
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(size());

        buffer.putInt(inodeId);
        buffer.put(Parameters.typeToByte(type));
        buffer.put((byte) myNameBytes.length);
        buffer.put(myNameBytes);
        buffer.rewind();

        return buffer;
    }
}
