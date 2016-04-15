package org.kshmakov;

import org.kshmakov.io.FileManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// begin

class Inserter implements Runnable {
    ConcurrentHashMap<Integer, String> map;
    private String name;

    public Inserter(ConcurrentHashMap<Integer, String> map, String name) {
        this.map = map;
        this.name = name;
    }

    public void run() {
        try {
            Random random = new Random();
            Thread.sleep(random.nextInt(100));
            for (int i = 0; i < 1000; i++) {
                Thread.sleep(random.nextInt(10));
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                sb.append(i);
//            synchronized (map) {
                map.put(i, sb.toString());
//            }
            }
        } catch (InterruptedException e) {}
    }
}

// end

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

// begin
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<Integer, String>();
        new Thread(new Inserter(map, "Alice")).start();
        new Thread(new Inserter(map, "Bob")).start();
        new Thread(new Inserter(map, "Charlie")).start();
        new Thread(new Inserter(map, "Daniel")).start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}


        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " -> " + pair.getValue());
//            it.remove();
        }
// end

    }
}
