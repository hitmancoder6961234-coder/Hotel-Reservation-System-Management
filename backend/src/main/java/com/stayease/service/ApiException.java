package com.stayease.service;

/**
 * A business/validation error that maps to an HTTP status and a user-facing message.
 * Unchecked so it can be thrown from inside transactional lambdas.
 */
public class ApiException extends RuntimeException {

  private final int status;

  public ApiException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
