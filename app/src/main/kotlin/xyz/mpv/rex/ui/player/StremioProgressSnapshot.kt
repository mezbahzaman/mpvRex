package xyz.mpv.rex.ui.player

import android.content.Context

/** Durable last-known playback values used when the player process dies unexpectedly. */
internal object StremioProgressSnapshot {
  private const val PREFS = "stremio_progress_snapshot"
  private const val POSITION_MS = "position_ms"
  private const val DURATION_MS = "duration_ms"
  private const val RETURN_RESULT = "return_result"

  fun save(context: Context, positionMs: Long?, durationMs: Long?, returnResult: Boolean) {
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
