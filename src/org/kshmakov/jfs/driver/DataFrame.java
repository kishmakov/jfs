package org.kshmakov.jfs.driver;

public class DataFrame {
    public final byte[] bytes;
    public final int offset;
    public final int length;

    public DataFrame(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public DataFrame(byte[] bytes) {
        this.bytes = bytes;
        this.offset = 0;
        this.length = bytes.length;
    }
}
