package org.kshmakov.io;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ConsoleManager {

    private FileManager myManager = null;

    private String myCurrentFile = "";
    private String myCurrentPath = "";

    public String prefix() {
        return myCurrentFile + myCurrentPath + "> ";
    }

    public String mountFile(String command[]) {
        if (command.length < 2)
            return "file name is not provided";

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

    public String execute(String[] command) {
        assert command.length > 0;

        switch (command[0]) {
            case "mount": return mountFile(command);
        }

        return "unsupported command";
    }
}
