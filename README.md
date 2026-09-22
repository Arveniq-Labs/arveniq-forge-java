# Arveniq Forge Java

Java 17 SDK for server-side applications integrating with Arveniq Forge O/S, plus an embeddable dark AI-agent drawer modeled on the supplied PingLead design.

It carries the same responsibilities as `arveniq-forge-sdk`:

- `ForgeDeveloperClient` discovers agents/workflows, triggers and polls runs, and relays SSE conversation events.
- `ForgeToolPackagesClient` manages governed Tool Package definitions and deployments.
- `ForgeManifests` creates and validates governed tool manifests before submission.
- `ForgeChatWidget` renders a configurable assistant drawer with quick actions, a launcher, a keyboard shortcut, streamed output, and context payload support.

The SDK uses only the JDK. It has no JSON, web-framework, or reactive-runtime dependency.

## Install

```xml
<dependency>
  <groupId>io.arveniq</groupId>
  <artifactId>arveniq-forge-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

Build locally with Maven:

```bash
mvn package
```

## Call Forge from your Java backend

Create one client from server-side configuration. Do not add the Forge API key to a browser bundle, mobile app, or widget configuration.

```java
import io.arveniq.forge.*;
import java.time.Duration;
import java.util.Map;

ForgeDeveloperClient forge = new ForgeDeveloperClient(ForgeClientOptions.builder()
    .baseUrl(System.getenv().getOrDefault("FORGE_API_URL", "https://forge-os.io/v1"))
    .apiKey(System.getenv("FORGE_API_KEY"))
    .userAgent("contacts-api/1.0")
    .build());

ForgeDeveloperRun run = forge.triggerAgentRun(
    System.getenv("FORGE_CONTACTS_AGENT_ID"),
    ForgeDeveloperRunInput.of("contacts-turn:" + turnId,
        Map.of("prompt", userMessage, "context", authorizedContactContext)));

ForgeDeveloperRun complete = forge.waitForRun(run.id(), ForgeRunWaitOptions.builder()
    .timeout(Duration.ofSeconds(60))
    .build());
if ("completed".equals(complete.status()) || "succeeded".equals(complete.status())) {
  System.out.println(complete.finalAnswer());
}
```

The client accepts an absolute HTTPS base URL. HTTP is restricted to loopback hosts. It strips a pasted `Bearer ` prefix, assigns an `x-request-id`, and never accepts a Forge user header.

## Render the agent widget

`ForgeChatWidget` is server-rendered HTML, CSS, and small framework-neutral JavaScript. Its visual language follows the image: a deep navy right drawer, lavender agent mark, agent selector, context line, action tiles, and bottom composer. It opens from a launcher or the configurable keyboard shortcut.

```java
import io.arveniq.forge.widget.ForgeChatWidget;
import java.util.Map;

ForgeChatWidget widget = ForgeChatWidget.builder()
    .id("contacts-ai")
    .title("PingLead AI")
    .agentName("Web Research Agent")
    .agentsAvailable(7)
    .contextLabel("Contacts")
    .context(Map.of("contactId", contact.id(), "screen", "contacts"))
    .endpoint("/api/contacts/agent")
    .shortcut("META+K")       // Also supports CTRL+SHIFT+K, ALT+J, etc.
    .build();

out.print(widget.renderLauncher()); // Place in the host page header.
out.print(widget.render());         // Place once near the end of <body>.
```

The widget sends this JSON to `endpoint` when the user submits a message:

```json
{
  "message": "Summarize this customer or lead.",
  "clientMessageId": "stable-uuid",
  "context": { "contactId": "contact_42", "screen": "contacts" }
}
```

Its endpoint expects Server-Sent Events and progressively renders `response.delta` and `response.completed` events. The endpoint is owned by your application; it authenticates the browser using your normal session and is the only layer that calls Forge.

### Safely supply context to an AI agent

Widget context is useful for presenting client state, but the browser can alter it. Treat `ForgeWidgetRequest.browserContext()` as untrusted. Resolve the user, tenant, contact, portfolio, and permissions on the server, then use `ForgeWidgetBridge` to build the actual agent input:

```java
import io.arveniq.forge.*;
import io.arveniq.forge.widget.*;

// Inside an authenticated application endpoint:
ForgeWidgetRequest widgetRequest = ForgeWidgetRequest.fromJson(requestBody);
Map<String, Object> trustedContext = contactService.authorizedAgentContext(session.userId());

ForgeDeveloperRun run = forge.triggerAgentRun(
    contactsAgentId,
    ForgeWidgetBridge.trustedRunInput(widgetRequest, trustedContext));
```

`trustedRunInput` intentionally omits the browser context and submits `{ prompt, context }` to Forge with the stable `clientMessageId` as its idempotency key. This retains the requested contextual-agent behavior without allowing an edited `contactId` or account ID to cross the authorization boundary.

If the endpoint relays a conversation instead of an asynchronous run, use the streaming client and forward each event unchanged:

```java
forge.streamMessage(conversationId, clientMessageId, message,
    ForgeChatStreamOptions.builder().maxReconnects(5).build(),
    event -> sse.write("event: " + event.type() + "\ndata: " + Json.stringify(event) + "\n\n"));
```

Only one turn may be active in a conversation. Closing a browser connection does not cancel it; call `cancelConversationTurn` from an authenticated Stop endpoint when cancellation is intended.

## Create a governed tool manifest

```java
ToolPackageHandlerManifest handler = ForgeManifests.createReadOnlyHandler(
    ForgeManifests.handler()
        .handlerRef("tool://contacts/get_summary")
        .slug("get_summary")
        .name("Get contact summary")
        .requiredScopes(List.of("contacts.read"))
        .inputSchema(Map.of("type", "object", "properties", Map.of("question", Map.of("type", "string"))))
        .outputSchema(Map.of("type", "object", "properties", Map.of("summary", Map.of("type", "string")))));

ToolPackageManifest manifest = ForgeManifests.manifest()
    .name("contact-tools")
    .namespace("contacts")
    .version("1.0.0")
    .runtime(ToolPackageRuntimeType.CONTAINER, "target/contact-tools.jar")
    .handlers(List.of(handler))
    .build();

ForgeManifests.assertValid(manifest);
```

Read-only helpers cannot be changed to a write or external-action handler and always retain call/input/output audit metadata. Validation also checks namespaces, handler references, explicit scopes, schemas, valid environments, risk/approval policy, and audit policy.

## CLI

The `io.arveniq.forge.ForgeTool` main class mirrors the TypeScript package’s Tool Package commands:

```bash
java -jar target/arveniq-forge-java-0.1.0.jar validate --manifest tool-package.json
java -jar target/arveniq-forge-java-0.1.0.jar create-version \
  --package-id pkg_123 --manifest tool-package.json --version 1.0.0 --commit-sha abc123
```

It reads `FORGE_API_URL` and `FORGE_API_KEY`, or accepts `--base-url` and `--api-key`. Keep the key in a secret store; do not pass it to client-side code.

## Verify without Maven

```bash
classes=$(mktemp -d)
javac --release 17 --add-modules jdk.httpserver -d "$classes" $(rg --files src/main/java src/test/java -g '*.java')
java --add-modules jdk.httpserver -cp "$classes" io.arveniq.forge.SdkContractTest
```

## License

Apache-2.0
