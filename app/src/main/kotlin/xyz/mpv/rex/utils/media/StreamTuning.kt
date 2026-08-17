package xyz.mpv.rex.utils.media

import android.net.Uri
import `is`.xyz.mpv.MPVLib
import java.net.URI

/**
 * Applies mpv options that improve playback smoothness for network and P2P (torrent)
 * streams, e.g. when used as an external player for Stremio.
 *
 * Must be called right before `loadfile` so the options are picked up by the
 * upcoming demuxer. Options are reset to safe defaults for local files.
 */
object StreamTuning {

  internal data class CacheLimits(
    val maximumBytes: String,
    val readaheadSeconds: String,
  )

  private val NETWORK_SCHEMES =
    setOf("http", "https", "rtmp", "rtmps", "rtsp", "rtsps", "mms", "mmsh", "ftp", "ftps")

  /** Hosts used by Stremio's WebTorrent server and in-app SMB/WebDAV/FTP proxies. */
  private val LOCAL_PROXY_HOSTS = setOf("127.0.0.1", "localhost", "0.0.0.0")

  fun isNetworkUri(uri: String): Boolean {
    val scheme = runCatching { Uri.parse(uri).scheme }.getOrNull()?.lowercase()
    return scheme in NETWORK_SCHEMES
  }

  fun isAdaptiveManifestUri(uri: String): Boolean {
    val path = runCatching { URI(uri).path }.getOrNull()?.lowercase() ?: return false
    return path.endsWith(".m3u8") || path.endsWith(".mpd")
  }

  fun isStremioTorrentUri(uri: String): Boolean {
    if (!isNetworkUri(uri)) return false
    val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return false
    val host = parsed.host?.lowercase()
    if (host !in LOCAL_PROXY_HOSTS) return false
    return parsed.port == 11470 || parsed.pathSegments.firstOrNull()?.matches(Regex("[0-9a-fA-F]{40}")) == true
  }

  internal fun cacheLimits(maximumDownloadMiB: Int, maximumBufferedSeconds: Int): CacheLimits {
    val downloadMiB = maximumDownloadMiB.coerceIn(0, 4096)
    val bufferedSeconds = maximumBufferedSeconds.coerceIn(0, 3600)
    return CacheLimits(
      maximumBytes = if (downloadMiB == 0) UNBOUNDED_BYTES else "${downloadMiB}MiB",
      readaheadSeconds = if (bufferedSeconds == 0) UNBOUNDED_SECONDS else bufferedSeconds.toString(),
    )
  }

  fun applyTuningForUri(
    uri: String?,
    maximumDownloadMiB: Int = 200,
    maximumBufferedSeconds: Int = 180,
  ) {
    if (uri.isNullOrBlank()) return
    val cacheLimits = cacheLimits(maximumDownloadMiB, maximumBufferedSeconds)
    when {
      isStremioTorrentUri(uri) -> {
        // Torrent (Stremio WebTorrent) server: peers are slow, so read far ahead
        // and tolerate long stalls so playback doesn't freeze.
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("network-timeout", "45")
        MPVLib.setOptionString("demuxer-max-bytes", cacheLimits.maximumBytes)
        MPVLib.setOptionString("demuxer-readahead-secs", cacheLimits.readaheadSeconds)
        MPVLib.setOptionString("cache-secs", cacheLimits.readaheadSeconds)
      }
      isNetworkUri(uri) -> {
        // Regular HTTP(S)/HLS/RTSP streams: modestly larger read-ahead cushion.
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("network-timeout", "30")
        MPVLib.setOptionString("demuxer-max-bytes", cacheLimits.maximumBytes)
        MPVLib.setOptionString("demuxer-readahead-secs", cacheLimits.readaheadSeconds)
        MPVLib.setOptionString("cache-secs", cacheLimits.readaheadSeconds)
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

  private const val UNBOUNDED_BYTES = "4611686018427387903"
  private const val UNBOUNDED_SECONDS = "1.7976931348623157e308"
}
