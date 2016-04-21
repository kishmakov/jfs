package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

@NotThreadSafe
public class BlocksStack {
    private FileSystemAccessor myAccessor;
    private int myUnallocatedBlocks;
    private int myFirstUnallocatedId;

    public BlocksStack(FileSystemAccessor accessor) throws JFSBadFileException {
        myAccessor = accessor;
        myUnallocatedBlocks = myAccessor.readInt(Offsets.TOTAL_UNALLOCATED_BLOCKS);
        myFirstUnallocatedId = myAccessor.readInt(Offsets.FIRST_UNALLOCATED_BLOCK_ID);
    }

    public int size() {
        return myUnallocatedBlocks;
    }

    public int pop() throws JFSException {
        assert size() > 0;
        int resultId = myFirstUnallocatedId;

        myFirstUnallocatedId = myAccessor.readInt(myAccessor.blockOffset(resultId));
        myAccessor.writeInt(Offsets.FIRST_UNALLOCATED_BLOCK_ID, myFirstUnallocatedId);
        myAccessor.writeInt(Offsets.TOTAL_UNALLOCATED_BLOCKS, --myUnallocatedBlocks);

        return resultId;
    }

    public void push(int blockId) throws JFSException {
        myAccessor.writeInt(myAccessor.blockOffset(blockId), myFirstUnallocatedId);
        myAccessor.writeInt(Offsets.FIRST_UNALLOCATED_BLOCK_ID, blockId);
        myAccessor.writeInt(Offsets.TOTAL_UNALLOCATED_BLOCKS, ++myUnallocatedBlocks);
        myFirstUnallocatedId = blockId;
    }

}
