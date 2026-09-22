package io.arveniq.forge.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Opaque, signed and expiring context authorization minted by a trusted backend. */
@JvmInline
value class ContextAssertion private constructor(val value: String) {
    companion object {
        fun of(value: String): ContextAssertion {
            require(value.trim().length in 32..16_384) {
                "Context assertions must be opaque values between 32 and 16384 characters."
            }
            return ContextAssertion(value)
        }
    }
}

/** A short-lived gateway token, never a Forge Developer API key. */
@JvmInline
value class GatewayAccessToken private constructor(val value: String) {
    companion object {
        fun of(value: String): GatewayAccessToken {
            require(value.isNotBlank()) { "A mobile gateway token is required." }
            return GatewayAccessToken(value)
        }
    }
}

fun interface MobileCredentialProvider {
    suspend fun getAccessToken(): GatewayAccessToken?
}

enum class GatewayHttpMethod { POST, PUT }

/**
 * A transport boundary for the customer relay / Forge mobile gateway. It deliberately offers no
 * Forge Developer key field and no raw authoritative resource-context field.
 */
interface MobileGatewayTransport {
    suspend fun execute(request: MobileGatewayRequest): MobileGatewayResponse
    fun stream(request: MobileGatewayRequest): Flow<ForgeMobileEvent>
}

data class MobileGatewayRequest(
    val method: GatewayHttpMethod,
    val path: String,
    val bearerToken: GatewayAccessToken,
    val body: Map<String, Any?> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
)

data class MobileGatewayResponse(
    val status: Int,
    val payload: Map<String, Any?> = emptyMap(),
    val requestId: String? = null,
)

data class MobileSession(val id: String, val expiresAt: String)
data class MobileConversation(val id: String, val createdAt: String)

data class CreateConversationInput(
    /** Application-approved alias only; backend policy resolves the actual agent. */
    val agentHandle: String? = null,
    val contextAssertion: ContextAssertion,
)

data class StreamMessageInput(
    val clientMessageId: String,
    val message: String,
    val contextAssertion: ContextAssertion,
    val afterEventId: String? = null,
)

data class PrepareUploadInput(
    val filename: String,
    val contentType: String,
    val byteLength: Long,
    val contextAssertion: ContextAssertion,
)

data class UploadPreparation(
    val attachmentId: String,
    val uploadUrl: String,
    val requiredHeaders: Map<String, String>,
    val expiresAt: String,
)

enum class PushPlatform { ANDROID, IOS }

data class ForgeMobileEvent(
    val id: String,
    val type: String,
    val conversationId: String,
    val turnId: String,
    val messageId: String? = null,
    val sequence: String,
    val data: Map<String, Any?>,
)

class MobileGatewayException(
    val status: Int,
    val requestId: String?,
    message: String,
) : RuntimeException(message)

class MobileAuthenticationException(message: String) : RuntimeException(message)

/**
 * Kotlin-first facade for the mobile integration boundary. A transport implementation is injected
 * so applications can use their approved HTTP stack, certificate policy, and offline strategy.
 */
class ForgeAndroidClient(
    private val credentialProvider: MobileCredentialProvider,
    private val transport: MobileGatewayTransport,
) {
    suspend fun createSession(): MobileSession {
        val response = execute(GatewayHttpMethod.POST, "/mobile/sessions")
        return MobileSession(response.requiredString("id"), response.requiredString("expiresAt"))
    }

    suspend fun createConversation(input: CreateConversationInput): MobileConversation {
        val response = execute(
            GatewayHttpMethod.POST,
            "/conversations",
            mapOf(
                "agentHandle" to input.agentHandle,
                "contextAssertion" to mapOf("value" to input.contextAssertion.value),
            ),
        )
        return MobileConversation(response.requiredString("id"), response.requiredString("createdAt"))
    }

    fun streamMessage(conversationId: String, input: StreamMessageInput): Flow<ForgeMobileEvent> = flow {
        require(input.message.isNotBlank()) { "A message is required." }
        val request = request(
            GatewayHttpMethod.POST,
            "/conversations/${conversationId.urlPathComponent()}/messages:stream",
            mapOf(
                "clientMessageId" to input.clientMessageId,
                "message" to input.message,
                "contextAssertion" to mapOf("value" to input.contextAssertion.value),
            ),
            input.afterEventId?.let { mapOf("Last-Event-ID" to it) } ?: emptyMap(),
        )
        emitAll(transport.stream(request))
    }

    suspend fun cancelConversationTurn(conversationId: String, turnId: String) {
        execute(GatewayHttpMethod.POST, "/conversations/${conversationId.urlPathComponent()}/turns/${turnId.urlPathComponent()}:cancel")
    }

    suspend fun prepareUpload(input: PrepareUploadInput): UploadPreparation {
        require(input.byteLength > 0) { "An upload must have a positive byte length." }
        val response = execute(
            GatewayHttpMethod.POST,
            "/uploads:prepare",
            mapOf(
                "filename" to input.filename,
                "contentType" to input.contentType,
                "byteLength" to input.byteLength,
                "contextAssertion" to mapOf("value" to input.contextAssertion.value),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val headers = response.payload["requiredHeaders"] as? Map<String, String> ?: emptyMap()
        return UploadPreparation(
            attachmentId = response.requiredString("attachmentId"),
            uploadUrl = response.requiredString("uploadUrl"),
            requiredHeaders = headers,
            expiresAt = response.requiredString("expiresAt"),
        )
    }

    suspend fun registerPushToken(platform: PushPlatform, token: String) {
        require(token.isNotBlank()) { "A push token is required." }
        execute(
            GatewayHttpMethod.PUT,
            "/devices/push-tokens",
            mapOf("platform" to platform.name.lowercase(), "token" to token),
        )
    }

    private suspend fun execute(method: GatewayHttpMethod, path: String, body: Map<String, Any?> = emptyMap()): MobileGatewayResponse {
        val response = transport.execute(request(method, path, body))
        if (response.status !in 200..299) throw MobileGatewayException(response.status, response.requestId, "Mobile gateway request failed with ${response.status}.")
        return response
    }

    private suspend fun request(
        method: GatewayHttpMethod,
        path: String,
        body: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): MobileGatewayRequest {
        val token = credentialProvider.getAccessToken()
            ?: throw MobileAuthenticationException("A short-lived mobile gateway access token is required.")
        return MobileGatewayRequest(method, path, token, body, headers)
    }
}

private fun MobileGatewayResponse.requiredString(name: String): String =
    payload[name] as? String ?: throw MobileGatewayException(status, requestId, "Gateway response is missing $name.")

private fun String.urlPathComponent(): String =
    buildString {
        for (byte in encodeToByteArray()) {
            val value = byte.toInt() and 0xff
            if ((value in 'a'.code..'z'.code) || (value in 'A'.code..'Z'.code) || (value in '0'.code..'9'.code) || value == '-'.code || value == '_'.code || value == '.'.code || value == '~'.code) {
                append(value.toChar())
            } else {
                append('%')
                append(value.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
