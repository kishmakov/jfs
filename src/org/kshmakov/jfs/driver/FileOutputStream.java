package org.kshmakov.jfs.driver;

import org.kshmakov.jfs.JFSException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FileOutputStream extends OutputStream {
    private FileDescriptor fd;
    private FileSystemDriver fs;

    private final Object myLock = new Object();

    public FileOutputStream(FileDescriptor fd, FileSystemDriver fs) {
        this.fd = fd;
        this.fs = fs;
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (myLock) {
            try {
                if (fd == null || fs == null) {
                    throw new IOException("Stream is closed");
                }

                ByteBuffer buffer = ByteBuffer.allocate(1);
                buffer.put((byte) b);

                fs.tryAppendToFile(fd, buffer);
            } catch (JFSException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (myLock) {
            try {
                if (fd == null || fs == null) {
                    throw new IOException("Stream is closed");
                }

                if (len <= 0) {
                    return;
                }

                ByteBuffer buffer = ByteBuffer.allocate(len);
                buffer.put(b, off, len);
                fs.tryAppendToFile(fd, buffer);
            } catch (JFSException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    @Override
    public void flush() throws IOException {
        // nothing is buffered
    }

    @Override
    public void close() {
        synchronized (myLock) {
            fd = null;
            fs = null;
        }
    }
}
