package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.io.primitives.BlockBase;
import org.kshmakov.jfs.io.primitives.InodeBase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

@NotThreadSafe
abstract public class FileAccessorBase {
    protected final FileChannel myChannel;
    protected final RandomAccessFile myFile;

    public final long fileSize;

    protected final int myTotalInodes;
    protected final int myTotalBlocks;

    abstract protected int getTotalInodes() throws JFSBadFileException;

    abstract protected int getTotalBlocks() throws JFSBadFileException;

    static public ByteBuffer newBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer;
    }

    static public ByteBuffer newBuffer(byte[] bytes) {
        ByteBuffer result = ByteBuffer.wrap(bytes);
        result.order(ByteOrder.BIG_ENDIAN);
        return result;
    }

    protected FileAccessorBase(String fileName) throws JFSBadFileException {
        try {
            myFile = new RandomAccessFile(fileName, "rw");
            myChannel = myFile.getChannel();
            fileSize = myChannel.size();

            if (fileSize < Parameters.MIN_SIZE || fileSize > Parameters.MAX_SIZE) {
                String range = "[" + Long.toString(Parameters.MIN_SIZE) + ", " + Long.toString(Parameters.MAX_SIZE) + "]";
                throw new JFSBadFileException("file size of " + fileName + " is not in range " + range);
            }

            myTotalInodes = getTotalInodes();
            myTotalBlocks = getTotalBlocks();
        } catch (FileNotFoundException e) {
            throw new JFSBadFileException("file " + fileName + " not found");
        } catch (IOException e) {
            throw new JFSBadFileException("problems while accessing file " + fileName + ": " + e.getMessage());
        }
    }

    public int readHeaderInt(byte inHeaderOffset) throws JFSBadFileException {
        return readInt(inHeaderOffset);
    }

    public void writeHeaderInt(int number, byte inHeaderOffset) throws JFSBadFileException {
        writeInt(number, inHeaderOffset);
    }

    public int readInodeInt(int inodeId, byte inInodeOffset) throws JFSException {
        return readInt(inodeOffset(inodeId) + inInodeOffset);
    }

    public void writeInodeInt(int number, int inodeId, byte inInodeOffset) throws JFSException {
        writeInt(number, inodeOffset(inodeId) + inInodeOffset);
    }

    public int readBlockInt(int blockId) throws JFSException {
        return readInt(blockOffset(blockId));
    }

    public void writeBlockInt(int number, int blockId) throws JFSException {
        writeInt(number, blockOffset(blockId));
    }

    public ByteBuffer readBlock(int blockId) throws JFSException {
        try {
            ByteBuffer buffer = newBuffer(Parameters.DATA_BLOCK_SIZE);
            myChannel.read(buffer, blockOffset(blockId));
            buffer.rewind();
            return buffer;
        } catch (IOException e) {
            throw new JFSBadFileException("could not read buffer from file: " + e.getMessage());
        }
    }

    public void writeBlock(BlockBase block, int blockId) throws JFSException {
        try {
            ByteBuffer buffer = block.toBuffer();
            buffer.flip();
            buffer.limit(buffer.capacity());
            assert blockOffset(blockId) + buffer.capacity() <= fileSize;
            int result = myChannel.write(buffer, blockOffset(blockId));
            assert result == buffer.capacity();
        } catch (IOException e) {
            throw new JFSBadFileException("could not write buffer to file: " + e.getMessage());
        }
    }

    public ByteBuffer readInode(int inodeId) throws JFSException {
        try {
            ByteBuffer buffer = newBuffer(Parameters.INODE_SIZE);
            myChannel.read(buffer, inodeOffset(inodeId));
            buffer.rewind();
            return buffer;
        } catch (IOException e) {
            throw new JFSBadFileException("could not read buffer from file: " + e.getMessage());
        }
    }

    public void writeInode(InodeBase inode, int blockId) throws JFSException {
        try {
            ByteBuffer buffer = inode.toBuffer();
            buffer.flip();
            buffer.limit(buffer.capacity());
            assert inodeOffset(blockId) + buffer.capacity() <= fileSize;
            int result = myChannel.write(buffer, inodeOffset(blockId));
            assert result == buffer.capacity();
        } catch (IOException e) {
            throw new JFSBadFileException("could not write buffer to file: " + e.getMessage());
        }
    }

    private int readInt(long position) throws JFSBadFileException {
        try {
            assert position + 4 <= fileSize;
            ByteBuffer buffer = newBuffer(4);
            myChannel.read(buffer, position);
            buffer.rewind();
            return buffer.getInt();
        } catch (IOException e) {
            throw new JFSBadFileException("could not read int from file: " + e.getMessage());
        }
    }

    private void writeInt(int number, long position) throws JFSBadFileException {
        try {
            ByteBuffer buffer = newBuffer(4);
            buffer.putInt(number);
            buffer.flip();
            buffer.limit(4);
            assert position + 4 <= fileSize;
            int result = myChannel.write(buffer, position);
            assert result == 4;
        } catch (IOException e) {
            throw new JFSBadFileException("could not write int to file: " + e.getMessage());
        }
    }

    private long inodeOffset(int inodeId) throws JFSException {

        if (inodeId <= 0 || inodeId > myTotalInodes) {
            String range = "[1; " + Integer.toString(myTotalInodes) + "]";
            throw new JFSException("inodeId=" + Integer.toString(inodeId) + " not in " + range);
        }

        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    private long blockOffset(int blockId) throws JFSException {
        if (blockId <= 0 || blockId > myTotalBlocks) {
            String range = "[1; " + Integer.toString(myTotalBlocks) + "]";
            throw new JFSException("blockId=" + Integer.toString(blockId) + " not in " + range);
        }
        return Parameters.HEADER_SIZE
                + myTotalInodes * Parameters.INODE_SIZE
                + (blockId - 1) * Parameters.DATA_BLOCK_SIZE;
    }
}
