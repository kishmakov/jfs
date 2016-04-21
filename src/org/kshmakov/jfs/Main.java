package org.kshmakov.jfs;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

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

    public static void main(String[] args) throws UnsupportedEncodingException, JFSException {
        Console console = new Console();
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
