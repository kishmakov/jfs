package org.kshmakov.jfs;

import org.kshmakov.jfs.driver.Directory;
import org.kshmakov.jfs.driver.DirectoryDescriptor;
import org.kshmakov.jfs.driver.FileSystemDriver;
import org.kshmakov.jfs.driver.JFSRefuseException;
import org.kshmakov.jfs.io.*;
import org.kshmakov.jfs.io.NameHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;

public class Console {
    private FileSystemDriver myDriver = null;
    private String myCurrentFile = "";
    private ArrayList<String> myCurrentPath = new ArrayList<String>();
    private DirectoryDescriptor myCurrentDir;

    public String prefix() {
        StringBuilder builder = new StringBuilder();
        if (!myCurrentFile.isEmpty()) {
            builder.append(myCurrentFile);
            builder.append(NameHelper.SEPARATOR);

            boolean firstItem = true;
            for (String pathItem : myCurrentPath) {
                if (!firstItem)
                    builder.append(NameHelper.SEPARATOR);

                firstItem = false;
                builder.append(pathItem);
            }
        }

        return builder.append("> ").toString();
    }

    private String changeDirectory(String[] command) throws JFSException {
        String usage = "usage: cd dir_name";

        if (command.length < 2) {
            return "directory name is not provided, " + usage;
        }

        Directory directory = myDriver.getEntries(myCurrentDir);

        DirectoryDescriptor newDir = directory.getDirectory(command[1]);

        if (newDir == null) {
            return "no such directory";
        }

        myCurrentDir = newDir;

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

    private String crateFile(String[] command) throws JFSException {
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

        return command[1] + " created";
    }

    private String formatFile(String[] command) throws JFSException {
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

    private String listDirectory() throws JFSException {
        if (myDriver == null) {
            return "file system is not mounted";
        }

        Directory directory = myDriver.getEntries(myCurrentDir);
        ArrayList<String> listItems = new ArrayList<String>(directory.entriesNumber());

        directory.directories.forEach(item -> listItems.add("d: " + item.name));
        directory.files.forEach(item -> listItems.add("f: " + item.name));

        Collections.sort(listItems);

        StringBuilder builder = new StringBuilder();
        for (String item : listItems) {
            if (builder.length() > 0)
                builder.append("\n");
            builder.append(item);
        }

        return builder.toString();
    }

    private String makeDirectory(String[] command) throws JFSException {
        if (myDriver == null) {
            return "file system is not mounted";
        }

        if (command.length < 2) {
            return "directory name is not provided, usage: mkdir directory_name";
        }

        try {
            myDriver.addDirectory(myCurrentDir, command[1]);
        } catch (JFSRefuseException e) {
            return "could not create directory: " + e.getMessage();
        }

        return "";
    }

    private String mountFile(String[] command) throws JFSException {
        if (command.length < 2)
            return "file name is not provided";

        if (myDriver != null)
            umountFile();

        try {
            myDriver = new FileSystemDriver(command[1]);
            myCurrentDir = myDriver.rootInode();
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
        myDriver = null;

        return "";
    }

    public String execute(String[] command) {
        assert command.length > 0;

        try {
            switch (command[0]) {
                case "cd"    : return changeDirectory(command);
                case "create": return crateFile(command);
                case "format": return formatFile(command);
                case "ls"    : return listDirectory();
                case "mkdir" : return makeDirectory(command);
                case "mount" : return mountFile(command);
                case "umount": return umountFile();
            }

            return "unsupported command";
        } catch (JFSException e) {
            umountFile();
            return "logic error encountered: " + e.getMessage() + ",\n" + e.getStackTrace().toString();
        }
    }
}
