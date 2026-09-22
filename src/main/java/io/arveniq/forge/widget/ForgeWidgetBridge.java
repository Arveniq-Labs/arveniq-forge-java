package io.arveniq.forge.widget;

import io.arveniq.forge.ForgeDeveloperRunInput;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts an authenticated widget message to a run input. The context argument
 * must come from the host application's session and data authorization layer;
 * browserContext is intentionally excluded.
 */
public final class ForgeWidgetBridge {
  private ForgeWidgetBridge() {}
  public static ForgeDeveloperRunInput trustedRunInput(ForgeWidgetRequest request, Map<String, Object> authorizedContext) {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("prompt", request.message());
    input.put("context", authorizedContext == null ? Map.of() : new LinkedHashMap<>(authorizedContext));
    return ForgeDeveloperRunInput.of(request.clientMessageId(), input);
  }
}
