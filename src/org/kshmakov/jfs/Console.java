package org.kshmakov.jfs;

import org.kshmakov.jfs.io.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

public class Console {
    private FileSystemManager myManager = null;
    private String myCurrentFile = "";
    private ArrayList<String> myCurrentPath = new ArrayList<String>();
    private Descriptor myCurrentDir;

    public String prefix() {
        StringBuilder builder = new StringBuilder();
        if (!myCurrentFile.isEmpty()) {
            builder.append(myCurrentFile);
            builder.append(Parameters.SEPARATOR);

            boolean firstItem = true;
            for (String pathItem : myCurrentPath) {
                if (!firstItem)
                    builder.append(Parameters.SEPARATOR);

                firstItem = false;
                builder.append(pathItem);
            }
        }

        return builder.append("> ").toString();
    }

    private String changeDirectory(String command[]) throws UnsupportedEncodingException, JFSBadFileException {
        String usage = "usage: cd dir_name";

        if (command.length < 2) {
            return "directory name is not provided, " + usage;
        }

        Directory directory = myManager.directory(myCurrentDir);

        if (!directory.entries.containsKey(command[1])) {
            return "no such file or directory";
        }

        Descriptor descriptor = directory.entries.get(command[1]);

        if (descriptor.getType() != Parameters.EntryType.DIRECTORY) {
            return command[1] + " is not a directory";
        }

        myCurrentDir = descriptor;

        switch (command[1]) {
            case ".":
                break;
            case "..":
                if (myCurrentPath.size() > 0) {
                    myCurrentPath.remove(myCurrentPath.size() - 1);
                }
                break;
            default:
                myCurrentPath.add(command[1]);
        }

        return "";
    }

    private String crateFile(String command[]) throws UnsupportedEncodingException, JFSException {
        String usage = "usage: create file_name file_size";

        if (command.length < 2) {
            return "file name is not provided, " + usage;
        }

        if (command.length < 3) {
            return "file size is not provided, " + usage;
        }

        int size;

        try {
            size = Integer.parseInt(command[2]);
        } catch (NumberFormatException e) {
            return "bad size description provided";
        }

        if (size < Parameters.MIN_SIZE) {
            return "required size is too small, minimal size is " + Long.toString(Parameters.MIN_SIZE);
        }

        if (size > Parameters.MAX_SIZE) {
            return "required size is too big, maximal size is " + Long.toString(Parameters.MAX_SIZE);
        }

        try {
            RandomAccessFile file = new RandomAccessFile(command[1], "rw");
            file.setLength(size);
        } catch (IOException e) {
            return "could not create file, reason: " + e.getMessage();
        }

        String formatResult = formatFile(command);
        return formatResult.isEmpty() ? "done" : formatResult;
    }

    private String formatFile(String command[]) throws UnsupportedEncodingException, JFSException {
        String usage = "usage: format file_name";

        if (command.length < 2) {
            return "file name is not provided, " + usage;
        }

        try {
            Formatter.formatFile(command[1]);
        } catch (JFSBadFileException e) {
            return "could not format file, reason: " + e.getMessage();
        } catch (FileNotFoundException e) {
            return "file not found";
        }

        return command[1] + " formatted";
    }

    private String listDirectory() throws JFSBadFileException, UnsupportedEncodingException {
        if (myManager == null)
            return "file system is not mounted";

        Directory directory = myManager.directory(myCurrentDir);
        ArrayList<String> listItems = new ArrayList<String>(directory.entries.size());

        directory.entries.forEach((key, value) -> listItems.add((value.getType() == Parameters.EntryType.DIRECTORY ? "d: " : "f: ") + key));

        Collections.sort(listItems);

        StringBuilder builder = new StringBuilder();
        for (String item : listItems) {
            if (builder.length() > 0)
                builder.append("\n");
            builder.append(item);
        }

        return builder.toString();
    }

    private String mountFile(String command[]) throws JFSException {
        if (command.length < 2)
            return "file name is not provided";

        if (myManager != null)
            umountFile();

        try {
            myManager = new FileSystemManager(command[1]);
            myCurrentDir = myManager.rootInode();
            myCurrentPath = new ArrayList<String>();
            myCurrentFile = "@" + command[1] + ":";
        } catch (FileNotFoundException e) {
            return "file " + command[1] + " not found";
        } catch (JFSBadFileException e) {
            return "file " + command[1] + " could not be mounted: " + e.getMessage();
        }

        return "";
    }

    private String umountFile() {
        myCurrentFile = "";
        myCurrentPath = new ArrayList<String>();
        myManager = null;

        return "";
    }

    public String execute(String[] command) throws UnsupportedEncodingException, JFSException {
        assert command.length > 0;

        try {
            switch (command[0]) {
                case "cd"    : return changeDirectory(command);
                case "create": return crateFile(command);
                case "format": return formatFile(command);
                case "ls"    : return listDirectory();
                case "umount": return umountFile();
                case "mount" : return mountFile(command);
            }

            return "unsupported command";
        } catch (JFSException e) {
            umountFile();
            return "logic error encountered: " + e.getMessage() + ",\n" + e.getStackTrace().toString();
        }
    }
}
