package xyz.mpv.rex.utils.media

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live stats for network streams (Stremio HTTP / P2P torrents).
 *
 * For torrent streams, Stremio's streaming server (local WebTorrent HTTP engine)
 * exposes a real-time JSON endpoint: `{origin}/{infoHash}/stats.json`.
 * Polled once per second; combined with mpv's own cache properties.
 */
data class StreamStats(
  val isNetwork: Boolean = false,
  val isTorrent: Boolean = false,
  val fetchingMetadata: Boolean = false,
  val hasTorrentStats: Boolean = false,
  val bufferedSeconds: Float = 0f,
  val seeds: Int = 0,
  val peers: Int = 0,
  val swarmSeeds: Int? = null,
  val speedBytesPerSec: Long = 0,
)

data class TorrentStats(
  val downloadSpeed: Long,
  val seeds: Int,
  val peers: Int,
  /** True when the engine reported active wire connections this poll (real seeder data). */
  val hasWireData: Boolean,
  /** HTTP(S) tracker announce URLs advertised by the engine's `sources[]`. */
  val trackerUrls: List<String>,
)

object StreamStatsFetcher {

  private const val STATS_TIMEOUT_MS = 1500
  private val INFO_HASH_REGEX = Regex("[0-9a-fA-F]{40}")

  /**
   * Detects a Stremio torrent URL. The infoHash is the first path segment
   * (e.g. `{origin}/{infoHash}/{filename}`), or an `infoHash=` query parameter.
   */
  fun parseInfoHash(url: String): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    uri.pathSegments.getOrNull(0)?.takeIf { INFO_HASH_REGEX.matches(it) }?.let { return it.lowercase() }
    return uri.getQueryParameter("infoHash")?.takeIf { INFO_HASH_REGEX.matches(it) }?.lowercase()
  }

  /** Stats endpoint for a torrent: `{scheme}://{host}:{port}/{infoHash}/stats.json`. */
  fun buildStatsUrl(streamUrl: String, infoHash: String): String? {
    val base = runCatching {
      val uri = Uri.parse(streamUrl)
      val scheme = uri.scheme ?: return null
      val host = uri.host ?: return null
      "$scheme://$host${if (uri.port >= 0) ":${uri.port}" else ""}"
    }.getOrNull() ?: return null
    return "$base/$infoHash/stats.json"
  }

  fun fetchTorrentStats(statsUrl: String): TorrentStats? {
    var connection: HttpURLConnection? = null
    return try {
      connection = URL(statsUrl).openConnection() as HttpURLConnection
      connection.connectTimeout = STATS_TIMEOUT_MS
      connection.readTimeout = STATS_TIMEOUT_MS
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/json")
      if (connection.responseCode !in 200..299) {
        null
      } else {
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val filesReady = (json.optJSONArray("files")?.length() ?: 0) > 0
        if (!filesReady) {
          // Torrent metadata not resolved yet (engine knows the hash but not the files)
          null
        } else {
          val wires = json.optJSONArray("wires")
          val hasWireData = (wires?.length() ?: 0) > 0
          TorrentStats(
            downloadSpeed = (json.optDouble("downloadSpeed", 0.0)).toLong(),
            // `swarmSize` is a constant tracker total (not seeders). Only
            // actually-connected peers expose a seeder flag, so count those.
            seeds = if (hasWireData) countSeeders(json) else 0,
            peers = json.optInt("peers", 0),
            hasWireData = hasWireData,
            trackerUrls = httpTrackerUrls(json),
          )
        }
      }
    } catch (_: Exception) {
      null
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * Queries the torrent's HTTP(S) trackers (announce) and returns the maximum
   * tracker-reported seeder count (`complete`). Returns null when every tracker
   * fails or none are HTTP(S). Trackers are the only reliable source of the
   * global swarm seeder count; WebTorrent's own engine does not expose one.
   */
  fun fetchSwarmSeeds(infoHash: String, trackerUrls: List<String>): Int? {
    val rawHash = hexToBytes(infoHash) ?: return null
    val infoHashParam = percentEncode(rawHash)
    val peerId = percentEncode("-MP0001-0123456789ab".toByteArray(Charsets.ISO_8859_1))
    val query =
      "info_hash=$infoHashParam&peer_id=$peerId&port=6881" +
        "&uploaded=0&downloaded=0&left=0&compact=1&numwant=0&event=started"

    var maxSeeds: Int? = null
    for (tracker in trackerUrls) {
      var connection: HttpURLConnection? = null
      try {
        val announceUrl = "$tracker?$query"
        connection = URL(announceUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "mpvRex/4.5")
        if (connection.responseCode in 200..299) {
          val body = connection.inputStream.readBytes().toString(Charsets.ISO_8859_1)
          bencodeInt(body, "complete")?.let { complete ->
            maxSeeds = maxOf(maxSeeds ?: 0, complete)
          }
        }
      } catch (_: Exception) {
        // Individual trackers often fail; keep trying the rest.
      } finally {
        connection?.disconnect()
      }
    }
    return maxSeeds
  }

  /** Reads a bencoded integer value: `{key-len}:{key}i{value}e`. */
  private fun bencodeInt(body: String, key: String): Int? {
    val marker = "${key.length}:$key"
    val idx = body.indexOf(marker)
    if (idx < 0) return null
    val valueStart = idx + marker.length
    if (valueStart >= body.length || body[valueStart] != 'i') return null
    val valueEnd = body.indexOf('e', valueStart)
    if (valueEnd < 0) return null
    return body.substring(valueStart + 1, valueEnd).toIntOrNull()
  }

  /** HTTP(S) tracker announce URLs from the engine's `sources[]`. */
  private fun httpTrackerUrls(json: JSONObject): List<String> {
    val sources = json.optJSONArray("sources") ?: return emptyList()
    val urls = mutableListOf<String>()
    for (i in 0 until sources.length()) {
      val url = sources.optJSONObject(i)?.optString("url") ?: continue
      if (url.startsWith("tracker:http://") || url.startsWith("tracker:https://")) {
        urls.add(url.removePrefix("tracker:"))
      }
    }
    return urls
  }

  private fun countSeeders(json: JSONObject): Int {
    val wires = json.optJSONArray("wires") ?: return 0
    var seeds = 0
    for (i in 0 until wires.length()) {
      if (wires.optJSONObject(i)?.optBoolean("isSeeder", false) == true) seeds++
    }
    return seeds
  }

  private fun hexToBytes(hex: String): ByteArray? {
    if (hex.length != 40) return null
    return try {
      ByteArray(20) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    } catch (_: NumberFormatException) {
      null
    }
  }

  private fun percentEncode(bytes: ByteArray): String {
    val hex = "0123456789abcdef"
    val sb = StringBuilder(bytes.size * 3)
    for (b in bytes) {
      sb.append('%').append(hex[(b.toInt() ushr 4) and 0xF]).append(hex[b.toInt() and 0xF])
    }
    return sb.toString()
  }

  fun formatSpeed(bytesPerSec: Long): String =
    when {
      bytesPerSec >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSec / 1_000_000f)
      bytesPerSec >= 1_000 -> "${bytesPerSec / 1_000} KB/s"
      bytesPerSec > 0 -> "$bytesPerSec B/s"
      else -> "0 KB/s"
    }
}
