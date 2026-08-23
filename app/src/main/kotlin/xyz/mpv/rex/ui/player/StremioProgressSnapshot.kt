package xyz.mpv.rex.ui.player

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Durable last-known playback values used when the player process dies unexpectedly. */
internal object StremioProgressSnapshot {
  private const val PREFS = "stremio_progress_snapshot"
  private const val POSITION_MS = "position_ms"
  private const val DURATION_MS = "duration_ms"
  private const val RETURN_RESULT = "return_result"

  // Single writer keeps snapshots ordered (a newer position can never be overwritten
  // by an older queued one) and keeps the blocking commit() off the UI/observer threads.
  private val writeExecutor =
    Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "stremio-snapshot-writer").apply { isDaemon = true }
    }

  /** Periodic save during playback; asynchronous and ordered. */
  fun save(context: Context, positionMs: Long?, durationMs: Long?, returnResult: Boolean) {
    val app = context.applicationContext
    writeExecutor.execute { writeNow(app, positionMs, durationMs, returnResult) }
  }

  /**
   * Final save on quit/result paths. Queued through the same writer to preserve order,
   * then waited on briefly so the snapshot is durable before the process may go away.
   */
  fun saveAwaitingWrite(context: Context, positionMs: Long?, durationMs: Long?, returnResult: Boolean) {
    val app = context.applicationContext
    runCatching {
      writeExecutor
        .submit { writeNow(app, positionMs, durationMs, returnResult) }
        .get(500, TimeUnit.MILLISECONDS)
    }
  }

  private fun writeNow(context: Context, positionMs: Long?, durationMs: Long?, returnResult: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .apply {
        positionMs?.let { putLong(POSITION_MS, it) } ?: remove(POSITION_MS)
        durationMs?.let { putLong(DURATION_MS, it) } ?: remove(DURATION_MS)
        putBoolean(RETURN_RESULT, returnResult)
      }
      .commit()
  }

  fun consume(context: Context): Triple<Long?, Long?, Boolean> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val result = Triple(
      prefs.getLong(POSITION_MS, -1L).takeIf { it >= 0L },
      prefs.getLong(DURATION_MS, -1L).takeIf { it >= 0L },
      prefs.getBoolean(RETURN_RESULT, false),
    )
    prefs.edit().clear().commit()
    return result
  }

  fun clear(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
  }
}
