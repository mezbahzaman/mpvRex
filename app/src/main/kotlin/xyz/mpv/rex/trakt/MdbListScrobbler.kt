package xyz.mpv.rex.trakt

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MdbListScrobbler(
  private val client: OkHttpClient,
  private val json: Json,
  private val preferences: MdbListPreferences,
) {
  companion object {
    private const val TAG = "MdbListScrobbler"
    private const val BASE_URL = "https://api.mdblist.com"
  }

  private val jsonMediaType = "application/json".toMediaType()
  private val apiJson = Json(json) { explicitNulls = false }

  private fun buildUrl(path: String): String {
    val apiKey = preferences.apiKey.get()
    return "$BASE_URL$path?apikey=$apiKey"
  }

  suspend fun scrobble(
    action: String,
    request: MdbListScrobbleRequest,
  ): Result<MdbListScrobbleResponse?> = withContext(Dispatchers.IO) {
    try {
      if (!preferences.isConfigured()) return@withContext Result.failure(IllegalStateException("Not configured"))

      val bodyJson = apiJson.encodeToString(MdbListScrobbleRequest.serializer(), request)
      Log.d(TAG, "Scrobble $action: $bodyJson")

      val httpRequest = Request.Builder()
        .url(buildUrl("/scrobble/$action"))
        .post(bodyJson.toRequestBody(jsonMediaType))
        .addHeader("Content-Type", "application/json")
        .build()

      client.newCall(httpRequest).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        when (response.code) {
          in 200..299 -> {
            if (responseBody.isNotBlank()) {
              val scrobbleResponse = apiJson.decodeFromString(MdbListScrobbleResponse.serializer(), responseBody)
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

  suspend fun fetchUsername(): Result<String> = withContext(Dispatchers.IO) {
    try {
      if (!preferences.isConfigured()) return@withContext Result.failure(IllegalStateException("Not configured"))

      val request = Request.Builder()
        .url(buildUrl("/user"))
        .addHeader("Content-Type", "application/json")
        .build()

      client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
          val userJson = apiJson.parseToJsonElement(responseBody)
          val username = userJson.toString().let {
            val match = Regex("\"username\"\\s*:\\s*\"([^\"]+)\"").find(it)
            match?.groupValues?.get(1) ?: "Unknown"
          }
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
}
