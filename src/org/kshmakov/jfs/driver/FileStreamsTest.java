package org.kshmakov.jfs.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kshmakov.jfs.TestCommon;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.HeaderOffsets;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;

public class FileStreamsTest {
    private FileAccessor accessor = null;
    private FileSystemDriver fs = null;
    private FileDescriptor fd = null;
    private final String FILE_NAME = "file.txt";

    @Before
    public void initialization() throws IOException, JFSException {
        accessor = TestCommon.createAccessor(200000);
        fs = new FileSystemDriver(accessor);
        fd = fs.tryAddFile(fs.rootInode(), FILE_NAME);
    }

    @Test
    public void test00() throws IOException, JFSException {
        /**
         * Checks new block allocation.
         */
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        PrintWriter writer = new PrintWriter(new FileOutputStream(fs, fd));

        String refLine = "0123456789ABCDEF";

        for (int i = 0; i < 256; i++) {
            writer.append(refLine);
        }

        writer.flush();

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(46, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        writer.append(refLine);
        writer.flush();

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(45, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        for (int i = 0; i < 255; i++) {
            writer.append(refLine);
        }

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(45, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        writer.append(refLine);
        writer.flush();

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(44, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        writer.close();

        FileInputStream inputStream = new FileInputStream(fs, fd);

        for (int i = 0; i < 513; i++) {
            for (byte refByte = '0'; refByte <= '9'; refByte++) {
                assertEquals(refByte, inputStream.read());
            }

            for (byte refByte = 'A'; refByte <= 'F'; refByte++) {
                assertEquals(refByte, inputStream.read());
            }
        }

    }

    @Test
    public void test01() throws IOException, JFSException {
        /**
         * Checks file sizes 48K.
         */
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        OutputStream outputStream = new FileOutputStream(fs, fd);

        byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        for (int blockId = 0; blockId < Parameters.DIRECT_POINTERS_NUMBER; blockId++) {
            int portionsNumber = Parameters.DATA_BLOCK_SIZE / bytes.length;
            for (int portionId = 0; portionId < portionsNumber; portionId++) {
                outputStream.write(bytes);
            }
        }

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47 - Parameters.DIRECT_POINTERS_NUMBER, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
    }

    @Test
    public void test02() throws IOException, JFSException {
        /**
         * Checks file sizes 48K.
         */
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        for (int repetition = 0; repetition < 5; ++repetition) {
            OutputStream outputStream = new FileOutputStream(fs, fd);

            byte[] outBytes = new byte[Parameters.MAX_FILE_SIZE];

            for (int i = 0; i < outBytes.length; ++i) {
                outBytes[i] = (byte) (i % 100);
            }

            outputStream.write(outBytes);

            assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(47 - Parameters.DIRECT_POINTERS_NUMBER, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

            outputStream.close();

            InputStream inputStream = new FileInputStream(fs, fd);
            byte[] inBytes = new byte[Parameters.MAX_FILE_SIZE];
            inputStream.read(inBytes);

            for (int i = 0; i < inBytes.length; ++i) {
                assertEquals(inBytes[i], (byte) (i % 100));
            }

            fs.tryRemoveFile(fs.rootInode(), FILE_NAME);

            assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

            fd = fs.tryAddFile(fs.rootInode(), FILE_NAME);
        }
    }

    @After
    public void cleanUp() {
        TestCommon.cleanUp();
    }
}