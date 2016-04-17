package org.kshmakov.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ConsoleManager {

    private FileManager myManager = null;

    private String myCurrentFile = "";
    private String myCurrentPath = "";

    public String prefix() {
        return myCurrentFile + myCurrentPath + "> ";
    }

    private String crateFile(String command[]) {
        String usage = "usage: create file_name file_size";

        if (command.length < 2) {
            return "file name is not provided, " + usage;
        }

        if (command.length < 3) {
            return "file size is not provided, " + usage;
        }


        int size = 0;

        try {
            size = Integer.parseInt(command[2]);
        } catch (NumberFormatException e) {
            return "bad size description provided";
        }

        if (size < FileManager.MINIMAL_SIZE) {
            return "required size is too small, minimal size is " + Integer.toString(FileManager.MINIMAL_SIZE);
        }

        if (size > FileManager.MAXIMAL_SIZE) {
            return "required size is too big, maximal size is " + Integer.toString(FileManager.MAXIMAL_SIZE);
        }

        try {
            RandomAccessFile file = new RandomAccessFile(command[1], "rw");
            file.setLength(size);
        } catch (IOException e) {
            return "could not create file, reason: " + e.getMessage();
        }

        String formatResult = formatFile(command);
        return formatResult.isEmpty() ? "created" : formatResult;
    }

    private String formatFile(String command[]) {
        return "";
    }

    private String mountFile(String command[]) {
        if (command.length < 2)
            return "file name is not provided";

        if (myManager != null)
            umountFile();

        try {
            myManager = new FileManager(command[1]);
            myCurrentFile = "@" + command[1] + ":";
            myCurrentPath = "/";
        } catch (FileNotFoundException e) {
            System.err.println("file not found");
        } catch (IOException e) {
            System.err.println("file access problems: " + e.getMessage());
        }

        return "";
    }

    private String umountFile() {
        myCurrentFile = "";
        myCurrentPath = "";
        myManager = null;

        return "";
    }

    public String execute(String[] command) {
        assert command.length > 0;

        switch (command[0]) {
            case "create": return crateFile(command);
            case "format": return formatFile(command);
            case "mount": return mountFile(command);
            case "umount": return umountFile();
        }

        return "unsupported command";
    }
}
