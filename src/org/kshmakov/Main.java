package org.kshmakov;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage: java -jar jfs.jar file_name");
            return;
        }

        for (String arg: args) {
            System.out.println("argument: " + arg);
        }
    }
}
