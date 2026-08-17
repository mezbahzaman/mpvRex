package xyz.mpv.rex.utils.media

import android.net.Uri
import `is`.xyz.mpv.MPVLib

/**
 * Applies mpv options that improve playback smoothness for network and P2P (torrent)
 * streams, e.g. when used as an external player for Stremio.
 *
 * Must be called right before `loadfile` so the options are picked up by the
 * upcoming demuxer. Options are reset to safe defaults for local files.
 */
object StreamTuning {

  private val NETWORK_SCHEMES =
    setOf("http", "https", "rtmp", "rtmps", "rtsp", "rtsps", "mms", "mmsh", "ftp", "ftps")

  /** Hosts used by Stremio's WebTorrent server and in-app SMB/WebDAV/FTP proxies. */
  private val LOCAL_PROXY_HOSTS = setOf("127.0.0.1", "localhost", "0.0.0.0")

  fun isNetworkUri(uri: String): Boolean {
    val scheme = runCatching { Uri.parse(uri).scheme }.getOrNull()?.lowercase()
    return scheme in NETWORK_SCHEMES
  }

  fun isStremioTorrentUri(uri: String): Boolean {
    if (!isNetworkUri(uri)) return false
    val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
    val host = parsed.host?.lowercase()
    if (host !in LOCAL_PROXY_HOSTS) return false
    return parsed.port == 11470 || parsed.pathSegments.firstOrNull()?.matches(Regex("[0-9a-fA-F]{40}")) == true
  }

  fun applyTuningForUri(
    uri: String?,
    maximumDownloadMiB: Int = 200,
    maximumBufferedSeconds: Int = 180,
  ) {
    if (uri.isNullOrBlank()) return
    val downloadMiB = maximumDownloadMiB.coerceIn(1, 4096)
    val bufferedSeconds = maximumBufferedSeconds.coerceIn(1, 3600)
    when {
      isStremioTorrentUri(uri) -> {
        // Torrent (Stremio WebTorrent) server: peers are slow, so read far ahead
        // and tolerate long stalls so playback doesn't freeze.
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("network-timeout", "45")
        MPVLib.setOptionString("demuxer-max-bytes", "${downloadMiB}MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", bufferedSeconds.toString())
        MPVLib.setOptionString("cache-secs", bufferedSeconds.toString())
      }
      isNetworkUri(uri) -> {
        // Regular HTTP(S)/HLS/RTSP streams: modestly larger read-ahead cushion.
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("network-timeout", "30")
        MPVLib.setOptionString("demuxer-max-bytes", "${downloadMiB}MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", bufferedSeconds.toString())
        MPVLib.setOptionString("cache-secs", bufferedSeconds.toString())
      }
      else -> {
        // Local files: reset to mpv defaults so cached tuning doesn't linger.
        MPVLib.setOptionString("cache", "no")
        MPVLib.setOptionString("network-timeout", "30")
        MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", "10")
        MPVLib.setOptionString("cache-secs", "10")
      }
    }
  }
}
