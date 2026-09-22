package io.arveniq.forge;

/** Returned when Forge accepts the connection but rejects the API request. */
public final class ForgeApiError extends RuntimeException {
  private final int status;
  private final String responseBody;
  private final String requestId;

  public ForgeApiError(String message, int status, String responseBody, String requestId) {
    super(message);
    this.status = status;
    this.responseBody = responseBody;
    this.requestId = requestId;
  }

  public int status() { return status; }
  public String responseBody() { return responseBody; }
  public String requestId() { return requestId; }
}
