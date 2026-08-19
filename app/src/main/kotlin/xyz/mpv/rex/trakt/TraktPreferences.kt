package xyz.mpv.rex.trakt

import xyz.mpv.rex.preferences.preference.PreferenceStore

class TraktPreferences(
  preferenceStore: PreferenceStore,
) {
  val clientId = preferenceStore.getString("trakt_client_id", "")
  val clientSecret = preferenceStore.getString("trakt_client_secret", "")
  val accessToken = preferenceStore.getString("trakt_access_token", "")
  val refreshToken = preferenceStore.getString("trakt_refresh_token", "")
  val tokenExpiresAt = preferenceStore.getLong("trakt_token_expires_at", 0L)
  val enabled = preferenceStore.getBoolean("trakt_scrobbling_enabled", false)
  val username = preferenceStore.getString("trakt_username", "")
  val oauthState = preferenceStore.getString("trakt_oauth_state", "")

  fun isTokenValid(): Boolean {
    val token = accessToken.get()
    val expiresAt = tokenExpiresAt.get()
    if (token.isBlank()) return false
    if (expiresAt <= 0L) return false
    return System.currentTimeMillis() < (expiresAt - 60_000L)
  }

  fun saveTokens(response: TraktTokenResponse) {
    accessToken.set(response.accessToken)
    refreshToken.set(response.refreshToken)
    tokenExpiresAt.set(response.createdAt * 1000L + response.expiresIn * 1000L)
  }

  fun clearTokens() {
    accessToken.set("")
    refreshToken.set("")
    tokenExpiresAt.set(0L)
    username.set("")
    oauthState.set("")
  }

  fun hasCredentials(): Boolean {
    return clientId.get().isNotBlank() && clientSecret.get().isNotBlank()
  }

  fun hasAuthentication(): Boolean {
    return accessToken.get().isNotBlank() || refreshToken.get().isNotBlank()
  }
}
