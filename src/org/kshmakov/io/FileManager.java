package org.kshmakov.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

public final class FileManager {
    public static final long MINIMAL_SIZE = 239;
    public static final long MAXIMAL_SIZE = 100500;

    private static final int SIGNATURE = 0xAABBCCDD;

    private static final long BLOCK_SIZE = 4096;
    private static final long INODE_SIZE = 64;

    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    private static int inodesNumbers(long size) {
        // heuristic rule, see https://en.wikipedia.org/wiki/Inode#Details
        return (int) (Math.min(size, MAXIMAL_SIZE) / (100 * INODE_SIZE));
    }

    public static void formatFile(String fileName) throws IOException {
        if (!isFile(fileName)) {
            throw new FileNotFoundException();
        }

        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        FileChannel channel = file.getChannel();

        long fileSize = channel.size();

        if (fileSize < MINIMAL_SIZE) {
            throw new IOException("file is too small");
        }

        if (fileSize > MAXIMAL_SIZE) {
            throw new IOException("file is too big");
        }

        ByteBuffer header = ByteBuffer.allocate(18);
        header.order(ByteOrder.BIG_ENDIAN);

        header.putInt(SIGNATURE);
        header.putShort((short) BLOCK_SIZE);
        header.putInt(inodesNumbers(fileSize));
        header.putInt(2); // first vacant inode
        header.putInt(2); // first vacant data block
        header.flip();

        int written = channel.write(header);
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
