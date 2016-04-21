package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.Header;

import java.nio.ByteBuffer;

@NotThreadSafe
public class BlocksStack {
    private FileSystemAccessor myAccessor;
    private FileSystemLocator myLocator;
    private int myUnallocatedBlocks;
    private int myFirstUnallocatedId;

    public BlocksStack(FileSystemAccessor accessor, FileSystemLocator locator) throws JFSBadFileException {
        myAccessor = accessor;
        myLocator = locator;
        Header header = accessor.header();
        myUnallocatedBlocks = header.blocksUnallocated;
        myFirstUnallocatedId = header.firstUnallocatedBlockId;
    }

    public int size() {
        return myUnallocatedBlocks;
    }

    public int pop() throws JFSException {
        assert size() > 0;
        int resultId = myFirstUnallocatedId;

        ByteBuffer buffer = myAccessor.readBuffer(myLocator.blockOffset(resultId) , 4);

        myFirstUnallocatedId = buffer.getInt();
        myAccessor.writeBuffer(buffer, Header.FIRST_UNALLOCATED_BLOCK_ID_OFFSET);
        buffer.rewind();
        buffer.putInt(--myUnallocatedBlocks);
        myAccessor.writeBuffer(buffer, Header.TOTAL_UNALLOCATED_BLOCKS_OFFSET);

        return resultId;
    }

    public void push(int blockId) throws JFSException {
        ByteBuffer buffer = FileSystemAccessor.newBuffer(4);
        buffer.putInt(myFirstUnallocatedId);
        myAccessor.writeBuffer(buffer, myLocator.blockOffset(blockId));

        myFirstUnallocatedId = blockId;

        buffer.rewind();
        buffer.putInt(++myUnallocatedBlocks);
        myAccessor.writeBuffer(buffer, Header.TOTAL_UNALLOCATED_BLOCKS_OFFSET);

        buffer.rewind();
        buffer.putInt(blockId);
        myAccessor.writeBuffer(buffer, Header.FIRST_UNALLOCATED_BLOCK_ID_OFFSET);
    }

}
