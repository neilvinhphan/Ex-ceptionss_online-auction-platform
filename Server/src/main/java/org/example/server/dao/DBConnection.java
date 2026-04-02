package org.example.server.dao;


import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {
  public static Connection getConnection() throws Exception {
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

  static void main() {
    try (Connection connection = getConnection()) {
      if (connection != null) {
        System.out.println("Ket noi thanh cong");
        connection.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
