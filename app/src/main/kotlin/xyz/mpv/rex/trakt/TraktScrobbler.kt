package xyz.mpv.rex.trakt

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class TraktScrobbler(
  private val client: OkHttpClient,
  private val json: Json,
  private val preferences: TraktPreferences,
) {
  companion object {
    private const val TAG = "TraktScrobbler"
    private const val BASE_URL = "https://api.trakt.tv"
    private const val AUTHORIZE_URL = "https://trakt.tv/oauth/authorize"
    private const val API_VERSION = "2"
    private const val REDIRECT_URI = "mpvrex://trakt-callback"
  }

  private val jsonMediaType = "application/json".toMediaType()
  private val apiJson = Json(json) { explicitNulls = false }

  private fun ensureAuth(): Boolean {
    if (!preferences.isTokenValid()) {
      Log.w(TAG, "Token expired or missing, attempting refresh")
      return refreshTokenBlocking()
    }
    return true
  }

  private fun refreshTokenBlocking(): Boolean {
    val refreshToken = preferences.refreshToken.get()
    if (refreshToken.isBlank()) {
      Log.e(TAG, "No refresh token available")
      return false
    }
    val clientId = preferences.clientId.get()
    val clientSecret = preferences.clientSecret.get()
    if (clientId.isBlank() || clientSecret.isBlank()) {
      Log.e(TAG, "Missing client credentials")
      return false
    }

    return try {
      val body = TraktRefreshTokenRequest(
        refreshToken = refreshToken,
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = REDIRECT_URI,
      )
      val bodyJson = apiJson.encodeToString(TraktRefreshTokenRequest.serializer(), body)
      val request = Request.Builder()
        .url("$BASE_URL/oauth/token")
        .post(bodyJson.toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val responseBody = response.body?.string() ?: return false
          val tokenResponse = apiJson.decodeFromString(TraktTokenResponse.serializer(), responseBody)
          preferences.saveTokens(tokenResponse)
          Log.d(TAG, "Token refreshed successfully")
          true
        } else {
          Log.e(TAG, "Token refresh failed: ${response.code}")
          preferences.clearTokens()
          false
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Token refresh error", e)
      false
    }
  }

  fun getAuthUrl(): String {
    val clientId = preferences.clientId.get()
    val state = UUID.randomUUID().toString()
    preferences.oauthState.set(state)
    return AUTHORIZE_URL.toHttpUrl().newBuilder()
      .addQueryParameter("response_type", "code")
      .addQueryParameter("client_id", clientId)
      .addQueryParameter("redirect_uri", REDIRECT_URI)
      .addQueryParameter("state", state)
      .build()
      .toString()
  }

  @Synchronized
  fun claimOAuthState(state: String): Boolean {
    val expectedState = preferences.oauthState.get()
    if (state.isBlank() || expectedState.isBlank() || state != expectedState) return false
    preferences.oauthState.set("")
    return true
  }

  suspend fun exchangeCodeForToken(code: String): Result<TraktTokenResponse> = withContext(Dispatchers.IO) {
    try {
      val clientId = preferences.clientId.get()
      val clientSecret = preferences.clientSecret.get()
      if (clientId.isBlank() || clientSecret.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Missing client credentials"))
      }

      val body = TraktTokenRequest(
        code = code,
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = REDIRECT_URI,
      )
      val bodyJson = apiJson.encodeToString(TraktTokenRequest.serializer(), body)
      val request = Request.Builder()
        .url("$BASE_URL/oauth/token")
        .post(bodyJson.toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
          val tokenResponse = apiJson.decodeFromString(TraktTokenResponse.serializer(), responseBody)
          preferences.saveTokens(tokenResponse)
          preferences.oauthState.set("")
          Log.d(TAG, "Token exchange successful")
          Result.success(tokenResponse)
        } else {
          Log.e(TAG, "Token exchange failed: ${response.code} $responseBody")
          Result.failure(RuntimeException("Token exchange failed: ${response.code}"))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Token exchange error", e)
      Result.failure(e)
    }
  }

  suspend fun fetchUsername(): Result<String> = withContext(Dispatchers.IO) {
    try {
      if (!ensureAuth()) return@withContext Result.failure(IllegalStateException("Not authenticated"))

      val request = Request.Builder()
        .url("$BASE_URL/users/settings")
        .addHeader("trakt-api-version", API_VERSION)
        .addHeader("trakt-api-key", preferences.clientId.get())
        .addHeader("Authorization", "Bearer ${preferences.accessToken.get()}")
        .build()

      client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
          val settings = apiJson.decodeFromString(TraktSettingsResponse.serializer(), responseBody)
          val userResponse = settings.user
          val username = userResponse.ids?.slug ?: userResponse.username ?: "Unknown"
          preferences.username.set(username)
          Log.d(TAG, "Username fetched: $username")
          Result.success(username)
        } else {
          Log.e(TAG, "Fetch username failed: ${response.code}")
          Result.failure(RuntimeException("Failed to fetch username: ${response.code}"))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Fetch username error", e)
      Result.failure(e)
    }
  }

  suspend fun scrobble(
    action: String,
    request: TraktScrobbleRequest,
    allowRefresh: Boolean = true,
  ): Result<TraktScrobbleResponse?> = withContext(Dispatchers.IO) {
    try {
      if (!ensureAuth()) return@withContext Result.failure(IllegalStateException("Not authenticated"))

      val bodyJson = apiJson.encodeToString(TraktScrobbleRequest.serializer(), request)
      Log.d(TAG, "Scrobble $action: $bodyJson")

      val httpRequest = Request.Builder()
        .url("$BASE_URL/scrobble/$action")
        .post(bodyJson.toRequestBody(jsonMediaType))
        .addHeader("trakt-api-version", API_VERSION)
        .addHeader("trakt-api-key", preferences.clientId.get())
        .addHeader("Authorization", "Bearer ${preferences.accessToken.get()}")
        .build()

      client.newCall(httpRequest).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        when (response.code) {
          in 200..299 -> {
            if (responseBody.isNotBlank()) {
              val scrobbleResponse = apiJson.decodeFromString(TraktScrobbleResponse.serializer(), responseBody)
              Log.d(TAG, "Scrobble $action OK: action=${scrobbleResponse.action}")
              Result.success(scrobbleResponse)
            } else {
              Result.success(null)
            }
          }
          409 -> {
            Log.d(TAG, "Scrobble $action: duplicate (409)")
            Result.success(null)
          }
          422 -> {
            Log.w(TAG, "Scrobble $action rejected (422): $responseBody")
            Result.failure(RuntimeException("Scrobble rejected: $responseBody"))
          }
          401, 403 -> {
            Log.e(TAG, "Scrobble $action: auth failed (${response.code})")
            if (allowRefresh && refreshTokenBlocking()) {
              scrobble(action, request, allowRefresh = false)
            } else {
              Result.failure(RuntimeException("Authentication failed"))
            }
          }
          else -> {
            Log.e(TAG, "Scrobble $action failed: ${response.code} $responseBody")
            Result.failure(RuntimeException("Scrobble failed: ${response.code}"))
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Scrobble $action error", e)
      Result.failure(e)
    }
  }

  fun logout() {
    preferences.clearTokens()
  }
}
