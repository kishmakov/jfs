package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.DirectoryEntry;

public interface Descriptor {
    Parameters.EntryType getType();
    int getInodeId();
    String getName();
}
