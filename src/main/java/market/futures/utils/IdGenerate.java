package market.futures.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class IdGenerate {

    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmssSSS");

    public static synchronized int nextRequestId() {
        return Integer.parseInt(timeFormatter.format(LocalTime.now()));
    }
}
