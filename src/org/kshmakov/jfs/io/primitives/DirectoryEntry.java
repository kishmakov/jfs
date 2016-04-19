package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DirectoryEntry {
    private final static int ENTRY_HEADER_SIZE = 5;
    private final static String CHARSET = "UTF-8";

    public final static char SEPARATOR = '/';
    public final static int MAX_NAME_SIZE = 255;

    public static String checkName(String name) throws IOException {
        if (name.isEmpty()) {
            return "entry name must not be empty";
        }

        if (name.indexOf(SEPARATOR) != -1) {
            return "entry name must not contain separator character";
        }

        if (name.getBytes(CHARSET).length > MAX_NAME_SIZE) {
            return "entry name length limit exceeded";
        }

        return "";
    }

    public final int inodeId;
    public final String myName;

    private final byte[] myNameBytes;

    public DirectoryEntry(int inodeId, String name) throws IOException {
        this.inodeId = inodeId;
        String checkResult = checkName(name);
        if (!checkResult.isEmpty()) {
            throw new IOException(checkResult);
        }

        myName = name;
        myNameBytes = name.getBytes(CHARSET);
    }

    public DirectoryEntry(ByteBuffer buffer) throws UnsupportedEncodingException {
        assert buffer.position() + Parameters.INODE_SIZE <= buffer.capacity();
        inodeId = buffer.getInt();
        myNameBytes = new byte[buffer.get()];
        buffer.get(myNameBytes, 0, myNameBytes.length);
        myName = new String(myNameBytes, CHARSET);
    }

    public int size() {
        return ENTRY_HEADER_SIZE + myNameBytes.length;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(size());

        buffer.putInt(inodeId);
        buffer.put((byte) myNameBytes.length);
        buffer.put(myNameBytes);
        buffer.rewind();

        return buffer;
    }
}
