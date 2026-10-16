package com.stayease.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminAuthTest {

  /** A clock the test can move forward. */
  private static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-16T08:00:00Z");

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static final String HASH = PasswordHasher.hash("s3cret-pass", 1000);

  @Test
  void hashesAreSaltedAndVerifyOnlyTheRightPassword() {
    String other = PasswordHasher.hash("s3cret-pass", 1000);
    assertNotEquals(HASH, other);
    assertTrue(HASH.startsWith("pbkdf2_sha256:1000:"));
    assertTrue(PasswordHasher.verify("s3cret-pass", HASH));
    assertTrue(PasswordHasher.verify("s3cret-pass", other));
    assertFalse(PasswordHasher.verify("s3cret-pasS", HASH));
    assertFalse(PasswordHasher.verify("", HASH));
    assertFalse(PasswordHasher.verify(null, HASH));
  }

  @Test
  void malformedStoredHashesNeverVerify() {
    assertFalse(PasswordHasher.verify("x", null));
    assertFalse(PasswordHasher.verify("x", ""));
    assertFalse(PasswordHasher.verify("x", "plain-text"));
    assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256:abc:AAAA:BBBB"));
    assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256:0:AAAA:BBBB"));
    assertFalse(PasswordHasher.verify("x", "md5:1000:AAAA:BBBB"));
  }

  @Test
  void acceptsTheConfiguredCredentialsOnly() {
    AdminAuth auth = new AdminAuth("admin@example", HASH, new MutableClock());
    assertTrue(auth.isConfigured());
    assertTrue(auth.authenticate("admin@example", "s3cret-pass"));
    assertTrue(auth.authenticate("  admin@example ", "s3cret-pass"));
    assertFalse(auth.authenticate("admin@example", "wrong"));
    assertFalse(auth.authenticate("someone-else", "s3cret-pass"));
    assertFalse(auth.authenticate(null, null));
  }

  @Test
  void isDisabledWithoutConfiguration() {
    AdminAuth none = AdminAuth.fromEnv(Map.of(), new MutableClock());
    assertFalse(none.isConfigured());
    assertFalse(none.authenticate("", ""));
    assertFalse(new AdminAuth("admin", "", new MutableClock()).isConfigured());
    assertFalse(new AdminAuth("", HASH, new MutableClock()).isConfigured());
  }

  @Test
  void locksAfterFiveFailuresAndUnlocksAfterTheWindow() {
    MutableClock clock = new MutableClock();
    AdminAuth auth = new AdminAuth("admin", HASH, clock);
    for (int i = 0; i < AdminAuth.MAX_FAILURES; i++) {
      assertFalse(auth.isLocked());
      assertFalse(auth.authenticate("admin", "bad" + i));
    }
    assertTrue(auth.isLocked());

    clock.now = clock.now.plus(AdminAuth.WINDOW).plus(Duration.ofSeconds(1));
    assertFalse(auth.isLocked());
    assertTrue(auth.authenticate("admin", "s3cret-pass"));
  }

  @Test
  void aSuccessfulLoginClearsEarlierFailures() {
    AdminAuth auth = new AdminAuth("admin", HASH, new MutableClock());
    for (int i = 0; i < AdminAuth.MAX_FAILURES - 1; i++) {
      auth.authenticate("admin", "bad");
    }
    assertTrue(auth.authenticate("admin", "s3cret-pass"));
    for (int i = 0; i < AdminAuth.MAX_FAILURES - 1; i++) {
      auth.authenticate("admin", "bad");
    }
    assertFalse(auth.isLocked());
  }
}
