/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import citos.server.database.SqliteUserDatabase;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

public class CSVImporter {
    private CSVImporter() {
    }

    public static String getSuitablePathSeperator(String path) {
        if(path.contains("\\")) {
            return "\\";
        } else if(path.contains("/") || path.equals(".")) {
            return "/";
        } else {
            return "";
        }
    }

    public static int importCSVtoUserDatabase(String path, String filename) {
        SqliteUserDatabase sqliteUserDatabase = SqliteUserDatabase.getInstance();
        int imported = 0;
        String line = "";
        String cvsSplitBy = ",";
        Logger.getLogger(CSVImporter.class.getName()).info(path+CSVImporter.getSuitablePathSeperator(path)+filename);
        try(BufferedReader br = new BufferedReader(new FileReader(path+CSVImporter.getSuitablePathSeperator(path)+filename))){
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] user = line.split(cvsSplitBy);
                if(user.length>2) {
                    String salt = BCrypt.gensalt();
                    String hashed = BCrypt.hashpw(user[1], salt);
                    sqliteUserDatabase.insertUserInDatabase(user[0],hashed,salt,user[2]);
                    imported ++;
                }
            }
        } catch (IOException e) {
            Logger.getLogger(CSVImporter.class.getName()).info("Error reading the file");
            Logger.getLogger(CSVImporter.class.getName()).info(e.getMessage());
        }

        return imported;
    }


}
