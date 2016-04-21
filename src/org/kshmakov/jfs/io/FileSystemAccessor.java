package org.kshmakov.jfs.io;

import org.kshmakov.jfs.JFSException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.io.primitives.Block;
import org.kshmakov.jfs.io.primitives.Header;

@NotThreadSafe
public class FileSystemAccessor {

    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    private final FileChannel myChannel;
    private final RandomAccessFile myFile;

    private final int myTotalInodes;
    private final int myTotalBlocks;

    public final long fileSize;

    public FileSystemAccessor(String fileName) throws FileNotFoundException, JFSException {
        if (!isFile(fileName)) {
            throw new FileNotFoundException();
        }

        myFile = new RandomAccessFile(fileName, "rw");
        myChannel = myFile.getChannel();

        try {
            fileSize = myChannel.size();
        } catch (IOException e) {
            throw new JFSException("problems while accessing file " + fileName + ": " + e.getMessage());
        }

        if (fileSize < Parameters.MIN_SIZE || fileSize > Parameters.MAX_SIZE) {
            String range = "[" + Long.toString(Parameters.MIN_SIZE) + ", " + Long.toString(Parameters.MAX_SIZE) + "]";
            throw new JFSBadFileException("file size of " + fileName + " is not in range " + range);
        }

        myTotalInodes = readInt(Offsets.TOTAL_INODES);
        myTotalBlocks = readInt(Offsets.TOTAL_BLOCKS);

        System.out.printf("inodes total = %d\n", myTotalInodes);
        System.out.printf("blocks total = %d\n", myTotalBlocks);
    }

    static public ByteBuffer newBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer;
    }

    public int readInt(long position) throws JFSBadFileException {
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

    public void writeInt(long position, int number) throws JFSBadFileException {
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

    public int writeBlock(Block block, int blockId) throws JFSException {
        try {
            ByteBuffer buffer = block.toBuffer();
            buffer.flip();
            buffer.limit(buffer.capacity());
            assert blockOffset(blockId) + buffer.capacity() <= fileSize;
            int result = myChannel.write(buffer, blockOffset(blockId));
            assert result == buffer.capacity();
            return result;
        } catch (IOException e) {
            throw new JFSBadFileException("could not write buffer to file: " + e.getMessage());
        }
    }

    public ByteBuffer readBuffer(long position, int size) throws JFSBadFileException {
        try {
            assert position + size <= fileSize;
            ByteBuffer result = ByteBuffer.allocate(size);
            result.order(ByteOrder.BIG_ENDIAN);
            myChannel.read(result, position);
            result.rewind();
            return result;
        } catch (IOException e) {
            throw new JFSBadFileException("could not read buffer from file: " + e.getMessage());
        }
    }

    public int writeBuffer(ByteBuffer buffer, long position) throws JFSBadFileException {
        try {
            buffer.flip();
            buffer.limit(buffer.capacity());
            assert position + buffer.capacity() <= fileSize;
            int result = myChannel.write(buffer, position);
            assert result == buffer.capacity();
            return result;
        } catch (IOException e) {
            throw new JFSBadFileException("could not write buffer to file: " + e.getMessage());
        }
    }

    public int writeBuffer(ByteBuffer buffer) throws JFSBadFileException {
        try {
            buffer.flip();
            buffer.limit(buffer.capacity());
            assert myChannel.position() + buffer.capacity() <= fileSize;
            int result = myChannel.write(buffer);
            assert result == buffer.capacity();
            return result;
        } catch (IOException e) {
            throw new JFSBadFileException("could not write buffer to file: " + e.getMessage());
        }
    }

    public Header header() throws JFSBadFileException {
        ByteBuffer buffer = readBuffer(0, Parameters.HEADER_SIZE);
        return new Header(buffer);
    }

    public long inodeOffset(int inodeId) throws JFSException {

        if (inodeId <= 0 || inodeId > myTotalInodes) {
            String range = "[1; " + Integer.toString(myTotalInodes) + "]";
            throw new JFSException("inodeId=" + Integer.toString(inodeId) + " not in " + range);
        }

        return Parameters.HEADER_SIZE + (inodeId - 1) * Parameters.INODE_SIZE;
    }

    public long blockOffset(int blockId) throws JFSException {
        if (blockId <= 0 || blockId > myTotalBlocks) {
            String range = "[1; " + Integer.toString(myTotalBlocks) + "]";
            throw new JFSException("blockId=" + Integer.toString(blockId) + " not in " + range);
        }
        return Parameters.HEADER_SIZE
                + myTotalInodes * Parameters.INODE_SIZE
                + (blockId - 1) * Header.DATA_BLOCK_SIZE;
    }

}
