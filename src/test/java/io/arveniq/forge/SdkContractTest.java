package io.arveniq.forge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.arveniq.forge.widget.ForgeChatWidget;
import io.arveniq.forge.widget.ForgeWidgetBridge;
import io.arveniq.forge.widget.ForgeWidgetRequest;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free checks runnable with javac/java when Maven is unavailable. */
public final class SdkContractTest {
  public static void main(String[] args) throws Exception {
    ToolPackageHandlerManifest handler = ForgeManifests.createReadOnlyHandler(ForgeManifests.handler()
        .handlerRef("tool://contacts/get_summary").slug("get_summary").name("Get summary")
        .requiredScopes(List.of("contacts.read"))
        .inputSchema(Map.of("type", "object", "properties", Map.of("question", Map.of("type", "string"))))
        .outputSchema(Map.of("type", "object", "properties", Map.of("summary", Map.of("type", "string")))));
    ToolPackageManifest manifest = ForgeManifests.manifest().name("contact-tools").namespace("contacts").version("1.0.0")
        .runtime(ToolPackageRuntimeType.CONTAINER, "target/app.jar").handlers(List.of(handler)).build();
    check(ForgeManifests.validate(manifest).valid(), "valid governed manifest");
    check(handler.executionMode() == ToolExecutionMode.READ_ONLY && handler.auditPolicy().auditOnCall(), "read-only policy is enforced");

    ForgeChatWidget widget = ForgeChatWidget.builder().id("contact-agent").title("PingLead AI").agentName("Web Research Agent")
        .contextLabel("Contacts").context(Map.of("contactId", "contact_42")).endpoint("/api/contacts/agent")
        .shortcut("META+SHIFT+K").build();
    String html = widget.render();
    check(html.contains("/api/contacts/agent") && html.contains("contact_42") && html.contains("\"meta\":true") && html.contains("\"shift\":true"), "widget embeds endpoint, context, and shortcut");
    check(widget.renderLauncher().contains("data-forge-open=\"contact-agent\""), "launcher opens the matching widget");

    ForgeWidgetRequest browser = ForgeWidgetRequest.fromJson("{\"message\":\"Summarize this\",\"clientMessageId\":\"turn-1\",\"context\":{\"accountId\":\"forged\"}}");
    ForgeDeveloperRunInput runInput = ForgeWidgetBridge.trustedRunInput(browser, Map.of("accountId", "server-authorized", "contactId", "contact_42"));
    String runJson = Json.stringify(runInput);
    check(runJson.contains("server-authorized") && !runJson.contains("forged"), "server context wins over browser context");

    ForgeChatState state = ForgeChatEvents.reduce(ForgeChatState.empty(), new ForgeChatEvent("response.delta", 1, "c1", "t1", "m1", "e1", "1", "now", Map.of("delta", "Hello")));
    state = ForgeChatEvents.reduce(state, new ForgeChatEvent("response.completed", 1, "c1", "t1", "m1", "e2", "2", "now", Map.of("text", "Hello world")));
    check("Hello world".equals(state.text()), "chat reducer honors completed answer");
    clientReconnectsAnAcceptedTurn();
    System.out.println("SDK contract checks passed.");
  }
  private static void clientReconnectsAnAcceptedTurn() throws Exception {
    AtomicInteger requests = new AtomicInteger(); List<String> methods = new ArrayList<>(); List<String> cursors = new ArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> respond(requests.incrementAndGet(), exchange, methods, cursors));
    server.start();
    try {
      ForgeDeveloperClient client = new ForgeDeveloperClient(ForgeClientOptions.builder()
          .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
          .apiKey("forge_test_key").requestTimeout(Duration.ofSeconds(2)).build());
      ForgeChatState[] state = {ForgeChatState.empty()};
      client.streamMessage("c1", "m1", "hello", ForgeChatStreamOptions.builder().reconnectDelay(Duration.ofMillis(1)).build(),
          event -> state[0] = ForgeChatEvents.reduce(state[0], event));
      check(requests.get() == 2, "stream reconnects once");
      check("POST".equals(methods.get(0)) && "GET".equals(methods.get(1)), "accepted stream resumes with GET");
      check("e1".equals(cursors.get(1)), "resumed stream carries its last event ID");
      check("Hello world!".equals(state[0].text()) && "completed".equals(state[0].status()), "replayed stream is deduplicated");
    } finally { server.stop(0); }
  }
  private static void respond(int request, HttpExchange exchange, List<String> methods, List<String> cursors) throws java.io.IOException {
    methods.add(exchange.getRequestMethod()); cursors.add(exchange.getRequestHeaders().getFirst("Last-Event-Id")); exchange.getRequestBody().readAllBytes();
    String body = request == 1
        ? sse(new ForgeChatEvent("turn.accepted", 1, "c1", "t1", "t1:assistant", "e0", null, "now", Map.of()), new ForgeChatEvent("response.delta", 1, "c1", "t1", "t1:assistant", "e1", "1", "now", Map.of("delta", "Hello")))
        : sse(new ForgeChatEvent("response.delta", 1, "c1", "t1", "t1:assistant", "e1", "1", "now", Map.of("delta", "Hello")), new ForgeChatEvent("response.delta", 1, "c1", "t1", "t1:assistant", "e2", "2", "now", Map.of("delta", " world")), new ForgeChatEvent("response.completed", 1, "c1", "t1", "t1:assistant", "e3", "3", "now", Map.of("text", "Hello world!")), new ForgeChatEvent("turn.completed", 1, "c1", "t1", "t1:assistant", "e4", "4", "now", Map.of("status", "completed")));
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().add("Content-Type", "text/event-stream"); exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
  }
  private static String sse(ForgeChatEvent... events) {
    StringBuilder output = new StringBuilder(); for (ForgeChatEvent event : events) output.append("event: ").append(event.type()).append("\ndata: ").append(Json.stringify(event)).append("\n\n"); return output.toString();
  }
  private static void check(boolean value, String label) { if (!value) throw new AssertionError("Failed: " + label); }
}
