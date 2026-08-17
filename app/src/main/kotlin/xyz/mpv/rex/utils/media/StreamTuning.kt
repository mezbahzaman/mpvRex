package xyz.mpv.rex.utils.media

import `is`.xyz.mpv.MPVLib
import java.net.URI

/**
 * Applies network timeouts for network and P2P (torrent) streams, e.g. when used
 * as an external player for Stremio.
 *
 * Must be called right before `loadfile` so the timeout is picked up by the
 * upcoming demuxer.
 */
object StreamTuning {
  private val NETWORK_SCHEMES =
    setOf("http", "https", "rtmp", "rtmps", "rtsp", "rtsps", "mms", "mmsh", "ftp", "ftps")

  /** Hosts used by Stremio's WebTorrent server and in-app SMB/WebDAV/FTP proxies. */
  private val LOCAL_PROXY_HOSTS = setOf("127.0.0.1", "localhost", "0.0.0.0")

  fun isNetworkUri(uri: String): Boolean {
    val scheme = runCatching { URI(uri).scheme }.getOrNull()?.lowercase()
    return scheme in NETWORK_SCHEMES
  }

  fun isStremioTorrentUri(uri: String): Boolean {
    if (!isNetworkUri(uri)) return false
    val parsed = runCatching { URI(uri) }.getOrNull() ?: return false
    val host = parsed.host?.lowercase()
    if (host !in LOCAL_PROXY_HOSTS) return false
    val firstPathSegment = parsed.path?.trimStart('/')?.substringBefore('/')
    return parsed.port == 11470 || firstPathSegment?.matches(Regex("[0-9a-fA-F]{40}")) == true
  }

  fun applyTuningForUri(uri: String?) {
    if (uri.isNullOrBlank()) return
    when {
      isStremioTorrentUri(uri) -> {
        // Torrent peers can stall substantially longer than ordinary HTTP sources.
        MPVLib.setOptionString("network-timeout", "45")
      }
      isNetworkUri(uri) -> {
        MPVLib.setOptionString("network-timeout", "30")
      }
      else -> {
        MPVLib.setOptionString("network-timeout", "30")
      }
    }
  }
}
