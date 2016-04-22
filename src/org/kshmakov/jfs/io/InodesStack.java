package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

@NotThreadSafe
public class InodesStack {
    private FileAccessor myAccessor;
    private int myUnallocatedInodes;
    private int myFirstUnallocatedId;

    public InodesStack(FileAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedInodes = accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES);
        myFirstUnallocatedId = accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
    }

    public boolean empty() {
        return myUnallocatedInodes == 0;
    }

    public int pop() throws JFSException {
        assert !empty();
        int resultId = myFirstUnallocatedId;

        myFirstUnallocatedId = myAccessor.readInodeInt(resultId, InodeOffsets.NEXT_INODE);
        myAccessor.writeHeaderInt(myFirstUnallocatedId, HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
        myAccessor.writeHeaderInt(--myUnallocatedInodes, HeaderOffsets.TOTAL_UNALLOCATED_INODES);

        return resultId;
    }

    public void push(int inodeId) throws JFSException {
        myAccessor.writeInodeInt(myFirstUnallocatedId, inodeId, InodeOffsets.NEXT_INODE);
        myAccessor.writeHeaderInt(inodeId, HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
        myAccessor.writeHeaderInt(++myUnallocatedInodes, HeaderOffsets.TOTAL_UNALLOCATED_INODES);
        myFirstUnallocatedId = inodeId;
    }
}
