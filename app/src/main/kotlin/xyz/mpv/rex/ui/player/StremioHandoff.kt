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

  /** Preserve the original external-player result contract: Int milliseconds. */
  fun result(positionSeconds: Int?, durationSeconds: Int?) = ExternalPlayerResult(
    positionMs = positionSeconds?.times(1000),
    durationMs = durationSeconds?.times(1000),
  )

  fun resultIntent(positionSeconds: Int?, durationSeconds: Int?): Intent = Intent(RESULT_ACTION).apply {
    val result = result(positionSeconds, durationSeconds)
    result.positionMs?.let { putExtra(POSITION_EXTRA, it) }
    result.durationMs?.let { putExtra(DURATION_EXTRA, it) }
  }
}
