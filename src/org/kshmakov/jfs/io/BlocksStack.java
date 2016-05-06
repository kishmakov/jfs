package org.kshmakov.jfs.io;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.driver.tools.DriverHelper;

import java.util.ArrayList;

@ThreadSafe
public class BlocksStack {
    private FileAccessor myAccessor;
    private int myUnallocatedBlocks;
    private int myFirstUnallocatedId;

    private final Object myLock = new Object();

    public BlocksStack(FileAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedBlocks = myAccessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        myFirstUnallocatedId = myAccessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
    }

    @GuardedBy("myLock")
    private int popOne() throws JFSException {
        final int resultId = myFirstUnallocatedId;
        myFirstUnallocatedId = myAccessor.readBlockInt(resultId);
        myAccessor.writeHeaderInt(myFirstUnallocatedId, HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
        myAccessor.writeHeaderInt(--myUnallocatedBlocks, HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        return resultId;
    }

    @GuardedBy("myLock")
    private void pushOne(int blockId) throws JFSException {
        myAccessor.writeBlockInt(myFirstUnallocatedId, blockId);
        myAccessor.writeHeaderInt(blockId, HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
        myAccessor.writeHeaderInt(++myUnallocatedBlocks, HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        myFirstUnallocatedId = blockId;
    }

    public ArrayList<Integer> pop(int amount) throws JFSException {
        synchronized (myLock) {
            DriverHelper.refuseIf(myUnallocatedBlocks < amount, "not enough unallocated blocks for requested operation");
            ArrayList<Integer> result = new ArrayList<Integer>(amount);
            while (amount-- > 0) {
                result.add(popOne());
            }
            return result;
        }
    }

    public void push(ArrayList<Integer> ids) throws JFSException {
        synchronized (myLock) {
            for (int id : ids) {
                pushOne(id);
            }
        }
    }
}
