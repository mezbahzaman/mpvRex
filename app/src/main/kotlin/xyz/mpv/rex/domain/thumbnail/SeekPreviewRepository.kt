package xyz.mpv.rex.domain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SeekPreviewRepository(
  private val context: Context,
) {
  private val cacheDir = File(context.cacheDir, "seek-previews").apply { mkdirs() }
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val memoryCache =
    object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
  private val _cacheRevision = MutableStateFlow(0)
  val cacheRevision: StateFlow<Int> = _cacheRevision.asStateFlow()
  private val decoderMutex = Mutex()

  @Volatile private var activePath: String? = null
  @Volatile private var activeKey: String? = null
  private var generationJob: Job? = null

  @Synchronized
  fun prepare(
    path: String,
    source: String = path,
    headers: Map<String, String> = emptyMap(),
    durationHintSeconds: Float? = null,
  ) {
    if (path.isBlank()) return
    val key = cacheKey(path, source, headers)
    if (activeKey == key && generationJob?.isActive == true) return

    generationJob?.cancel()
    activePath = path
    activeKey = key
    synchronized(memoryCache) { memoryCache.evictAll() }

    val mediaDir = File(cacheDir, key)
    val completed = completeFile(mediaDir)
    val isNetworkSource = Uri.parse(source).scheme?.lowercase() in setOf("http", "https")
    val isFresh = !isNetworkSource || System.currentTimeMillis() - completed.lastModified() < NETWORK_CACHE_MAX_AGE_MS
    if (completed.exists() && isFresh) {
      _cacheRevision.value++
      return
    }
    if (completed.exists()) mediaDir.deleteRecursively()

    generationJob = scope.launch {
      decoderMutex.withLock {
        currentCoroutineContext().ensureActive()
        if (activeKey == key) generate(source, headers, durationHintSeconds, key, mediaDir)
      }
    }
  }

  @Synchronized
  fun cancel() {
    generationJob?.cancel()
    generationJob = null
    activePath = null
    activeKey = null
    synchronized(memoryCache) { memoryCache.evictAll() }
  }

  suspend fun frameAt(
    path: String,
    durationSeconds: Float,
    positionSeconds: Float,
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (path.isBlank() || durationSeconds <= 0f) return@withContext null
    val key = activeKey.takeIf { activePath == path } ?: return@withContext null
    val mediaDir = File(cacheDir, key)
    val count = countFile(mediaDir).readTextOrNull()?.toIntOrNull() ?: return@withContext null

    val preferred = nearestFrameIndex(positionSeconds, durationSeconds, count)
    val index = nearestAvailableIndex(mediaDir, preferred, count) ?: return@withContext null
    val memoryKey = "$key:$index"
    synchronized(memoryCache) { memoryCache.get(memoryKey) }?.let { return@withContext it }

    BitmapFactory.decodeFile(frameFile(mediaDir, index).absolutePath)?.also {
      synchronized(memoryCache) { memoryCache.put(memoryKey, it) }
    }
  }

  private suspend fun generate(
    path: String,
    headers: Map<String, String>,
    durationHintSeconds: Float?,
    key: String,
    mediaDir: File,
  ) {
    val retriever = MediaMetadataRetriever()
    try {
      setDataSource(retriever, path, headers)
      val metadataDuration =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
          ?.toLongOrNull()
          ?.takeIf { it > 0L }
          ?.div(1000f)
      val duration = metadataDuration ?: durationHintSeconds?.takeIf { it.isFinite() && it > 0f } ?: return
      val count = frameCount(duration)
      val positions = samplePositions(duration, count)
      mediaDir.mkdirs()
      countFile(mediaDir).writeText(count.toString())

      for (index in progressiveOrder(count)) {
        currentCoroutineContext().ensureActive()
        if (activeKey != key) return
        val output = frameFile(mediaDir, index)
        if (output.exists()) continue
        val positionUs = (positions[index] * 1_000_000L).toLong()
        val bitmap =
          runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
              retriever.getScaledFrameAtTime(
                positionUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
              )
            } else {
              retriever.getFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.scaleToThumbnailMax(PREVIEW_WIDTH)
            }
          }.getOrNull() ?: continue
        currentCoroutineContext().ensureActive()
        if (activeKey != key) return
        try {
          val bytes = ByteArrayOutputStream().use { encoded ->
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, encoded)) encoded.toByteArray() else null
          }
          if (bytes != null && bytes.size <= MAX_FRAME_BYTES) output.writeBytes(bytes)
        } finally {
          bitmap.recycle()
        }
        if (output.exists()) _cacheRevision.value++
      }

      if (activeKey == key && (0 until count).all { frameFile(mediaDir, it).exists() }) {
        completeFile(mediaDir).writeText("")
      }
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      // A preview failure must never affect primary playback.
    } finally {
      runCatching { retriever.release() }
      if (activeKey == key) pruneOldCaches(key)
    }
  }

  private fun setDataSource(
    retriever: MediaMetadataRetriever,
    path: String,
    headers: Map<String, String>,
  ) {
    val uri = Uri.parse(path)
    when (uri.scheme?.lowercase()) {
      "content", "android.resource" -> retriever.setDataSource(context, uri)
      "file" -> retriever.setDataSource(uri.path ?: path)
      "http", "https" -> retriever.setDataSource(path, headers)
      else -> retriever.setDataSource(path)
    }
  }

  private fun nearestAvailableIndex(directory: File, preferred: Int, count: Int): Int? {
    if (frameFile(directory, preferred).exists()) return preferred
    for (distance in 1 until count) {
      val before = preferred - distance
      if (before >= 0 && frameFile(directory, before).exists()) return before
      val after = preferred + distance
      if (after < count && frameFile(directory, after).exists()) return after
    }
    return null
  }

  private fun pruneOldCaches(currentKey: String) {
    cacheDir.listFiles()
      ?.filter { it.isDirectory && it.name != currentKey }
      ?.sortedByDescending { it.lastModified() }
      ?.drop(MAX_RETAINED_CACHES - 1)
      ?.forEach { it.deleteRecursively() }
  }

  private fun cacheKey(path: String, source: String, headers: Map<String, String>): String {
    val sourceUri = Uri.parse(source)
    val localFile = when (sourceUri.scheme?.lowercase()) {
      null -> File(source)
      "file" -> sourceUri.path?.let(::File)
      else -> null
    }
    val localIdentity = localFile?.takeIf { it.exists() }?.let { "${it.length()}|${it.lastModified()}" }.orEmpty()
    val headerIdentity = headers.entries
      .sortedBy { it.key.lowercase() }
      .joinToString("|") { "${it.key.lowercase()}:${it.value}" }
    val identity = "$path|$source|$localIdentity|$headerIdentity|$CACHE_VERSION"
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
    return digest.take(16).joinToString("") { "%02x".format(it) }
  }

  private fun frameFile(directory: File, index: Int) = File(directory, "%03d.jpg".format(index))

  private fun countFile(directory: File) = File(directory, COUNT_FILE)

  private fun completeFile(directory: File) = File(directory, COMPLETE_FILE)

  companion object {
    private const val CACHE_VERSION = 3
    private const val COUNT_FILE = ".count"
    private const val COMPLETE_FILE = ".complete"
    private const val PREVIEW_WIDTH = 160
    private const val PREVIEW_HEIGHT = 90
    private const val JPEG_QUALITY = 25
    private const val MAX_FRAME_BYTES = 48 * 1024
    private const val MEMORY_CACHE_BYTES = 2 * 1024 * 1024
    private const val MAX_RETAINED_CACHES = 3
    private const val NETWORK_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private const val MIN_FRAME_COUNT = 12
    private const val MAX_FRAME_COUNT = 32

    internal fun frameCount(durationSeconds: Float): Int =
      ceil(durationSeconds / 300f).toInt().coerceIn(MIN_FRAME_COUNT, MAX_FRAME_COUNT)

    internal fun samplePositions(durationSeconds: Float, count: Int): List<Float> {
      if (count <= 1) return listOf(0f)
      val lastPosition = (durationSeconds - 0.25f).coerceAtLeast(0f)
      return List(count) { index -> lastPosition * index / (count - 1) }
    }

    internal fun nearestFrameIndex(positionSeconds: Float, durationSeconds: Float, count: Int): Int {
      if (durationSeconds <= 0f || count <= 1) return 0
      return ((positionSeconds / durationSeconds).coerceIn(0f, 1f) * (count - 1)).roundToInt()
    }

    internal fun progressiveOrder(count: Int): List<Int> {
      if (count <= 0) return emptyList()
      val result = mutableListOf<Int>()
      val queue = ArrayDeque<IntRange>()
      queue.add(0 until count)
      while (queue.isNotEmpty()) {
        val range = queue.removeFirst()
        if (range.isEmpty()) continue
        val middle = (range.first + range.last) / 2
        result += middle
        if (range.first < middle) queue.add(range.first until middle)
        if (middle < range.last) queue.add((middle + 1)..range.last)
      }
      return result
    }
  }
}

private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()
