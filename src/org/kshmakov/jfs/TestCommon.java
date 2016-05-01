package org.kshmakov.jfs;

import org.junit.runner.notification.RunListener;
import org.kshmakov.jfs.driver.*;
import org.kshmakov.jfs.driver.FileDescriptor;
import org.kshmakov.jfs.driver.FileInputStream;
import org.kshmakov.jfs.driver.FileOutputStream;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.FileFormatter;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

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

    public static void writelnLinesTo(FileSystemDriver fs, FileDescriptor fd, String[] lines) {
        PrintWriter writer = new PrintWriter(new FileOutputStream(fs, fd));
        for (String line : lines) {
            writer.println(line);
        }

        writer.close();
    }

    public static void writeLinesTo(FileSystemDriver fs, FileDescriptor fd, String[] lines) {
        PrintWriter writer = new PrintWriter(new FileOutputStream(fs, fd));
        for (String line : lines) {
            writer.print(line);
        }

        writer.close();
    }

    public static String[] readLinesFrom(FileSystemDriver fs, FileDescriptor fd) {
        Scanner scanner = new Scanner(new FileInputStream(fs, fd));
        ArrayList<String> lines = new ArrayList<String>();

        while (scanner.hasNext()) {
            lines.add(scanner.next());
        }

        String[] result = new String[lines.size()];
        return lines.toArray(result);
    }

    public static void cleanUp() {
        (new File(TestCommon.TEST_JFS_NAME)).delete();
    }
}
