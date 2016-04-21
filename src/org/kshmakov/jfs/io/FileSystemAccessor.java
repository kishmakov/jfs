package org.kshmakov.jfs.io;

import org.kshmakov.jfs.JFSBadFileException;
import org.kshmakov.jfs.JFSException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Thread unsafe.
 */
public class FileSystemAccessor {
    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    private FileChannel myChannel;
    private RandomAccessFile myFile;

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

        if (fileSize < Parameters.MIN_SIZE) {
            throw new JFSBadFileException("file " + fileName + " is too small");
        }

        if (fileSize > Parameters.MAX_SIZE) {
            throw new JFSBadFileException("file " + fileName + " is too big");
        }

    }

    public static ByteBuffer newBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer;
    }

    public ByteBuffer readBuffer(long position, int size) throws JFSBadFileException {
        try {
            assert position + size <= fileSize;
            ByteBuffer result = ByteBuffer.allocate(size);
            result.order(ByteOrder.BIG_ENDIAN);
            myChannel.read(result, position);
            return result;
        } catch (IOException e) {
            throw new JFSBadFileException("could not read buffer from file: " + e.getMessage());
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
}
