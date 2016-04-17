package org.kshmakov.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

public final class FileManager {
    public static final long MIN_SIZE = 239;
    public static final long MAX_SIZE = 100500;

    private static final int SIGNATURE = 0xAABBCCDD;

    private static final long HEADER_SIZE = 18;
    private static final long INODE_SIZE = 64;
    private static final long BLOCK_SIZE = 4096;

    private enum INODE_TYPE {
        DIRECTORY,
        FILE
    }

    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    private static int numberOfInodes(long size) {
        assert size >= MIN_SIZE && size <= MAX_SIZE;
        // heuristic rule, see https://en.wikipedia.org/wiki/Inode#Details
        return (int) (size / (100 * INODE_SIZE));
    }

    private static int numberOfBlocks(long size) {
        assert size >= MIN_SIZE && size <= MAX_SIZE;
        long inodesSize = numberOfInodes(size) * INODE_SIZE;
        long blocksSize = size - HEADER_SIZE - inodesSize;
        return (int) (blocksSize / BLOCK_SIZE);
    }

    private static ByteBuffer allocateBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer;
    }

    private static int writeBuffer(FileChannel channel, ByteBuffer buffer) throws IOException {
        buffer.flip();
        buffer.limit(buffer.capacity());
        int result = channel.write(buffer);
        System.out.printf("channel.pos=%d\n", channel.position());
        assert result == buffer.capacity();
        return result;
    }

    public static void formatFile(String fileName) throws IOException {
        if (!isFile(fileName)) {
            throw new FileNotFoundException();
        }

        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        FileChannel channel = file.getChannel();

        long fileSize = channel.size();

        if (fileSize < MIN_SIZE) {
            throw new IOException("file is too small");
        }

        if (fileSize > MAX_SIZE) {
            throw new IOException("file is too big");
        }

        ByteBuffer header = allocateBuffer((int) HEADER_SIZE);

        header.putInt(SIGNATURE);
        header.putShort((short) BLOCK_SIZE);
        header.putInt(numberOfInodes(fileSize));
        header.putInt(2); // first vacant inode
        header.putInt(2); // first vacant data block

        writeBuffer(channel, header);

        int inodesNum = numberOfInodes(fileSize);
        for (int inodeId = 0; inodeId < inodesNum; inodeId++) {
            ByteBuffer inode = allocateBuffer((int) INODE_SIZE);
            if (inodeId > 0) {
                inode.putInt((inodeId + 2) % (inodesNum + 1));
            } else {
                inode.putInt((INODE_TYPE.DIRECTORY.ordinal() << 24) + 0x000001);
                inode.putInt((int) BLOCK_SIZE);
                inode.putInt(0x00000001); // pointer to first data block
            }

            writeBuffer(channel, inode);
        }

        int blocksNum = numberOfBlocks(fileSize);
        for (int blockId = 0; blockId < blocksNum; blockId++) {
            ByteBuffer block = allocateBuffer((int) BLOCK_SIZE);
            if (blockId > 0) {
                block.putInt((blockId + 2) % (blocksNum + 1));
            } else {
                // TODO: empty directory layout
            }

            writeBuffer(channel, block);
        }
    }

    private FileChannel myChanel;
    private RandomAccessFile myRWFile;

    public FileManager(String name) throws IOException {
        if (!isFile(name)) {
            throw new FileNotFoundException();
        }

        myRWFile = new RandomAccessFile(name, "rw");

        myChanel = myRWFile.getChannel();

        System.out.format("File size: %d\n", myChanel.size());
        System.out.format("Starting position: %d\n", myChanel.position());

        FileLock lock1 = myChanel.lock(0, 4, true);
        FileLock lock2 = myChanel.lock(4, 4, false);

        System.out.printf("lock1.isShared = %b, lock2.isShared = %b\n", lock1.isShared(), lock2.isShared());

//        FileLock lock3 = myChanel.tryLock(4, 4, false);
//        System.out.printf("lock3.isNull = %b\n", lock3 == null);

        ByteBuffer signature = ByteBuffer.allocate(4);
        myChanel.read(signature, 0);

        if (signature.getInt(0) != SIGNATURE) {
            throw new IOException("Bad file provided.");
        }

        System.out.format("Position after read: %d\n", myChanel.position());
    }
}
