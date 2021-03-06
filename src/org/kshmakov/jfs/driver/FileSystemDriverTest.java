package org.kshmakov.jfs.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kshmakov.jfs.TestCommon;
import org.kshmakov.jfs.io.FileAccessor;
import org.kshmakov.jfs.io.HeaderOffsets;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class FileSystemDriverTest {
    private final static String LONG_NAME = "thisisa1reallylongdirectory__````name;youshoudnot_name_directory_likethis.because)then1%@&df^it_mayjustblow_up_some_sunnymorning;moreover9itwillnotmake_you_famous_nor_rich;please,stop_it,stopit--+=whileUcan;DOYOUHEREME?AHhhhh!1111111-934itgtkregetfghodnfd";

    private FileAccessor accessor = null;
    private FileSystemDriver driver = null;

    @Before
    public void initialization() throws IOException, JFSException {
        accessor = TestCommon.createAccessor(200000);
        driver = new FileSystemDriver(accessor);
    }

    @Test
    public void test00() throws IOException, JFSException {
        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        DirectoryDescriptor rootDir = driver.rootInode();
        assertEquals(2, driver.getDirectories(rootDir).size());

        driver.tryAddDirectory(rootDir, "abacaba");

        assertEquals(3, driver.getDirectories(rootDir).size());
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(46, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        driver.tryRemoveDirectory(rootDir, "abacaba");

        assertEquals(2, driver.getDirectories(rootDir).size());
        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
    }

    @Test(expected = JFSException.class)
    public void test01() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();
        driver.tryAddDirectory(rootDir, "abacaba");
        driver.tryRemoveDirectory(rootDir, "abacaba");
        driver.tryRemoveDirectory(rootDir, "abacaba");
    }

    @Test
    public void test02() throws IOException, JFSException {
        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        DirectoryDescriptor rootDir = driver.rootInode();
        driver.tryAddDirectory(rootDir, LONG_NAME);

        assertEquals(3, driver.getDirectories(rootDir).size());
        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(46, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
    }

    @Test(expected = JFSException.class)
    public void test03() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();
        driver.tryAddDirectory(rootDir, LONG_NAME + "1");
    }

    @Test
    public void test04() throws IOException, JFSException {
        String[] names = new String[]{"a", "directory", LONG_NAME};

        for (String name : names) {
            TestCommon.formatFile();
            FileSystemDriver driver = new FileSystemDriver(accessor);
            DirectoryDescriptor currentDir = driver.rootInode();

            for (int i = 0; i < 30; i++) {
                driver.tryAddDirectory(currentDir, name);

                assertEquals(29 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
                assertEquals(46 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

                Map<String, DirectoryDescriptor> directories = driver.getDirectories(currentDir);

                assertEquals(3, directories.size());
                assertTrue(directories.containsKey(name));

                currentDir = directories.get(name);
            }
        }
    }

    @Test
    public void test05() throws IOException, JFSException {
        String prefix = "directory";

        DirectoryDescriptor rootDir = driver.rootInode();

        for (int i = 0; i < 30; i++) {
            driver.tryAddDirectory(rootDir, prefix + Integer.toString(i));
            assertEquals(29 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(46 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
        }
    }

    @Test
    public void test06() throws IOException, JFSException {
        String prefix = LONG_NAME.substring(2);

        DirectoryDescriptor rootDir = driver.rootInode();

        for (int i = 0; i < 15; ++i) {
            driver.tryAddDirectory(rootDir, prefix + Integer.toString(i));
            assertEquals(29 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(46 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
        }

        for (int i = 15; i < 30; ++i) {
            driver.tryAddDirectory(rootDir, prefix + Integer.toString(i));
            assertEquals(29 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(45 - i, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
        }
    }

    @Test
    public void test07() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();
        DirectoryDescriptor aDir = driver.tryAddDirectory(rootDir, "a");
        driver.tryAddDirectory(rootDir, "b");

        assertEquals(28, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(45, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        DirectoryDescriptor aaDir = driver.tryAddDirectory(aDir, "aa");
        driver.tryAddDirectory(aDir, "bb");

        assertEquals(26, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(43, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        String fileName = "file.txt";

        driver.tryAddFile(aaDir, fileName);

        assertEquals(25, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(43, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        Map<String, FileDescriptor> files = driver.getFiles(aaDir);
        assertEquals(1, files.size());
        assertTrue(files.containsKey(fileName));
        String[] lines = new String[]{"First line.", "Second line.", "Bazinga!"};
        TestCommon.writelnLinesTo(driver, files.get(fileName), lines);

        assertEquals(25, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(42, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        driver.tryRemoveDirectory(rootDir, "a");

        assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(46, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

        driver.tryRemoveDirectory(rootDir, "b");

        assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
        assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
    }

    @Test
    public void test08() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();

        String fileName = "file.txt";

        FileDescriptor file = driver.tryAddFile(rootDir, fileName);

        String[] input = new String[]{"First_line.", "Second_line.", "Bazinga!"};
        TestCommon.writelnLinesTo(driver, file, input);
        String[] output = TestCommon.readLinesFrom(driver, file);
        assertArrayEquals(input, output);
    }

    @Test
    public void test09() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();

        String fileName = "file.txt";

        int bufferSize = Parameters.MAX_FILE_SIZE;
        byte[] bytes = new byte[bufferSize];

        for (int i = 0; i < bufferSize; ++i) {
            bytes[i] = (byte) (i % 100);
        }

        for (int repetition = 0; repetition < 5; ++repetition) {
            assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

            FileDescriptor file = driver.tryAddFile(rootDir, fileName);
            driver.tryWriteIntoFile(file, new DataFrame(bytes), 0);

            assertEquals(29, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(35, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));

            driver.tryRemoveFile(rootDir, fileName);

            assertEquals(30, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_INODES));
            assertEquals(47, accessor.readHeaderInt(HeaderOffsets.TOTAL_UNALLOCATED_BLOCKS));
        }
    }

    @Test
    public void test10() throws IOException, JFSException {
        DirectoryDescriptor rootDir = driver.rootInode();

        DirectoryDescriptor aDir = driver.tryAddDirectory(rootDir, "a");
        DirectoryDescriptor bDir = driver.tryAddDirectory(aDir, "b");

        DirectoryDescriptor rtDir = driver.getParentDirectory(rootDir);

        assertEquals(rootDir, driver.getParentDirectory(rootDir));
        assertEquals(rootDir, driver.getParentDirectory(aDir));
        assertEquals(aDir, driver.getParentDirectory(bDir));
    }

    @After
    public void cleanUp() {
        TestCommon.cleanUp();
    }
}