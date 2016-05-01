package org.kshmakov.jfs;

import org.junit.runner.notification.RunListener;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.FileFormatter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TestCommon extends RunListener {
    private static final String TEST_JFS_NAME = "temp.jfs";

    public static void createFile(int size) throws IOException {
        RandomAccessFile file = new RandomAccessFile(TestCommon.TEST_JFS_NAME, "rw");
        file.setLength(size);
    }

    public static void formatFile() throws JFSException {
        FileFormatter formatter = new FileFormatter(TestCommon.TEST_JFS_NAME);
        formatter.format();
    }

    public static FileAccessor createAccessor(int size) throws IOException, JFSException {
        createFile(size);
        formatFile();

        return new FileAccessor(TestCommon.TEST_JFS_NAME);
    }

    public static void cleanUp() {
        (new File(TestCommon.TEST_JFS_NAME)).delete();
    }
}
