package com.example.airscanner.models;

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object TokenManager {
    private const val PREF_NAME = "auth_prefs"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val TOKEN_TYPE_KEY = "token_type"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        sharedPreferences = EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String, tokenType: String) {
        sharedPreferences.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .putString(TOKEN_TYPE_KEY, tokenType)
            .apply()
    }

    fun getAccessToken(): String? = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    fun getRefreshToken(): String? = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    fun getTokenType(): String? = sharedPreferences.getString(TOKEN_TYPE_KEY, "Bearer")
    fun clearTokens() = sharedPreferences.edit().clear().apply()
    fun isLoggedIn(): Boolean = getAccessToken() != null
    fun getAuthHeader(): String? {
        val token = getAccessToken()
        val tokenType = getTokenType()
        return if (token != null) "$tokenType $token" else null
    }
}
