package org.kshmakov.jfs.driver;

import org.kshmakov.jfs.JFSException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class FileOutputStream extends OutputStream {
    private FileDescriptor fd;
    private FileSystemDriver fs;

    private final Object myLock = new Object();

    public FileOutputStream(FileSystemDriver fs, FileDescriptor fd) {
        this.fd = fd;
        this.fs = fs;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] bytes = new byte[]{(byte) b};
        write(bytes, 0, 1);
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

                fs.tryAppendToFile(fd, new DataFrame(b, off, len));
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
