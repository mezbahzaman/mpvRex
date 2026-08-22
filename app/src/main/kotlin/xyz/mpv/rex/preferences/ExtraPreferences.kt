package xyz.mpv.rex.preferences

import xyz.mpv.rex.preferences.preference.PreferenceStore

class ExtraPreferences(
  preferenceStore: PreferenceStore,
) {
  val autoStremioSubtitles = preferenceStore.getBoolean("sub_auto_stremio", true)
  val maximumBufferedSeconds = preferenceStore.getInt("maximum_buffered_seconds", 180)
  val maximumNetworkDownloadMiB = preferenceStore.getInt("maximum_network_download_mib", 200)
  val pingHost = preferenceStore.getString("ping_host", "google.com")
  val autoLocalSubtitles = preferenceStore.getBoolean("auto_local_subtitles", false)
  val localSubtitleExcludedFolders = preferenceStore.getStringSet("local_subtitle_excluded_folders", emptySet())
}
