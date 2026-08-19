package xyz.mpv.rex.trakt

import xyz.mpv.rex.preferences.preference.PreferenceStore

class MdbListPreferences(
  preferenceStore: PreferenceStore,
) {
  val apiKey = preferenceStore.getString("mdblist_api_key", "")
  val enabled = preferenceStore.getBoolean("mdblist_scrobbling_enabled", false)
  val username = preferenceStore.getString("mdblist_username", "")

  fun isConfigured(): Boolean {
    return enabled.get() && apiKey.get().isNotBlank()
  }
}
