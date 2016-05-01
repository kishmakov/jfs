package org.kshmakov.jfs.driver;

import org.kshmakov.jfs.JFSException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FileInputStream extends InputStream {
    private FileDescriptor fd;
    private FileSystemDriver fs;

    private final Object myLock = new Object();
    private int myOffset = 0;
    private int myMarkedOffset = 0;

    public FileInputStream(FileSystemDriver fs, FileDescriptor fd) {
        this.fd = fd;
        this.fs = fs;
    }

    @Override
    public int read() throws IOException {
        byte[] bytes = new byte[1];
        int result = read(bytes, 0, 1);
        return result == 1
                ? (int) bytes[0] & 0xFF
                : -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (myLock) {
            try {
                if (fd == null || fs == null) {
                    throw new IOException("Stream is closed");
                }

                if (len <= 0) {
                    return 0;
                }

                ByteBuffer buffer = fs.tryReadFromFile(fd, myOffset, len);
                if (buffer.capacity() == 0) {
                    return  -1;
                }

                myOffset += buffer.capacity();
                buffer.rewind();
                buffer.get(b, off, buffer.capacity());
                return buffer.capacity();
            } catch (JFSException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    @Override
    public long skip(long n) throws IOException {
        synchronized (myLock) {
            try {
                if (fd == null || fs == null) {
                    throw new IOException("Stream is closed");
                }

                int oldOffset = myOffset;
                myOffset = Math.min(myOffset + (int) n, fs.getFileSize(fd));
                return myOffset - oldOffset;
            } catch (JFSException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    @Override
    public int available() {
        return 0; // nothing is buffered
    }

    @Override
    public void close() {
        synchronized (myLock) {
            fd = null;
            fs = null;
        }
    }

    @Override
    public void mark(int readlimit) {
        myMarkedOffset = myOffset;
    }

    @Override
    public void reset() throws IOException {
        synchronized (myLock) {
            if (fd == null || fs == null) {
                throw new IOException("Stream is closed");
            }
        }

        myOffset = myMarkedOffset;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
