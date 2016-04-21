package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.Header;

import java.nio.ByteBuffer;

@NotThreadSafe
public class InodesStack {
    private FileSystemAccessor myAccessor;
    private FileSystemLocator myLocator;
    private int myUnallocatedInodes;
    private int myFirstUnallocatedId;

    public InodesStack(FileSystemAccessor accessor, FileSystemLocator locator) throws JFSBadFileException {
        myAccessor = accessor;
        myLocator = locator;
        Header header = accessor.header();
        myUnallocatedInodes = header.inodesUnallocated;
        myFirstUnallocatedId = header.firstUnallocatedInodeId;
    }

    public boolean empty() {
        return myUnallocatedInodes == 0;
    }

    public int pop() throws JFSException {
        assert !empty();
        int resultId = myFirstUnallocatedId;

        ByteBuffer buffer = myAccessor.readBuffer(myLocator.inodeOffset(resultId) , 4);

        myFirstUnallocatedId = buffer.getInt();
        myAccessor.writeBuffer(buffer, Header.FIRST_UNALLOCATED_INODE_ID_OFFSET);
        buffer.rewind();
        buffer.putInt(--myUnallocatedInodes);
        myAccessor.writeBuffer(buffer, Header.TOTAL_UNALLOCATED_INODES_OFFSET);

        return resultId;
    }

    public void push(int inodeId) throws JFSException {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(4);
        buffer.putInt(myFirstUnallocatedId);
        myAccessor.writeBuffer(buffer, myLocator.inodeOffset(inodeId));

        myFirstUnallocatedId = inodeId;

        buffer.rewind();
        buffer.putInt(++myUnallocatedInodes);
        myAccessor.writeBuffer(buffer, Header.TOTAL_UNALLOCATED_INODES_OFFSET);

        buffer.rewind();
        buffer.putInt(inodeId);
        myAccessor.writeBuffer(buffer, Header.FIRST_UNALLOCATED_INODE_ID_OFFSET);
    }
}
