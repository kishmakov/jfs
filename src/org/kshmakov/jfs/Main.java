package org.kshmakov.jfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Console console = new Console();

        if (args.length > 0) {
            try {
                Scanner scanner = new Scanner(new File(args[0]));
                while (scanner.hasNextLine()) {
                    console.execute(scanner.nextLine().trim().split(" "));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (args.length < 2 || !args[1].equals("continue")) {
                return;
            }
        }

        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.print(console.prefix());
            String[] tokens = input.nextLine().trim().split(" ");

            if (tokens.length == 0)
                continue;

            if (tokens[0].equals("exit"))
                break;

            String result = console.execute(tokens);

            if (result.length() > 0)
                System.out.println(result);
        }
    }
}
