/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.database;

import citos.server.packet.CdrPacket;

import java.io.File;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlLiteCallRecordDatabase {

    private static final String JDBC = "jdbc:sqlite:";
    private String database;
    private static String path = "";
    private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    // Beispiel database: "settingsAndData.db"
    public SqlLiteCallRecordDatabase() {
        database = path + "callrecords.db";
        File f = new File(path + "callrecords.db");
        if (f.exists() && !f.isDirectory()) {
            try(Connection connection = DriverManager.getConnection(JDBC + database)) {
                String query = "SELECT name FROM sqlite_master WHERE type=?";
                try (PreparedStatement ptsm = connection.prepareStatement(query)) {
                    ptsm.setString(1, "table");
                    ResultSet table = ptsm.executeQuery();
                    ArrayList<String> check = new ArrayList<>();
                    while(table.next()) {
                        check.add(table.getString("name"));
                    }
                    if(!(check.contains("records"))){
                        throw new SQLException("Database seems not to have the right scheme");
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try (Connection con = DriverManager.getConnection(JDBC + database); Statement statement = con.createStatement()) {
                statement.executeUpdate("create table records (id Integer PRIMARY KEY AUTOINCREMENT, source text, destination text, starttime int, duration real, disposition int, internalCall int, prefixTrunk int, countryCode int)");
                //TODO if new database is created the inital record with id  = 1 should be added  or else
            } catch (SQLException e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    public static void setWorkingDir(String dir) {
        path = dir;
    }



    public ResultSet query(String query) {
        try (Connection con = DriverManager.getConnection(JDBC + database); Statement statement = con.createStatement()) {
            statement.setQueryTimeout(10);
            return statement.executeQuery(query);
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    public void update(String update) {
        try (Connection con = DriverManager.getConnection(JDBC + database); Statement statement = con.createStatement()) {
            statement.setQueryTimeout(10);
            statement.executeUpdate(update);
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }


    public String insertCdrInDatabase(CdrPacket cdrPacket) {
        String prepareString = "Select max(id) from records where source=? and destination=? and starttime=?";

        try (Connection con = DriverManager.getConnection(JDBC + database); Statement statement = con.createStatement(); PreparedStatement ptsm = con.prepareStatement(prepareString)) {
            statement.setQueryTimeout(10);
            Date time = new Date(cdrPacket.getStartTime());
            long longTime = time.getTime();
            final String query = "INSERT INTO records (source , destination, starttime, duration, disposition, internalCall, prefixTrunk, countryCode) " +
                    "Values ('" + cdrPacket.getSource() + "','" + cdrPacket.getDestination()
                    + "' , " + longTime + ", " + cdrPacket.getDuration() + " , "
                    + cdrPacket.getDisposition() + " , " + (cdrPacket.isInternalCall() ? 1 : 0) + " , " +cdrPacket.getPrefix()+ " , " +cdrPacket.getCountryCode()+" )";
            statement.execute(query);
            statement.closeOnCompletion();

            ptsm.setString(1,cdrPacket.getSource());
            ptsm.setString(2, cdrPacket.getDestination());
            ptsm.setLong(3,longTime);
            ptsm.setQueryTimeout(10);
            ResultSet resultSet = ptsm.executeQuery();
            String databaseId = String.valueOf(resultSet.getInt(1));
            resultSet.close();
            return databaseId;
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
        }
        return "-1";
    }

    public List<CdrPacket> getLastNCallsStartingFrom(String extension, int start, int count) {
        String prepareString = "Select * from (Select * from records where source=? or destination=? order by id desc LIMIT ?,?) order by starttime asc";
        List<CdrPacket> cdr = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(JDBC + database); PreparedStatement ptsm = con.prepareStatement(prepareString)) {
            ptsm.setString(1,extension);
            ptsm.setString(2, extension);
            ptsm.setInt(3,start);
            ptsm.setInt(4,count);
            ptsm.setQueryTimeout(10);

            ResultSet rs = ptsm.executeQuery();
            while (rs.next()) {
                cdr.add(new CdrPacket(rs.getString(2), rs.getString(3),rs.getLong(4) , rs.getLong(5), rs.getInt(6),
                        (rs.getInt(7) == 1), rs.getInt(8), rs.getInt(9)));
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return cdr;
        }
        return cdr;
    }

    public long createLongFromDate(String databaseDate) {
        try {
            Date date = dateFormat.parse(databaseDate);
                return date.getTime();
        } catch (ParseException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,"Database seems to have integrity issues", e);
            return 0;
        }
    }

    public int count(String extension) {
        int found = 0;
        String prepareString = "Select count(id) from records where source=? or destination=?";
        try (Connection con = DriverManager.getConnection(JDBC + database); PreparedStatement ptsm = con.prepareStatement(prepareString)) {
            ptsm.setString(1,extension);
            ptsm.setString(2, extension);
            ptsm.setQueryTimeout(10);
            ResultSet rs = ptsm.executeQuery();
            while (rs.next()) {
                found = rs.getInt(1);
            }
            rs.close();
            return found;
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return found;
        }
    }

    public void removeCdr(long timeStamp, String source, String destination) {
        String prepareString = "Delete from records where starttime=? and destination=? and source=?";
        try (Connection con = DriverManager.getConnection(JDBC + database); PreparedStatement ptsm = con.prepareStatement(prepareString)) {
            ptsm.setLong(1,timeStamp);
            ptsm.setString(2, destination);
            ptsm.setString(3,source);
            ptsm.setQueryTimeout(10);
            ptsm.execute();
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<CdrPacket> getSearchedCdr(String extension, String search, int amount, boolean strict){
        String prepareString = "Select * from records where (source like ? and destination like ?) or (destination LIKE ? And source like ?) ORDER BY starttime asc LIMIT ?";
        List<CdrPacket> cdr = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(JDBC + database); PreparedStatement ptsm = con.prepareStatement(prepareString)) {
            // strict -> sometimes its not good to search with % -> this is the case if we know the exact number
            if(strict) {
                ptsm.setString(1,  search);
                ptsm.setString(3, search);
            } else {
                ptsm.setString(1,"%" + search + "%");
                ptsm.setString(3, "%" + search+ "%");
            }
            ptsm.setString(2, extension);
            ptsm.setString(4, extension);
            ptsm.setInt(5,amount);
            ptsm.setQueryTimeout(10);
            ResultSet rs = ptsm.executeQuery();
            while (rs.next()) {
                cdr.add(new CdrPacket(rs.getString(2), rs.getString(3),rs.getLong(4) , rs.getLong(5), rs.getInt(6),
                        (rs.getInt(7) == 1), rs.getInt(8), rs.getInt(9)));
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return cdr;
        }
        return cdr;


    }

}
