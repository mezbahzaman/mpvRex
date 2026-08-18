package xyz.mpv.rex.ui.player

import android.content.Intent
import android.os.Bundle
import kotlin.math.roundToLong

/** Normalized data exchanged by the Stremio external-player handoff. */
internal data class StremioPlaybackResult(
  val positionMs: Long?,
  val durationMs: Long?,
)

internal object StremioHandoff {
  const val RESULT_ACTION = "xyz.mpv.rex.ui.player.PlayerActivity.result"
  const val POSITION_EXTRA = "position"
  const val DURATION_EXTRA = "duration"
  const val RETURN_RESULT_EXTRA = "return_result"

  fun requestedResult(intent: Intent): Boolean =
    intent.getBooleanExtra(RETURN_RESULT_EXTRA, false)

  /**
   * External-player integrations have historically used several numeric Bundle types.
   * The contract is milliseconds; malformed, negative, and non-finite values are ignored.
   */
  fun positionMs(extras: Bundle?): Long? = extras?.let {
    if (it.containsKey(POSITION_EXTRA)) positionMs(it.get(POSITION_EXTRA)) else null
  }

  fun positionMs(value: Any?): Long? = when (value) {
    is Byte, is Short, is Int, is Long -> (value as Number).toLong()
    is Float, is Double -> (value as Number).toDouble().takeIf { it.isFinite() }?.toLong()
    is String -> value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
    else -> null
  }?.takeIf { it >= 0 }

  fun millisecondsFromSeconds(seconds: Double?): Long? = seconds
    ?.takeIf { it.isFinite() && it >= 0.0 && it <= Long.MAX_VALUE / 1000.0 }
    ?.times(1000.0)
    ?.roundToLong()

  fun resultIntent(result: StremioPlaybackResult): Intent = Intent(RESULT_ACTION).apply {
    result.positionMs?.let { putExtra(POSITION_EXTRA, it) }
    result.durationMs?.let { putExtra(DURATION_EXTRA, it) }
  }

  fun normalizeResult(positionMs: Long?, durationMs: Long?): StremioPlaybackResult {
    val duration = durationMs?.takeIf { it > 0 }
    val position = positionMs?.takeIf { it >= 0 }?.let { value ->
      duration?.let { value.coerceIn(0, it) } ?: value
    }
    return StremioPlaybackResult(position, duration)
  }
}
