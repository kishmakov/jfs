package org.kshmakov.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

public final class FileManager {

    private static final int SIGNATURE = 0xAABBCCDD;

    private FileChannel myChanel;
    private RandomAccessFile myRWFile;

    public FileManager(String name) throws IOException {
        myRWFile = new RandomAccessFile(name, "rw");
//        myRWFile.writeInt(SIGNATURE);

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
