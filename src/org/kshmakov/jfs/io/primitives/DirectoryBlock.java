package org.kshmakov.jfs.io.primitives;

import org.kshmakov.jfs.driver.JFSException;
import org.kshmakov.jfs.io.Parameters;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DirectoryBlock extends BlockBase {
    private final static short HEADER_SIZE = 2;

    private short myUnusedSize;

    public final ArrayList<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();

    public static DirectoryBlock emptyDirectoryBlock(int currentId, int parentId) throws JFSException {
        DirectoryBlock block = new DirectoryBlock();
        block.tryInsert(new DirectoryEntry(currentId, Parameters.EntryType.DIRECTORY, "."));
        block.tryInsert(new DirectoryEntry(parentId, Parameters.EntryType.DIRECTORY, ".."));
        return block;
    }

    public DirectoryBlock() {
        super(0);
        myUnusedSize = (short) (Parameters.DATA_BLOCK_SIZE - HEADER_SIZE);
    }

    public DirectoryBlock(byte[] bytes) throws JFSException {
        super(bytes);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        myUnusedSize = buffer.getShort();

        while (buffer.position() + myUnusedSize < Parameters.DATA_BLOCK_SIZE) {
            entries.add(new DirectoryEntry(buffer));
        }
    }

    public boolean tryInsert(DirectoryEntry entry) {
        if (entry.size() > myUnusedSize)
            return false;

        entries.add(entry);
        myUnusedSize -= entry.size();

        return true;
    }

    @Override
    public byte[] toBytes() {
        byte[] result = super.toBytes();

        ByteBuffer buffer = ByteBuffer.wrap(result);

        buffer.putShort(myUnusedSize);
        entries.forEach(item -> buffer.put(item.toBytes()));

        return result;
    }
}
