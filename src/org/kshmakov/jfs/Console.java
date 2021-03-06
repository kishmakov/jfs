package org.kshmakov.jfs;

import org.kshmakov.jfs.driver.DirectoryDescriptor;
import org.kshmakov.jfs.driver.FileSystemDriver;
import org.kshmakov.jfs.driver.JFSException;
import org.kshmakov.jfs.io.FileFormatter;
import org.kshmakov.jfs.io.JFSBadFileException;
import org.kshmakov.jfs.io.NameHelper;
import org.kshmakov.jfs.io.Parameters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class Console {
    private FileSystemDriver myDriver = null;
    private String myCurrentFile = "";
    private ArrayList<String> myCurrentPath = new ArrayList<String>();
    private DirectoryDescriptor myCurrentDir;

    private static final HashMap<String, String> myUsages;

    static {
        myUsages = new HashMap<String, String>();
        myUsages.put("cd", "usage: cd directory_name\n   or  cd");
        myUsages.put("create", "usage: create file_name file_size");
        myUsages.put("exit", "usage: exit");
        myUsages.put("format", "usage: format file_name");
        myUsages.put("help", "usage: help\n   or  help command");
        myUsages.put("ls", "usage: ls");
        myUsages.put("mkdir", "usage: mkdir directory_name");
        myUsages.put("mount", "usage: mount file_name");
        myUsages.put("rm", "usage: rm file_name\n   or  rm -r directory_name");
        myUsages.put("touch", "usage: touch file_name");
        myUsages.put("umount", "usage: umount");
    }

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

    private String changeDirectory(String[] command) {
        if (command.length != 1 && command.length != 2) {
            return "invalid arguments\n" + myUsages.get(command[0]);
        }

        if (command.length == 1) {
            myCurrentDir = myDriver.rootInode();
            myCurrentPath.clear();
            return "";
        }

        try {
            DirectoryDescriptor newDescriptor = myDriver.getDirectories(myCurrentDir).get(command[1]);
            if (newDescriptor == null) {
                return "no such directory";
            }

            myCurrentDir = newDescriptor;
        } catch (JFSException e) {
            return "could not change directory, reason: " + e.getMessage();
        }

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

    private String crateFile(String[] command) {
        if (command.length < 2) {
            return "file name is not provided\n " + myUsages.get(command[0]);
        }

        if (command.length < 3) {
            return "file size is not provided\n" + myUsages.get(command[0]);
        }

        int size;

        try {
            size = Integer.parseInt(command[2]);
        } catch (NumberFormatException e) {
            return "bad size description provided";
        }

        if (size < Parameters.MIN_FS_SIZE || size > Parameters.MAX_FS_SIZE) {
            String range = "[" + Long.toString(Parameters.MIN_FS_SIZE) + ", " + Long.toString(Parameters.MAX_FS_SIZE) + "]";
            return  "requested size is not in range " + range;
        }

        try {
            RandomAccessFile file = new RandomAccessFile(command[1], "rw");
            file.setLength(size);
        } catch (IOException e) {
            return "could not create file, reason: " + e.getMessage();
        }

        return command[1] + " created";
    }

    private String formatFile(String[] command) {
        if (command.length < 2) {
            return "file name is not provided\n" + myUsages.get(command[0]);
        }

        try {
            FileFormatter formatter = new FileFormatter(command[1]);
            formatter.format();
        } catch (JFSException e) {
            return "could not format file, reason: " + e.getMessage();
        }

        return command[1] + " formatted";
    }

    private String showHelp(String[] command) {
        StringBuilder builder = new StringBuilder();

        if (command.length > 2) {
            return myUsages.get(command[0]);
        }

        if (command.length == 2) {
            return myUsages.get(command[1]) == null
                    ? "unsupported command " + command[1]
                    : myUsages.get(command[1]);
        }

        for (String key : myUsages.keySet()) {
            builder.append("--- " + key + " ---\n");
            builder.append(myUsages.get(key) + "\n");
        }

        return builder.toString();
    }

    private String listDirectory() throws JFSException {
        if (myDriver == null) {
            return "file system is not mounted";
        }

        ArrayList<String> listItems = new ArrayList<String>();
        myDriver.getDirectories(myCurrentDir).forEach((k, v) -> listItems.add("d: " + k));
        myDriver.getFiles(myCurrentDir).forEach((k,v ) -> listItems.add("f: " + k));

        Collections.sort(listItems);

        StringBuilder builder = new StringBuilder();
        for (String item : listItems) {
            if (builder.length() > 0)
                builder.append("\n");
            builder.append(item);
        }

        return builder.toString();
    }

    private String makeDirectory(String[] command) throws Exception {
        if (myDriver == null) {
            return "file system is not mounted";
        }

        if (command.length < 2) {
            return "directory name is not provided\n" + myUsages.get(command[0]);
        }

        try {
            myDriver.tryAddDirectory(myCurrentDir, command[1]);
        } catch (JFSBadFileException e) {
            throw new Exception("underlying file is corrupted");
        } catch (JFSException e) {
            return "could not create directory: " + e.getMessage();
        }

        return "";
    }

    private String mountFile(String[] command) {
        if (command.length < 2)
            return "file name is not provided\n" + myUsages.get(command[0]);

        if (myDriver != null)
            umountFile();

        try {
            myDriver = new FileSystemDriver(command[1]);
            myCurrentDir = myDriver.rootInode();
            myCurrentPath = new ArrayList<String>();
            myCurrentFile = "@" + command[1] + ":";
        } catch (JFSException e) {
            return "command could not be executed, reason: " + e.getMessage();
        }

        return "";
    }

    private String removeEntry(String[] command) throws Exception {
        if (command.length != 2 && command.length != 3) {
            return "invalid arguments\n" + myUsages.get(command[0]);
        }

        try {
            if (command.length == 3) {
                if (command[1].equals("-r")) {
                    myDriver.tryRemoveDirectory(myCurrentDir, command[2]);
                } else {
                    return "invalid arguments\n" + myUsages.get(command[0]);
                }
            } else {
                myDriver.tryRemoveFile(myCurrentDir, command[1]);
            }
        } catch (JFSBadFileException e) {
            throw new Exception("underlying file is corrupted");
        } catch (JFSException e) {
            return "cannot remove " + command[command.length - 1] + ": " + e.getMessage();
        }


        return "";
    }

    private String createFile(String[] command) throws Exception {
        if (myDriver == null) {
            return "file system is not mounted";
        }

        if (command.length < 2) {
            return "file name is not provided\n" + myUsages.get(command[0]);
        }

        try {
            myDriver.tryAddFile(myCurrentDir, command[1]);
        } catch (JFSBadFileException e) {
            throw new Exception("underlying file is corrupted");
        } catch (JFSException e) {
            return "could not create file: " + e.getMessage();
        }

        return "";

    }

    private String umountFile() {
        myCurrentFile = "";
        myCurrentPath = new ArrayList<String>();
        myDriver = null;

        return "";
    }

    public String execute(String[] command) throws Exception {
        assert command.length > 0;

        try {
            switch (command[0]) {
                case "cd":
                    return changeDirectory(command);
                case "create":
                    return crateFile(command);
                case "format":
                    return formatFile(command);
                case "help":
                    return showHelp(command);
                case "ls":
                    return listDirectory();
                case "mkdir":
                    return makeDirectory(command);
                case "mount":
                    return mountFile(command);
                case "rm":
                    return removeEntry(command);
                case "touch":
                    return createFile(command);
                case "umount":
                    return umountFile();
            }

            return "unsupported command";
        } catch (JFSBadFileException e) {
            umountFile();
            e.printStackTrace(System.out);
            throw new Exception("underlying file is corrupted");
        } catch (JFSException e) {
            return "logic error encountered: " + e.getMessage();
        }
    }
}
