/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ConfigLoader {
    private ConfigLoader() {
    }

    public static Map<String, String> loadConfig() {
        HashMap<String, String> result = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get("server.conf"))) {
            try {
                stream.forEach(str -> result.put(str.split("=")[0], str.split("=")[1]));
            } catch (Exception ex) {
                System.out.println("Faulty server.conf. Please see documentation for working examples.");
            }
        } catch (IOException e) {
            System.out.println("No server.conf found. Server exits!");
            System.exit(0);
        }
        return result;
    }

}
