package org.kshmakov.jfs.driver;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.kshmakov.jfs.JFSException;
import org.kshmakov.jfs.TestCommon;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.FileFormatter;
import org.kshmakov.jfs.io.HeaderOffsets;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileSystemDriverTest {

    @Test
    public void test00() throws IOException, JFSException {
        FileAccessor accessor = TestCommon.createAccessor(200000);
        FileSystemDriver driver = new FileSystemDriver(accessor);

        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        DirectoryDescriptor rootDir = driver.rootInode();
        assertEquals("After initial formatting there muse be empty directory.", 2, driver.getDirectories(rootDir).size());

        driver.tryAddDirectory(rootDir, "abacaba");

        assertEquals(3, driver.getDirectories(rootDir).size());
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(46, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        driver.tryRemoveDirectory(rootDir, "abacaba");

        assertEquals(2, driver.getDirectories(rootDir).size());
        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
    }

    @After
    public void cleanUp() {
        TestCommon.cleanUp();
    }
}