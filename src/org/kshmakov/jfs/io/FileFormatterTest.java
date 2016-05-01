package org.kshmakov.jfs.io;

import org.junit.After;
import org.junit.Test;
import org.kshmakov.jfs.TestCommon;

import javax.xml.ws.soap.Addressing;

import static org.junit.Assert.*;

public class FileFormatterTest {
    @Test(expected = JFSBadFileException.class)
    public void test00() throws Exception {
        TestCommon.createFile((int) Parameters.MIN_FS_SIZE - 1);
        TestCommon.formatFile();
    }

    @Test
    public void test01() throws Exception {
        FileAccessor accessor = TestCommon.createAccessor((int) Parameters.MIN_FS_SIZE);
        assertEquals(accessor.fileSize, Parameters.MIN_FS_SIZE);
        assertEquals(Parameters.MAGIC_NUMBER, accessor.readHeaderInt(HeaderOffsets.MAGIC_NUMBER));
        assertEquals(Parameters.DATA_BLOCK_SIZE, accessor.readHeaderInt(HeaderOffsets.VERSION_AND_BLOCK_SIZE));
        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID));
        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID));

        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        assertEquals(1, accessor.readHeaderInt(HeaderOffsets.TOTAL_INODES));
        assertEquals(1, accessor.readHeaderInt(HeaderOffsets.TOTAL_BLOCKS));
    }

    @Test
    public void test02() throws Exception {
        FileAccessor accessor = TestCommon.createAccessor((int) Parameters.MIN_FS_SIZE + Parameters.DATA_BLOCK_SIZE);
        assertEquals(Parameters.MAGIC_NUMBER, accessor.readHeaderInt(HeaderOffsets.MAGIC_NUMBER));
        assertEquals(Parameters.DATA_BLOCK_SIZE, accessor.readHeaderInt(HeaderOffsets.VERSION_AND_BLOCK_SIZE));
        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID));
        assertEquals(2, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID));

        assertEquals(0, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(1, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        assertEquals(1, accessor.readHeaderInt(HeaderOffsets.TOTAL_INODES));
        assertEquals(2, accessor.readHeaderInt(HeaderOffsets.TOTAL_BLOCKS));
    }

    @Test
    public void test03() throws Exception {
        FileAccessor accessor = TestCommon.createAccessor((int) Parameters.MIN_FS_SIZE + 100 * Parameters.DATA_BLOCK_SIZE);
        assertEquals(Parameters.MAGIC_NUMBER, accessor.readHeaderInt(HeaderOffsets.MAGIC_NUMBER));
        assertEquals(Parameters.DATA_BLOCK_SIZE, accessor.readHeaderInt(HeaderOffsets.VERSION_AND_BLOCK_SIZE));
        assertEquals(2, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_INODE_ID));
        assertEquals(2, accessor.readHeaderInt(HeaderOffsets.FIRST_UNALLOCATED_BLOCK_ID));

        assertEquals(63, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(99, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        assertEquals(64, accessor.readHeaderInt(HeaderOffsets.TOTAL_INODES));
        assertEquals(100, accessor.readHeaderInt(HeaderOffsets.TOTAL_BLOCKS));
    }

    @After
    public void cleanUp() {
        TestCommon.cleanUp();
    }
}