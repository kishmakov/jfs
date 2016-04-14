package org.kshmakov;

import org.kshmakov.io.FileManager;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage: java -jar jfs.jar file_name");
            return;
        }

        try {
            FileManager myManager = new FileManager(args[0]);
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
        } catch (IOException e) {
            System.err.println("File access problems: " + e.getMessage());
        }

    }
}
