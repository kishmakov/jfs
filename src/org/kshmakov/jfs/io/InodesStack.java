package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

@NotThreadSafe
public class InodesStack {
    private FileSystemAccessor myAccessor;    
    private int myUnallocatedInodes;
    private int myFirstUnallocatedId;

    public InodesStack(FileSystemAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedInodes = accessor.readInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES);
        myFirstUnallocatedId = accessor.readInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
    }

    public boolean empty() {
        return myUnallocatedInodes == 0;
    }

    public int pop() throws JFSException {
        assert !empty();
        int resultId = myFirstUnallocatedId;

        myFirstUnallocatedId = myAccessor.readInt(myAccessor.inodeOffset(resultId));
        myAccessor.writeInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID, myFirstUnallocatedId);
        myAccessor.writeInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES, --myUnallocatedInodes);

        return resultId;
    }

    public void push(int inodeId) throws JFSException {
        myAccessor.writeInt(myAccessor.inodeOffset(inodeId), myFirstUnallocatedId);
        myAccessor.writeInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID, inodeId);
        myAccessor.writeInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES, ++myUnallocatedInodes);
        myFirstUnallocatedId = inodeId;
    }
}
