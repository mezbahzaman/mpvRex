package xyz.mpv.rex.utils.media

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Live stats for network streams (Stremio HTTP / P2P torrents).
 *
 * For torrent streams, Stremio's streaming server (local WebTorrent HTTP engine)
 * exposes a real-time JSON endpoint: `{origin}/{infoHash}/stats.json`.
 * Combined with mpv's own cache properties by the player stats loop.
 */
data class StreamStats(
  val isNetwork: Boolean = false,
  val isTorrent: Boolean = false,
  val fetchingMetadata: Boolean = false,
  val bufferedSeconds: Float = 0f,
  val seeds: Int = 0,
  val peers: Int = 0,
  val swarmSeeds: Int? = null,
  val pingMs: Int? = null,
  val pingTarget: String = "",
  val protocol: String = "",
  val ip: String = "",
  val ipCountry: String = "",
  val ipCountryFlag: String = "",
  val speedBytesPerSec: Long = 0,
)

data class IpWhoisDetails(
  val ip: String,
  val country: String,
  val countryFlag: String,
)

data class TorrentStats(
  val downloadSpeed: Long,
  val seeds: Int,
  val peers: Int,
  /** True when the engine reported active wire connections this poll (real seeder data). */
  val hasWireData: Boolean,
  /** Tracker URLs advertised by the engine's `sources[]`. */
  val trackerUrls: List<String>,
)

object StreamStatsFetcher {

  private const val STATS_TIMEOUT_MS = 1500
  private val INFO_HASH_REGEX = Regex("[0-9a-fA-F]{40}")
  private val PING_TIME_REGEX = Regex("time[=<]([0-9]+(?:\\.[0-9]+)?)\\s*ms", RegexOption.IGNORE_CASE)

