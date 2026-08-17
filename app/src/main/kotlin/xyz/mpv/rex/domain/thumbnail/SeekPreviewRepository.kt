package xyz.mpv.rex.domain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import `is`.xyz.mpv.FastThumbnails
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

class SeekPreviewRepository(
  context: Context,
) {
  private val cacheDir = File(context.cacheDir, "seek-previews").apply { mkdirs() }
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val memoryCache =
    object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
  private val _cacheRevision = MutableStateFlow(0)
  val cacheRevision: StateFlow<Int> = _cacheRevision.asStateFlow()

  @Volatile private var activeKey: String? = null
  private var generationJob: Job? = null

  @Synchronized
  fun prepare(path: String, durationSeconds: Float) {
    if (path.isBlank() || !durationSeconds.isFinite() || durationSeconds <= 0f) {
      generationJob?.cancel()
      generationJob = null
      activeKey = null
      return
    }

    val frameCount = frameCount(durationSeconds)
    val key = cacheKey(path, durationSeconds, frameCount)
    if (activeKey == key && (generationJob?.isActive == true || isComplete(key, frameCount))) return

    generationJob?.cancel()
    activeKey = key
    synchronized(memoryCache) { memoryCache.evictAll() }

    if (isComplete(key, frameCount)) {
      _cacheRevision.value++
      return
    }

    generationJob = scope.launch {
      val mediaDir = File(cacheDir, key).apply { mkdirs() }
      File(mediaDir, COMPLETE_FILE).delete()
      val positions = samplePositions(durationSeconds, frameCount)

      for (index in progressiveOrder(frameCount)) {
        if (!isActive || activeKey != key) return@launch
        val output = frameFile(mediaDir, index)
        if (output.exists()) {
          _cacheRevision.value++
          continue
        }

        val bitmap = runCatching {
          FastThumbnails.generateAsync(path, positions[index].toDouble(), PREVIEW_WIDTH, useHwDec = false)
        }.getOrNull() ?: continue

        try {
          val bytes = encodeWithinBudget(bitmap, MAX_CACHE_BYTES / frameCount)
          if (bytes != null) output.writeBytes(bytes)
        } finally {
          bitmap.recycle()
        }
        if (output.exists()) _cacheRevision.value++
      }

      if (activeKey == key && (0 until frameCount).all { frameFile(mediaDir, it).exists() }) {
        File(mediaDir, COMPLETE_FILE).writeText(frameCount.toString())
      }
      pruneOldCaches(key)
    }
  }

  suspend fun frameAt(path: String, durationSeconds: Float, positionSeconds: Float): Bitmap? =
    withContext(Dispatchers.IO) {
      if (path.isBlank() || durationSeconds <= 0f) return@withContext null
      val count = frameCount(durationSeconds)
      val key = cacheKey(path, durationSeconds, count)
      val mediaDir = File(cacheDir, key)
      if (!mediaDir.exists()) return@withContext null

      val preferred = nearestFrameIndex(positionSeconds, durationSeconds, count)
      val index = nearestAvailableIndex(mediaDir, preferred, count) ?: return@withContext null
      val memoryKey = "$key:$index"
      synchronized(memoryCache) { memoryCache.get(memoryKey) }?.let { return@withContext it }

      BitmapFactory.decodeFile(frameFile(mediaDir, index).absolutePath)?.also {
        synchronized(memoryCache) { memoryCache.put(memoryKey, it) }
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

  private fun encodeWithinBudget(source: Bitmap, maxBytes: Int): ByteArray? {
    var bitmap = source
    try {
      while (bitmap.width >= MIN_PREVIEW_WIDTH) {
        for (quality in intArrayOf(55, 45, 35, 25)) {
          val output = ByteArrayOutputStream(maxBytes)
          if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output) && output.size() <= maxBytes) {
            return output.toByteArray()
          }
        }
        val width = (bitmap.width * 0.8f).roundToInt()
        val height = (bitmap.height * 0.8f).roundToInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (bitmap !== source) bitmap.recycle()
        bitmap = scaled
      }
      return null
    } finally {
      if (bitmap !== source) bitmap.recycle()
    }
  }

  private fun isComplete(key: String, count: Int): Boolean =
    File(File(cacheDir, key), COMPLETE_FILE).readTextOrNull()?.toIntOrNull() == count

  private fun pruneOldCaches(currentKey: String) {
    cacheDir.listFiles()
      ?.filter { it.isDirectory && it.name != currentKey }
      ?.sortedByDescending { it.lastModified() }
      ?.drop(MAX_RETAINED_CACHES - 1)
      ?.forEach { it.deleteRecursively() }
  }

  private fun cacheKey(path: String, durationSeconds: Float, count: Int): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest("$path|${durationSeconds.roundToInt()}|$count|$CACHE_VERSION".toByteArray())
    return digest.take(16).joinToString("") { "%02x".format(it) }
  }

  private fun frameFile(directory: File, index: Int) = File(directory, "%03d.jpg".format(index))

  companion object {
    private const val CACHE_VERSION = 1
    private const val COMPLETE_FILE = ".complete"
    private const val PREVIEW_WIDTH = 320
    private const val MIN_PREVIEW_WIDTH = 128
    private const val MIN_FRAME_COUNT = 24
    private const val MAX_FRAME_COUNT = 64
    private const val MAX_CACHE_BYTES = 3 * 1024 * 1024
    private const val MEMORY_CACHE_BYTES = 6 * 1024 * 1024
    private const val MAX_RETAINED_CACHES = 3

    internal fun frameCount(durationSeconds: Float): Int =
      ceil(durationSeconds / 150f).toInt().coerceIn(MIN_FRAME_COUNT, MAX_FRAME_COUNT)

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
        if (middle !in result) result += middle
        if (range.first < middle) queue.add(range.first until middle)
        if (middle < range.last) queue.add((middle + 1)..range.last)
      }
      return result
    }
  }
}

private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()
