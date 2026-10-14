package utils;

import java.util.UUID;

public class TestData {

    public static String uniqueUsername() {
        return "g11_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10);
    }

    public static final String PASSWORD = "Test1234";
}