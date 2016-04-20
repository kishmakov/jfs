package org.kshmakov.jfs.io;

import org.kshmakov.jfs.io.primitives.AllocatedInode;
import org.kshmakov.jfs.io.primitives.DirectoryEntry;

import java.util.HashMap;

// TODO: complete
public class Directory {
    public static class EntryDescription {
        public boolean isDirectory;
        public int inodeId;
    }

    public static EntryDescription description(AllocatedInode.Type type, int inodeId) {
        EntryDescription result = new EntryDescription();
        result.isDirectory = type == AllocatedInode.Type.DIRECTORY;
        result.inodeId = inodeId;
        return result;
    }

    public HashMap<String, EntryDescription> entries;

    public Directory() {
        entries = new HashMap<String, EntryDescription>();
    }
}
