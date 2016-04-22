package org.kshmakov.jfs.io;

import net.jcip.annotations.NotThreadSafe;
import org.kshmakov.jfs.JFSException;

import java.io.File;

@NotThreadSafe
public class FileAccessor extends FileAccessorBase {

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

//        System.out.printf("inodes total = %d\n", myTotalInodes);
//        System.out.printf("blocks total = %d\n", myTotalBlocks);
    }
}
