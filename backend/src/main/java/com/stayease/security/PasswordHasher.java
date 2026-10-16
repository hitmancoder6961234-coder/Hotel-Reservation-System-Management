package com.stayease.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2-HMAC-SHA256 password hashes (JDK only, no extra library).
 * Stored form: {@code pbkdf2_sha256:<iterations>:<salt base64>:<hash base64>} - no '$' so it can sit in a .env file.
 * Generate one with {@code PasswordHasher.main} (see docs/CLOUD-DEPLOY.md).
 */
public final class PasswordHasher {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String PREFIX = "pbkdf2_sha256";
  private static final int DEFAULT_ITERATIONS = 310_000;
  private static final int MAX_ITERATIONS = 5_000_000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {}

  public static String hash(String password) {
    return hash(password, DEFAULT_ITERATIONS);
  }

  public static String hash(String password, int iterations) {
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] key = derive(password, salt, iterations, KEY_BITS);
    return PREFIX + ":" + iterations + ":" + Base64.getEncoder().encodeToString(salt) + ":"
        + Base64.getEncoder().encodeToString(key);
  }

  /** True when {@code password} matches {@code stored}; false for any malformed or empty input. */
  public static boolean verify(String password, String stored) {
    if (password == null || password.isEmpty() || stored == null) {
      return false;
    }
    String[] parts = stored.trim().split(":");
    if (parts.length != 4 || !parts[0].equals(PREFIX)) {
      return false;
    }
    try {
      int iterations = Integer.parseInt(parts[1]);
      if (iterations < 1 || iterations > MAX_ITERATIONS) {
        return false;
      }
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      byte[] actual = derive(password, salt, iterations, expected.length * 8);
      return MessageDigest.isEqual(expected, actual);
    } catch (IllegalArgumentException malformed) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int iterations, int bits) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("PBKDF2 is not available", e);
    } finally {
      spec.clearPassword();
    }
  }

  /** Prints the hash of the password given as the first argument; use it as ADMIN_PASSWORD_HASH. */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: PasswordHasher <password>");
      System.exit(2);
    }
    System.out.println(hash(args[0]));
  }
}
