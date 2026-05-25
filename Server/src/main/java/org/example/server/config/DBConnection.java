package org.example.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp cấu hình và quản lý kết nối cơ sở dữ liệu sử dụng HikariCP Connection Pool. Đảm bảo hiệu năng
 * cao, chống rớt kết nối ngầm và gánh tải real-time.
 */
public class DBConnection {
  private static final Logger logger = Logger.getLogger(DBConnection.class.getName());
  private static final HikariDataSource dataSource;

  static {
    try {
      Properties properties = new Properties();
      try (InputStream is =
          DBConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
        if (is == null) {
          throw new IllegalStateException("Không tìm thấy file cấu hình database.properties!");
        }
        properties.load(is);
      }

      HikariConfig config = new HikariConfig();
      config.setDriverClassName("com.mysql.cj.jdbc.Driver");
      config.setJdbcUrl(properties.getProperty("db.url"));
      config.setUsername(properties.getProperty("db.username"));
      config.setPassword(properties.getProperty("db.password"));

      // Cấu hình tải và tối ưu kết nối
      config.setMaximumPoolSize(25);
      config.setMinimumIdle(5);
      config.setIdleTimeout(30000);
      config.setMaxLifetime(1800000);
      config.setConnectionTimeout(10000);

      // Tối ưu hóa hiệu năng MySQL Statements
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");

      dataSource = new HikariDataSource(config);
      logger.info("🚀 [DATABASE POOL] Đã kích hoạt hệ thống HikariCP bọc thép thành công!");

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi nghiêm trọng khi khởi tạo cấu hình DB Pool", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  private DBConnection() {}

  /**
   * Lấy một kết nối sạch từ Connection Pool.
   *
   * @return Đối tượng {@link Connection} sẵn sàng sử dụng.
   * @throws SQLException Nếu không thể mượn kết nối từ pool hoặc pool chưa khởi tạo.
   */
  public static Connection getConnection() throws SQLException {
    if (dataSource == null) {
      throw new SQLException("Hệ thống Database DataSource chưa được khởi tạo!");
    }
    return dataSource.getConnection();
  }

  /** Đóng toàn bộ kết nối trong Pool an toàn khi shutdown máy chủ. */
  public static void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      logger.info("💤 [DATABASE POOL] Đã đóng toàn bộ kết nối DB an toàn.");
    }
  }
}
