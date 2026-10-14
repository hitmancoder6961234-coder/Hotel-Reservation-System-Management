package com.stayease.config;

import java.time.ZoneId;
import java.util.Map;

/**
 * Application configuration read from environment variables.
 * Secrets (database password) are never hard-coded: they come from the environment
 * (Docker Compose passes them from the untracked .env file).
 */
public record AppConfig(
    String dbHost,
    int dbPort,
    String dbName,
    String dbUser,
    String dbPassword,
    String dbSslMode,
    int dbPoolSize,
    ZoneId timezone) {

  public static AppConfig fromEnv(Map<String, String> env) {
    return new AppConfig(
        get(env, "DB_HOST", "localhost"),
        Integer.parseInt(get(env, "DB_PORT", "3306")),
        get(env, "DB_NAME", "stayease"),
        get(env, "DB_USER", "stayease_app"),
        get(env, "DB_PASSWORD", ""),
        get(env, "DB_SSL_MODE", "PREFERRED"),
        Integer.parseInt(get(env, "DB_POOL_SIZE", "10")),
        ZoneId.of(get(env, "APP_TIMEZONE", "Asia/Kolkata")));
  }

  public String jdbcUrl() {
    return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
        + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
        + "&sslMode=" + dbSslMode + "&characterEncoding=UTF-8";
  }

  private static String get(Map<String, String> env, String key, String fallback) {
    String value = env.get(key);
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
