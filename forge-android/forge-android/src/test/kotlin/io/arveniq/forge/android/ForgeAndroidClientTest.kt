package io.arveniq.forge.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ForgeAndroidClientTest {
    @Test
    fun `conversation request contains an opaque assertion and gateway token only`() = runTest {
        var captured: MobileGatewayRequest? = null
        val client = ForgeAndroidClient(
            credentialProvider = MobileCredentialProvider { GatewayAccessToken.of("short-lived-token") },
            transport = object : MobileGatewayTransport {
                override suspend fun execute(request: MobileGatewayRequest): MobileGatewayResponse {
                    captured = request
                    return MobileGatewayResponse(201, mapOf("id" to "conversation-1", "createdAt" to "2026-09-22T10:00:00Z"))
                }

                override fun stream(request: MobileGatewayRequest): Flow<ForgeMobileEvent> = emptyFlow()
            },
        )

        client.createConversation(CreateConversationInput(contextAssertion = ContextAssertion.of("trusted-opaque-context-assertion-value-001")))
        assertEquals("short-lived-token", captured?.bearerToken?.value)
        assertEquals("trusted-opaque-context-assertion-value-001", (captured?.body?.get("contextAssertion") as Map<*, *>)["value"])
    }
}
