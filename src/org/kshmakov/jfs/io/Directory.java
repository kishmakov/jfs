package org.kshmakov.jfs.io;

import java.util.HashMap;

// TODO: complete
public class Directory {
    public HashMap<String, DirectoryDescriptor> directories = new HashMap<String, DirectoryDescriptor>();
    public HashMap<String, FileDescriptor> files = new HashMap<String, FileDescriptor>();
}
