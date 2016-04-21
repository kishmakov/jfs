package org.kshmakov.jfs.driver;

import com.sun.istack.internal.Nullable;

import java.util.ArrayList;

// TODO: complete
public class Directory {
    public ArrayList<DirectoryDescriptor> directories = new ArrayList<DirectoryDescriptor>();
    public ArrayList<FileDescriptor> files = new ArrayList<FileDescriptor>();

    public int entriesNumber() {
        return directories.size() + files.size();
    }

    @Nullable
    public DirectoryDescriptor getDirectory(String name) {
        for (DirectoryDescriptor descriptor : directories) {
            if (descriptor.name.equals(name))
                return descriptor;
        }

        return null;
    }

    @Nullable
    public FileDescriptor getFile(String name) {
        for (FileDescriptor descriptor : files) {
            if (descriptor.name.equals(name))
                return descriptor;
        }

        return null;
    }
}
