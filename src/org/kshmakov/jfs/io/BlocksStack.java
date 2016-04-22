package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

@NotThreadSafe
public class BlocksStack {
    private FileAccessor myAccessor;
    private int myUnallocatedBlocks;
    private int myFirstUnallocatedId;

    public BlocksStack(FileAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedBlocks = myAccessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        myFirstUnallocatedId = myAccessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
    }

    public int size() {
        return myUnallocatedBlocks;
    }

    public int pop() throws JFSException {
        assert size() > 0;
        int resultId = myFirstUnallocatedId;

        myFirstUnallocatedId = myAccessor.readBlockInt(resultId);
        myAccessor.writeHeaderInt(myFirstUnallocatedId, HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
        myAccessor.writeHeaderInt(--myUnallocatedBlocks, HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);

        return resultId;
    }

    public void push(int blockId) throws JFSException {
        myAccessor.writeBlockInt(myFirstUnallocatedId, blockId);
        myAccessor.writeHeaderInt(blockId, HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID);
        myAccessor.writeHeaderInt(++myUnallocatedBlocks, HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS);
        myFirstUnallocatedId = blockId;
    }

}
