package xyz.mpv.rex.utils.media

import android.app.ActivityManager
import android.content.Context
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

  /** Bounded floors so a zero setting can never produce an unbounded mpv cache. */
  internal const val MIN_DOWNLOAD_MIB = 8
  internal const val MAX_DOWNLOAD_MIB = 4096
  internal const val MIN_BUFFERED_SECONDS = 10
  internal const val MAX_BUFFERED_SECONDS = 3600

  /**
   * Default download budget on devices with ≤4 GB total RAM when the user never
   * changed it. 160 MiB keeps meaningful readahead (~2-3 min at typical bitrates,
   * so the overlay's buffered-seconds figure behaves like it did before the cap
   * existed) while still sitting well below the old stock 200 MiB that was
   * starving background processes on low-memory devices.
   */
  private const val LOW_RAM_DEFAULT_DOWNLOAD_MIB = 160
  private const val LOW_RAM_TOTAL_MEM_BYTES = 4600L * 1024L * 1024L

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

  /**
   * Effective download budget: honors an explicit user value, otherwise picks a
   * smaller, RAM-aware default on low-memory devices.
   */
  fun resolveNetworkDownloadMiB(
    context: Context,
    configuredMiB: Int,
    userConfigured: Boolean,
  ): Int {
    if (userConfigured || configuredMiB != DEFAULT_NETWORK_DOWNLOAD_MIB) return configuredMiB
    return if (isLowRamDevice(context)) LOW_RAM_DEFAULT_DOWNLOAD_MIB else configuredMiB
  }

  private fun isLowRamDevice(context: Context): Boolean =
    runCatching {
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return@runCatching false
      if (activityManager.isLowRamDevice) return@runCatching true
      val info = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(info)
      info.totalMem in 1 until LOW_RAM_TOTAL_MEM_BYTES
    }.getOrDefault(false)

  internal fun cacheLimits(maximumDownloadMiB: Int, maximumBufferedSeconds: Int): CacheLimits {
    val downloadMiB = maximumDownloadMiB.coerceIn(MIN_DOWNLOAD_MIB, MAX_DOWNLOAD_MIB)
    val bufferedSeconds = maximumBufferedSeconds.coerceIn(MIN_BUFFERED_SECONDS, MAX_BUFFERED_SECONDS)
    return CacheLimits(
      maximumBytes = "${downloadMiB}MiB",
      readaheadSeconds = bufferedSeconds.toString(),
    )
  }

  fun applyTuningForUri(
    uri: String?,
    maximumDownloadMiB: Int = DEFAULT_NETWORK_DOWNLOAD_MIB,
    maximumBufferedSeconds: Int = DEFAULT_BUFFERED_SECONDS,
  ) {
    if (uri.isNullOrBlank()) return
    val cacheLimits = cacheLimits(maximumDownloadMiB, maximumBufferedSeconds)
    when {
      isStremioTorrentUri(uri) -> {
        // Torrent (Stremio WebTorrent) server: peers are slow, so read far ahead
        // and tolerate long stalls so playback doesn't freeze.
        MPVLib.setOptionString("cache", "yes")
        applyFasterNetworkOptions()
        MPVLib.setOptionString("network-timeout", "45")
        MPVLib.setOptionString("demuxer-max-bytes", cacheLimits.maximumBytes)
        MPVLib.setOptionString("demuxer-readahead-secs", cacheLimits.readaheadSeconds)
        MPVLib.setOptionString("cache-secs", cacheLimits.readaheadSeconds)
      }
      isNetworkUri(uri) -> {
        // Regular HTTP(S)/HLS/RTSP streams: modestly larger read-ahead cushion.
        MPVLib.setOptionString("cache", "yes")
        applyFasterNetworkOptions()
        MPVLib.setOptionString("network-timeout", "30")
        MPVLib.setOptionString("demuxer-max-bytes", cacheLimits.maximumBytes)
        MPVLib.setOptionString("demuxer-readahead-secs", cacheLimits.readaheadSeconds)
        MPVLib.setOptionString("cache-secs", cacheLimits.readaheadSeconds)
      }
      else -> {
        // Local files: reset to mpv defaults so cached tuning doesn't linger.
        MPVLib.setOptionString("cache", "no")
        resetFasterNetworkOptions()
        MPVLib.setOptionString("network-timeout", "30")
        MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", "10")
        MPVLib.setOptionString("cache-secs", "10")
      }
    }
  }

  /**
   * Low-risk connection/throughput helpers for network streams:
   * - A larger stream buffer feeds the demuxer with fewer syscalls per second.
   * - lavf reconnect flags recover silently from dropped HTTP connections
   *   instead of ending playback mid-stream.
   */
  private fun applyFasterNetworkOptions() {
    MPVLib.setOptionString("stream-buffer-size", "1MiB")
    MPVLib.setOptionString("stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=5")
  }

  private fun resetFasterNetworkOptions() {
    MPVLib.setOptionString("stream-buffer-size", "128KiB")
    MPVLib.setOptionString("stream-lavf-o", "")
  }

  internal const val DEFAULT_NETWORK_DOWNLOAD_MIB = 200
  internal const val DEFAULT_BUFFERED_SECONDS = 180
}
