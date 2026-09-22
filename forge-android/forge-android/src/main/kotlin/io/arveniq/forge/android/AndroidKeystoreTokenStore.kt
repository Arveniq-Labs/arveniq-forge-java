package io.arveniq.forge.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Persist only short-lived mobile gateway tokens using an AES key that cannot leave Android Keystore. */
interface SecureMobileTokenStore {
    suspend fun readAccessToken(): GatewayAccessToken?
    suspend fun writeAccessToken(token: GatewayAccessToken)
    suspend fun clearAccessToken()
}

class AndroidKeystoreTokenStore(
    context: Context,
    private val preferenceName: String = "arveniq.forge.mobile.tokens",
    private val preferenceKey: String = "gateway-access-token",
    private val keystoreAlias: String = "arveniq.forge.mobile.gateway-token.v1",
) : SecureMobileTokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    override suspend fun readAccessToken(): GatewayAccessToken? = withContext(Dispatchers.IO) {
        val stored = preferences.getString(preferenceKey, null) ?: return@withContext null
        try {
            val parts = stored.split('.', limit = 2)
            if (parts.size != 2) return@withContext clearAndReturnNull()
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            GatewayAccessToken.of(cipher.doFinal(ciphertext).decodeToString())
        } catch (_: Exception) {
            clearAndReturnNull()
        }
    }

    override suspend fun writeAccessToken(token: GatewayAccessToken) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(token.value.encodeToByteArray())
        val stored = "${Base64.encodeToString(cipher.iv, Base64.NO_WRAP)}.${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}"
        check(preferences.edit().putString(preferenceKey, stored).commit()) { "Unable to store mobile gateway token." }
    }

    override suspend fun clearAccessToken() = withContext(Dispatchers.IO) {
        check(preferences.edit().remove(preferenceKey).commit()) { "Unable to clear mobile gateway token." }
    }

    private fun clearAndReturnNull(): GatewayAccessToken? {
        preferences.edit().remove(preferenceKey).commit()
        return null
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keystoreAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(keystoreAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}

class KeystoreMobileCredentialProvider(private val tokenStore: SecureMobileTokenStore) : MobileCredentialProvider {
    override suspend fun getAccessToken(): GatewayAccessToken? = tokenStore.readAccessToken()
}
