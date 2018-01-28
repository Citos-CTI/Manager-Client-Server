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

    public static  void importCSVtoUserDatabase(String path, String filename) {
        SqliteUserDatabase sqliteUserDatabase = SqliteUserDatabase.getInstance();

        String line = "";
        String cvsSplitBy = ",";
        try(BufferedReader br = new BufferedReader(new FileReader(path+"/"+filename))){
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] user = line.split(cvsSplitBy);
                if(user.length>2) {
                    String salt = BCrypt.gensalt();
                    String hashed = BCrypt.hashpw(user[1], salt);
                    sqliteUserDatabase.insertUserInDatabase(user[0],hashed,salt,user[2]);
                }
            }
        } catch (IOException e) {
            Logger.getLogger(CSVImporter.class.getName()).info("Error reading the file");
        }





    }


}
