package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

import java.io.File;

@NotThreadSafe
public class FileAccessor extends FileAccessorBase {

    private static boolean isFile(String name) {
        File file = new File(name);
        return file.exists() && !file.isDirectory();
    }

    @Override
    protected int getTotalInodes() throws JFSBadFileException {
        return readHeaderInt(HeaderOffsets.TOTAL_INODES);
    }

    @Override
    protected int getTotalBlocks() throws JFSBadFileException {
        return readHeaderInt(HeaderOffsets.TOTAL_BLOCKS);
    }

    public FileAccessor(String fileName) throws JFSException {
        super(fileName);

//        if (!isFile(fileName)) {
//            throw new FileNotFoundException();
//        }

//        System.out.printf("inodes total = %d\n", myTotalInodes);
//        System.out.printf("blocks total = %d\n", myTotalBlocks);
    }
}
