package org.example.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
  public static Connection getConnection() throws SQLException, IOException {
    Properties properties = new Properties();
    try (InputStream inputstream =
        DBConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
      if (inputstream == null) {
        throw new RuntimeException("Không tìm thấy file database.properties");
      }
      properties.load(inputstream);
    }
    String url = properties.getProperty("db.url");
    String user = properties.getProperty("db.user");
    String pass = properties.getProperty("db.pass");
    return DriverManager.getConnection(url, user, pass);
  }
}
