package com.stayease.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * The single administrator account. Username and password HASH come from the environment
 * (ADMIN_USER, ADMIN_PASSWORD_HASH) - never from the source code. Without them admin login is disabled.
 * Failed logins are throttled: 5 failures within 15 minutes lock logins until the window passes.
 */
public class AdminAuth {

  public static final String ATTRIBUTE = AdminAuth.class.getName();
  static final int MAX_FAILURES = 5;
  static final Duration WINDOW = Duration.ofMinutes(15);

  private final String username;
  private final String passwordHash;
  private final Clock clock;
  private final Deque<Instant> failures = new ArrayDeque<>();

  public AdminAuth(String username, String passwordHash, Clock clock) {
    this.username = username == null ? "" : username.trim();
    this.passwordHash = passwordHash == null ? "" : passwordHash.trim();
    this.clock = clock;
  }

  public static AdminAuth fromEnv(Map<String, String> env, Clock clock) {
    return new AdminAuth(env.get("ADMIN_USER"), env.get("ADMIN_PASSWORD_HASH"), clock);
  }

  public boolean isConfigured() {
    return !username.isEmpty() && !passwordHash.isEmpty();
  }

  public String username() {
    return username;
  }

  public synchronized boolean isLocked() {
    purge();
    return failures.size() >= MAX_FAILURES;
  }

  /** Checks the credentials (constant-time comparisons) and records a failure when they are wrong. */
  public boolean authenticate(String candidateUser, String candidatePassword) {
    // Always do both checks so a wrong username costs the same time as a wrong password.
    boolean userOk = MessageDigest.isEqual(
        username.getBytes(StandardCharsets.UTF_8),
        (candidateUser == null ? "" : candidateUser.trim()).getBytes(StandardCharsets.UTF_8));
    boolean passwordOk = PasswordHasher.verify(candidatePassword, passwordHash);
    boolean ok = isConfigured() && userOk && passwordOk;
    synchronized (this) {
      if (ok) {
        failures.clear();
      } else {
        failures.addLast(clock.instant());
      }
    }
    return ok;
  }

  private void purge() {
    Instant cutoff = clock.instant().minus(WINDOW);
    while (!failures.isEmpty() && failures.peekFirst().isBefore(cutoff)) {
      failures.removeFirst();
    }
  }
}
