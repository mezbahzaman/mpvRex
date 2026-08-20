package xyz.mpv.rex.ui.player

import android.content.Intent
import android.os.Bundle

internal data class ExternalPlayerResult(
  val positionMs: Int?,
  val durationMs: Int?,
)
internal object StremioHandoff {
  const val RESULT_ACTION = "xyz.mpv.rex.ui.player.PlayerActivity.result"
  const val POSITION_EXTRA = "position"
  const val START_FROM_EXTRA = "startfrom"
  const val DURATION_EXTRA = "duration"
  const val RETURN_RESULT_EXTRA = "return_result"

  fun requestedResult(intent: Intent): Boolean =
    intent.getBooleanExtra(RETURN_RESULT_EXTRA, false)

  /**
   * External-player integrations have historically used several numeric Bundle types.
   * The contract is milliseconds; malformed, negative, and non-finite values are ignored.
   */
  fun positionMs(extras: Bundle?): Long? = extras?.let {
    positionMs(it.get(POSITION_EXTRA), it.get(START_FROM_EXTRA))
  }

  fun positionMs(position: Any?, startFrom: Any?): Long? =
    positionMs(position) ?: positionMs(startFrom)

  fun positionMs(value: Any?): Long? = when (value) {
    is Byte, is Short, is Int, is Long -> (value as Number).toLong()
    is Float, is Double -> (value as Number).toDouble().takeIf { it.isFinite() }?.toLong()
    is String -> value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
    else -> null
  }?.takeIf { it >= 0 }

  /** Preserve Stremio's original Bundle type while accepting precise millisecond snapshots. */
  fun result(positionMs: Long?, durationMs: Long?) = ExternalPlayerResult(
    positionMs = compatibleMilliseconds(positionMs),
    durationMs = compatibleMilliseconds(durationMs),
  )

  fun resultIntent(positionMs: Long?, durationMs: Long?): Intent = Intent(RESULT_ACTION).apply {
    val result = result(positionMs, durationMs)
    result.positionMs?.let { putExtra(POSITION_EXTRA, it) }
    result.durationMs?.let { putExtra(DURATION_EXTRA, it) }
  }

  internal fun secondsToMilliseconds(value: Double?): Long? =
    value?.takeIf { it.isFinite() && it >= 0.0 }?.times(1000.0)?.toLong()

  private fun compatibleMilliseconds(value: Long?): Int? =
    value?.takeIf { it >= 0L }?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
}
