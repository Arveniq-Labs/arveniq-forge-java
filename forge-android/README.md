# Forge Android

Kotlin-first Android client for the Forge mobile integration protocol. It is intentionally independent of the Java/Maven Developer SDK in this repository.

## Security boundary

- Never configure this module with a Forge Developer API key.
- Obtain a short-lived, audience-bound gateway token from an authenticated customer backend or Forge mobile integration gateway.
- Pass only an opaque `ContextAssertion` minted by that trusted service. Do not place an account, portfolio, tenant, contact, or other authoritative resource identifier in a message request.
- Use `AndroidKeystoreTokenStore` only for short-lived gateway tokens; renewal remains an application/backend responsibility.

## Setup

Include this standalone Gradle build as an included build or publish `:forge-android` to your artifact repository. Inject a `MobileGatewayTransport` implemented with the app's approved networking stack. The transport receives a structured request and returns protocol events; it is the natural place to apply certificate pinning, proxy policy, and observability.

```kotlin
val tokenStore = AndroidKeystoreTokenStore(applicationContext)
val client = ForgeAndroidClient(
    credentialProvider = KeystoreMobileCredentialProvider(tokenStore),
    transport = gatewayTransport,
)

val assertion = ContextAssertion.of(assertionReturnedByYourBackend)
client.streamMessage(
    conversationId = savedConversationId,
    input = StreamMessageInput(
        clientMessageId = UUID.randomUUID().toString(),
        message = userMessage,
        contextAssertion = assertion,
        afterEventId = savedLastEventId,
    ),
).collect { event -> render(event) }
```

The app may resume a stream after foregrounding using a persisted `conversationId`, `turnId`, and `afterEventId`; it must not resend the user message once the gateway has accepted a turn.
