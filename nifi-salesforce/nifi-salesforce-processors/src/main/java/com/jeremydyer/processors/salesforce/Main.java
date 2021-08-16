package com.jeremydyer.processors.salesforce;

import java.time.Instant;

/**
 * Created by jdyer on 8/8/16.
 */
public class Main {

    public static void main(String args[]) {
        Instant now = Instant.now();

        String s = "2005-02-25 11:50:11.579410";
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(s);
        System.out.println(ts.toString());
        int microFraction = ts.getNanos() / 1000;

//        StringBuilder sb = new StringBuilder(fmt.format(ts));
//        String tail = String.valueOf(microFraction);
//        for (int i = 0; i < 6 - tail.length(); i++) {
//            sb.append('0');
//        }
//        sb.append(tail);
//        System.out.println(sb.toString());

        String value = String.format("");
    }
}
