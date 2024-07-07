package com.dbcon;

import java.sql.*;

public class dbcon {
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/billingmanagement";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "asdf";

    public Connection con;
    //Connecting Database
    public dbcon() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            System.out.println("MySQL Connection established successfully");
        } catch (Exception e) {
            System.out.println("Error establishing database connection: " + e.getMessage());
        }
    }

    // Close connection
    public void closecon() {
        try {
            if (con != null) {
                con.close();
                System.out.println("MySQL Connection closed successfully");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }

    // Execute query
    public ResultSet executeQuery(String query) {
        try {
            Statement stmt = con.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            System.out.println("Error executing query: " + e.getMessage());
            return null;
        }
    }

    // Get connection
    public Connection getConnection() {
        return con;
    }
}
