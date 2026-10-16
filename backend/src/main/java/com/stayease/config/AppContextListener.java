package com.stayease.config;

import com.stayease.db.DataSourceDb;
import com.stayease.db.Db;
import com.stayease.repository.BookingRepository;
import com.stayease.repository.JdbcBookingRepository;
import com.stayease.repository.JdbcRoomRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.security.AdminAuth;
import com.stayease.service.BookingService;
import com.stayease.service.RoomService;
import com.stayease.web.AppServices;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates the connection pool and the services at start-up and closes the pool on shutdown. */
@WebListener
public class AppContextListener implements ServletContextListener {

  private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

  private HikariDataSource dataSource;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    AppConfig config = AppConfig.fromEnv(System.getenv());
    if (config.dbPassword().isEmpty()) {
      log.error("DB_PASSWORD is not set - database connections will fail");
    }

    HikariConfig pool = new HikariConfig();
    pool.setPoolName("stayease");
    pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
    pool.setJdbcUrl(config.jdbcUrl());
    pool.setUsername(config.dbUser());
    pool.setPassword(config.dbPassword());
    pool.setMaximumPoolSize(config.dbPoolSize());
    pool.setMinimumIdle(Math.min(2, config.dbPoolSize()));
    pool.setConnectionTimeout(5_000);
    pool.setValidationTimeout(3_000);
    pool.setMaxLifetime(25 * 60_000L);
    // Start even when MySQL is not reachable yet; requests answer 503 until it is.
    pool.setInitializationFailTimeout(-1);
    dataSource = new HikariDataSource(pool);

    Db db = new DataSourceDb(dataSource);
    RoomRepository rooms = new JdbcRoomRepository();
    BookingRepository bookings = new JdbcBookingRepository();
    Clock clock = Clock.system(config.timezone());

    event.getServletContext().setAttribute(
        AppServices.ATTRIBUTE,
        new AppServices(new RoomService(db, rooms, bookings, clock), new BookingService(db, rooms, bookings, clock)));
    // The admin session cookie is Secure (HTTPS only). COOKIE_SECURE=false allows plain-HTTP testing by IP.
    boolean secureCookie = !"false".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));
    event.getServletContext().getSessionCookieConfig().setSecure(secureCookie);
    AdminAuth adminAuth = AdminAuth.fromEnv(System.getenv(), clock);
    event.getServletContext().setAttribute(AdminAuth.ATTRIBUTE, adminAuth);
    if (!adminAuth.isConfigured()) {
      log.warn("ADMIN_USER / ADMIN_PASSWORD_HASH are not set - admin login is disabled");
    }
    log.info("StayEase backend started (database {}:{}/{}, timezone {})",
        config.dbHost(), config.dbPort(), config.dbName(), config.timezone());
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (dataSource != null) {
      dataSource.close();
    }
  }
}
