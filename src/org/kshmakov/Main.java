package org.kshmakov;

import org.kshmakov.io.ConsoleManager;

import java.util.*;

//// begin
//
//class Inserter implements Runnable {
//    ConcurrentHashMap<Integer, String> map;
//    private String name;
//
//    public Inserter(ConcurrentHashMap<Integer, String> map, String name) {
//        this.map = map;
//        this.name = name;
//    }
//
//    public void run() {
//        try {
//            Random random = new Random();
//            Thread.sleep(random.nextInt(100));
//            for (int i = 0; i < 1000; i++) {
//                Thread.sleep(random.nextInt(10));
//                StringBuilder sb = new StringBuilder();
//                sb.append(name);
//                sb.append(i);
////            synchronized (map) {
//                map.put(i, sb.toString());
////            }
//            }
//        } catch (InterruptedException e) {}
//    }
//}
//
//// end

public class Main {

    public static void main(String[] args) {
        ConsoleManager consoleManager = new ConsoleManager();
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.print(consoleManager.prefix());
            String[] tokens = input.nextLine().trim().split(" ");

            if (tokens.length == 0)
                continue;

            if (tokens[0].equals("exit"))
                break;

            String result = consoleManager.execute(tokens);

            if (result.length() > 0)
                System.out.println(result);
        }

//// begin
//        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<Integer, String>();
//        new Thread(new Inserter(map, "Alice")).start();
//        new Thread(new Inserter(map, "Bob")).start();
//        new Thread(new Inserter(map, "Charlie")).start();
//        new Thread(new Inserter(map, "Daniel")).start();
//
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {}
//
//
//        Iterator it = map.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pair = (Map.Entry)it.next();
//            System.out.println(pair.getKey() + " -> " + pair.getValue());
////            it.remove();
//        }
//// end

    }
}
