package org.kshmakov.jfs.io;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.driver.tools.DriverHelper;
import org.kshmakov.jfs.io.primitives.InodeBase;

@ThreadSafe
public class InodesStack {
    private FileAccessor myAccessor;
    private int myUnallocatedInodes;
    private int myFirstUnallocatedId;

    private final Object myLock = new Object();

    public InodesStack(FileAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedInodes = accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES);
        myFirstUnallocatedId = accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
    }

    @GuardedBy("myLock")
    private boolean empty() {
        return myUnallocatedInodes == 0;
    }

    public int pop(InodeBase inode) throws JFSException {
        synchronized (myLock) {
            DriverHelper.refuseIf(empty(), "no unallocated inodes left");

            int resultId = myFirstUnallocatedId;

            myFirstUnallocatedId = myAccessor.readInodeInt(resultId, InodeOffsets.NEXT_INODE);
            myAccessor.writeHeaderInt(myFirstUnallocatedId, HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
            myAccessor.writeHeaderInt(--myUnallocatedInodes, HeaderOffsets.TOTAL_UNALLOCATED_INODES);

            myAccessor.writeInode(inode, resultId);

            return resultId;
        }
    }

    public void push(int inodeId) throws JFSException {
        synchronized (myLock) {
            myAccessor.writeInodeInt(myFirstUnallocatedId, inodeId, InodeOffsets.NEXT_INODE);
            myAccessor.writeHeaderInt(inodeId, HeaderOffsets.FIRST_UNALLOCATED_INODE_ID);
            myAccessor.writeHeaderInt(++myUnallocatedInodes, HeaderOffsets.TOTAL_UNALLOCATED_INODES);
            myFirstUnallocatedId = inodeId;
        }
    }
}
