package com.nellex.helpers;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Class to handle connection to the SQL database
 * 
 * @author nellex
 */
public class DatabaseConnector {
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/Wiki";

	// Database credentials
	static final String USER = "root";
	static final String PASS = "root";
	static Connection conn = null;

	static Connection connectDB() {
		if (conn != null)
			return conn;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
}
