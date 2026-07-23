package com.nilpo.contenttracker.core.mal

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class MalTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val accountName: String? = null,
)

data class MalPendingAuthorization(
    val verifier: String,
    val state: String,
)

interface MalCredentialStore {
    fun readTokens(): MalTokens?

    fun saveTokens(tokens: MalTokens)

    fun clear()
}

/** Stores MAL credentials encrypted by a non-exportable Android Keystore key. */
class MalTokenStore(context: Context) : MalCredentialStore {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    @Synchronized
    override fun readTokens(): MalTokens? = decrypt(preferences.getString(TokensKey, null))
        ?.let(::JSONObject)
        ?.let { json ->
            val accessToken = json.optString("access_token").takeIf { it.isNotBlank() } ?: return@let null
            val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: return@let null
            MalTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtEpochMillis = json.optLong("expires_at", 0L),
                accountName = json.optString("account_name").takeIf { it.isNotBlank() },
            )
        }

    @Synchronized
    override fun saveTokens(tokens: MalTokens) {
        val json = JSONObject()
            .put("access_token", tokens.accessToken)
            .put("refresh_token", tokens.refreshToken)
            .put("expires_at", tokens.expiresAtEpochMillis)
            .put("account_name", tokens.accountName)
        preferences.edit().putString(TokensKey, encrypt(json.toString())).commit()
    }

    @Synchronized
    fun savePendingAuthorization(authorization: MalPendingAuthorization) {
        val json = JSONObject()
            .put("verifier", authorization.verifier)
            .put("state", authorization.state)
        preferences.edit().putString(PendingAuthorizationKey, encrypt(json.toString())).commit()
    }

    @Synchronized
    fun readPendingAuthorization(): MalPendingAuthorization? =
        decrypt(preferences.getString(PendingAuthorizationKey, null))
            ?.let(::JSONObject)
            ?.let { json ->
                val verifier = json.optString("verifier").takeIf { it.isNotBlank() } ?: return@let null
                val state = json.optString("state").takeIf { it.isNotBlank() } ?: return@let null
                MalPendingAuthorization(verifier = verifier, state = state)
            }

    @Synchronized
    fun clearPendingAuthorization() {
        preferences.edit().remove(PendingAuthorizationKey).commit()
    }

    fun isSyncEnabled(): Boolean = preferences.getBoolean(SyncEnabledKey, false)

    fun setSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(SyncEnabledKey, enabled).apply()
    }

    @Synchronized
    override fun clear() {
        preferences.edit()
            .remove(TokensKey)
            .remove(PendingAuthorizationKey)
            .putBoolean(SyncEnabledKey, false)
            .commit()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(CipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return runCatching {
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            require(combined.size > GcmIvSize)
            val cipher = Cipher.getInstance(CipherTransformation)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GcmTagBits, combined.copyOfRange(0, GcmIvSize)),
            )
            cipher.doFinal(combined.copyOfRange(GcmIvSize, combined.size)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore).run {
            init(
                KeyGenParameterSpec.Builder(
                    KeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PreferencesName = "omnilog_mal_auth"
        const val TokensKey = "tokens"
        const val PendingAuthorizationKey = "pending_authorization"
        const val SyncEnabledKey = "sync_enabled"
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "omnilog_mal_tokens_v1"
        const val CipherTransformation = "AES/GCM/NoPadding"
        const val GcmIvSize = 12
        const val GcmTagBits = 128
    }
}
