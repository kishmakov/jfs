package org.kshmakov.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileAccessor {
    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    private FileChannel myChannel;
    private RandomAccessFile myFile;

    public final long fileSize;

    public FileAccessor(String fileName) throws IOException {
        if (!isFile(fileName)) {
            throw new FileNotFoundException();
        }

        myFile = new RandomAccessFile(fileName, "rw");
        myChannel = myFile.getChannel();

        fileSize = myChannel.size();

        if (fileSize < Parameters.MIN_SIZE) {
            throw new IOException("file is too small");
        }

        if (fileSize > Parameters.MAX_SIZE) {
            throw new IOException("file is too big");
        }

    }

    public ByteBuffer readBuffer(long position, int size) throws IOException {
        assert position + size <= fileSize;
        ByteBuffer result = ByteBuffer.allocate(size);
        result.order(Parameters.ENDIANNESS);
        myChannel.read(result, position);
        return result;
    }

    public int writeBuffer(ByteBuffer buffer) throws IOException {
        buffer.flip();
        buffer.limit(buffer.capacity());
        assert myChannel.position() + buffer.capacity() <= fileSize;
        int result = myChannel.write(buffer);
        System.out.printf("channel.pos=%d\n", myChannel.position());
        assert result == buffer.capacity();
        return result;
    }
}
