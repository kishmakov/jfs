package org.kshmakov.jfs.driver.tools;

import com.sun.istack.internal.Nullable;
import org.kshmakov.jfs.io.primitives.DirectoryEntry;

import java.util.ArrayList;

public interface EntriesHelper {
    @Nullable
    static DirectoryEntry find(ArrayList<DirectoryEntry> entries, String name) {
        for (DirectoryEntry entry : entries) {
            if (entry.name.equals(name)) {
                return entry;
            }
        }
        return null;
    }
}