  /** Returns ICMP latency to the configured host, or null when the probe times out/fails. */
  fun fetchPingMs(host: String = "google.com"): Int? {
    val target = host.trim().takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) } ?: "google.com"
    var process: Process? = null
    return try {
      process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "1", target)
        .redirectErrorStream(true)
        .start()
      if (!process.waitFor(800, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly()
        return null
      }
      val output = process.inputStream.bufferedReader().use { it.readText() }
      parsePingMs(output)
    } catch (_: Exception) {
      null
    } finally {
      process?.destroy()
    }
  }

  /** Retrieves this device/network's public IP, country, and flag once per stream activation. */
  fun fetchIpWhoisDetails(): IpWhoisDetails? {
    var connection: HttpURLConnection? = null
    return try {
      connection = URL("https://ipwho.is/?fields=ip,success,country,flag.emoji").openConnection() as HttpURLConnection
      connection.connectTimeout = STATS_TIMEOUT_MS
      connection.readTimeout = STATS_TIMEOUT_MS
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/json")
      if (connection.responseCode !in 200..299) return null
      parseIpWhoisDetails(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }))
    } catch (_: Exception) {
      null
    } finally {
      connection?.disconnect()
    }
  }

  internal fun parseIpWhoisDetails(json: JSONObject): IpWhoisDetails? {
    if (!json.optBoolean("success", false)) return null
    val ip = json.optString("ip").trim()
    val country = json.optString("country").trim()
    val flag = json.optJSONObject("flag")?.optString("emoji").orEmpty().trim()
    if (ip.isBlank() || country.isBlank() || flag.isBlank()) return null
    return IpWhoisDetails(ip, country, flag)
  }

  internal fun formatStreamIdentity(stats: StreamStats): String = listOfNotNull(
    stats.protocol.takeIf { it.isNotBlank() },
    stats.ip.takeIf { it.isNotBlank() },
    stats.ipCountry.takeIf { it.isNotBlank() }?.let { country ->
      "$country${stats.ipCountryFlag.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()}"
    },
  ).joinToString(" • ")


  internal fun parsePingMs(output: String): Int? =
    PING_TIME_REGEX.find(output)?.groupValues?.getOrNull(1)
      ?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1)

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
       val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: return null
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
            downloadSpeed = json.optDouble("downloadSpeed", 0.0).takeIf { it.isFinite() }
              ?.toLong()?.coerceAtLeast(0L) ?: 0L,
            // `swarmSize` is a constant tracker total (not seeders). Only
            // actually-connected peers expose a seeder flag, so count those.
            seeds = if (hasWireData) countSeeders(json) else 0,
            peers = json.optInt("peers", 0).coerceAtLeast(0),
            hasWireData = hasWireData,
            trackerUrls = trackerUrls(json),
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
   * Queries the torrent's HTTP(S) and UDP trackers and returns the maximum
   * tracker-reported seeder count (`complete`). Returns null when every tracker
   * fails. Trackers are the only reliable source of the
   * global swarm seeder count; WebTorrent's own engine does not expose one.
   */
  suspend fun fetchSwarmSeeds(infoHash: String, trackerUrls: List<String>): Int? = coroutineScope {
    val rawHash = hexToBytes(infoHash) ?: return@coroutineScope null
    val infoHashParam = percentEncode(rawHash)
    val peerId = percentEncode("-MP0001-0123456789ab".toByteArray(Charsets.ISO_8859_1))
    val query =
      "info_hash=$infoHashParam&peer_id=$peerId&port=6881" +
        "&uploaded=0&downloaded=0&left=1&compact=1&numwant=0"

    trackerUrls.distinct().take(3).map { tracker ->
      async(Dispatchers.IO) {
        when {
          tracker.startsWith("udp://", ignoreCase = true) -> fetchUdpTrackerSeeds(rawHash, tracker)
          tracker.startsWith("http://", ignoreCase = true) || tracker.startsWith("https://", ignoreCase = true) ->
            fetchHttpTrackerSeeds(tracker, query)
          else -> null
        }
      }
    }.awaitAll().filterNotNull().maxOrNull()
  }

  private fun fetchHttpTrackerSeeds(tracker: String, query: String): Int? {
      var connection: HttpURLConnection? = null
      return try {
        val announceUrl = "$tracker${if (tracker.contains('?')) "&" else "?"}$query"
        connection = URL(announceUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "mpvRex/4.5")
        if (connection.responseCode in 200..299) {
          val body = connection.inputStream.readBytes().toString(Charsets.ISO_8859_1)
          bencodeInt(body, "complete")
        } else null
      } catch (_: Exception) {
        null
      } finally {
        connection?.disconnect()
      }
  }

  /** BEP 15 scrape: returns the tracker's complete/seeder count without announcing. */
  private fun fetchUdpTrackerSeeds(infoHash: ByteArray, tracker: String): Int? {
    val uri = runCatching { URI(tracker) }.getOrNull() ?: return null
    val host = uri.host ?: return null
    val port = uri.port.takeIf { it > 0 } ?: 80
    val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return null
    return runCatching {
      DatagramSocket().use { socket ->
        socket.soTimeout = 2500
        val connectTx = Random.nextInt()
        val connect = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
          .putLong(0x41727101980L).putInt(0).putInt(connectTx).array()
        socket.send(DatagramPacket(connect, connect.size, address, port))
        val connectResponse = ByteArray(16)
        val connectPacket = DatagramPacket(connectResponse, connectResponse.size)
        socket.receive(connectPacket)
        val connected = ByteBuffer.wrap(connectResponse, 0, connectPacket.length).order(ByteOrder.BIG_ENDIAN)
        if (connectPacket.length < 16 || connected.int != 0 || connected.int != connectTx) return@use null
        val connectionId = connected.long

        val scrapeTx = Random.nextInt()
        val scrape = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN)
          .putLong(connectionId).putInt(2).putInt(scrapeTx).put(infoHash).array()
        socket.send(DatagramPacket(scrape, scrape.size, address, port))
        val scrapeResponse = ByteArray(32)
        val scrapePacket = DatagramPacket(scrapeResponse, scrapeResponse.size)
        val scraped = runCatching {
          socket.receive(scrapePacket)
          val result = ByteBuffer.wrap(scrapeResponse, 0, scrapePacket.length).order(ByteOrder.BIG_ENDIAN)
          if (scrapePacket.length < 20 || result.int != 2 || result.int != scrapeTx) null
          else result.int.coerceAtLeast(0)
        }.getOrNull()
        scraped ?: fetchUdpAnnounceSeeds(socket, address, port, connectionId, infoHash)
      }
    }.getOrNull()
  }

  private fun fetchUdpAnnounceSeeds(
    socket: DatagramSocket,
    address: InetAddress,
    port: Int,
    connectionId: Long,
    infoHash: ByteArray,
  ): Int? {
    val transactionId = Random.nextInt()
    val peerId = "-MP0001-0123456789ab".toByteArray(Charsets.ISO_8859_1)
    val announce = ByteBuffer.allocate(98).order(ByteOrder.BIG_ENDIAN)
      .putLong(connectionId).putInt(1).putInt(transactionId)
      .put(infoHash).put(peerId)
      .putLong(0).putLong(1).putLong(0)
      .putInt(0).putInt(0).putInt(Random.nextInt()).putInt(-1).putShort(6881.toShort())
      .array()
    return runCatching {
      socket.send(DatagramPacket(announce, announce.size, address, port))
      val response = ByteArray(32)
      val packet = DatagramPacket(response, response.size)
      socket.receive(packet)
      val result = ByteBuffer.wrap(response, 0, packet.length).order(ByteOrder.BIG_ENDIAN)
      if (packet.length < 20 || result.int != 1 || result.int != transactionId) null
      else {
        result.int // interval
        result.int // leechers
        result.int.coerceAtLeast(0)
      }
    }.getOrNull()
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

  /** HTTP(S)/UDP tracker URLs from string or object entries in `sources[]`. */
  internal fun trackerUrls(json: JSONObject): List<String> {
    val sources = json.optJSONArray("sources") ?: return emptyList()
    val rawUrls = mutableListOf<String>()
    for (i in 0 until sources.length()) {
      val entry = sources.opt(i)
      rawUrls += when (entry) {
        is String -> entry
        is JSONObject -> entry.optString("url")
        else -> ""
      }
    }
    return normalizeTrackerUrls(rawUrls)
  }

  internal fun normalizeTrackerUrls(values: List<String>): List<String> =
    values.map { it.trim().removePrefix("tracker:").trim() }
      .filter {
        it.startsWith("http://", ignoreCase = true) ||
          it.startsWith("https://", ignoreCase = true) ||
          it.startsWith("udp://", ignoreCase = true)
      }
      .distinct()

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
      else -> "0 KB/s"
    }
}
