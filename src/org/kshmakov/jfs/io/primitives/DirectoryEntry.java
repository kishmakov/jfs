package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.io.FileAccessor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DirectoryEntry {
    private final static int ENTRY_HEADER_SIZE = 5;
    private final static String CHARSET = "UTF-8";

    public final static char SEPARATOR = '/';
    public final static int MAX_NAME_SIZE = 255;

    public int inodeId;

    private String myName;
    private byte[] myNameBytes;

    public DirectoryEntry(int inodeId, String name) throws IOException {
        this.inodeId = inodeId;
        setName(name);
    }

    public void setName(String name) throws IOException {
        if (name.isEmpty()) {
            throw new IOException("directory name could not be empty");
        }

        if (name.indexOf(SEPARATOR) != -1) {
            throw new IOException("invalid name: contains separator character");
        }

        try {
            myNameBytes = name.getBytes(CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("could not handle " + CHARSET);
        }

        if (myNameBytes.length > MAX_NAME_SIZE) {
            throw new IOException("directory name length limit exceeded");
        }

        this.myName = name;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = FileAccessor.newBuffer(ENTRY_HEADER_SIZE + myName.length());

        buffer.putInt(inodeId);
        buffer.put((byte) myNameBytes.length);
        buffer.put(myNameBytes);
        buffer.rewind();

        return buffer;
    }
}
