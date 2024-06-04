package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnect {
    
    private static String user;
    private static String userPassword;
    private static String URL;

    public Connection connectWithOracle(String _user, String _password, String host, String port, String edition) throws SQLException {
       user = _user;
        userPassword = _password;
        URL = "jdbc:oracle:thin:@" + host + ":" + port + ":" + edition;
        return DriverManager.getConnection(URL, user, userPassword);
    }
}
