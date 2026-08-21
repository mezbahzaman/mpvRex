package xyz.mpv.rex.ui.player.controls

import kotlin.math.abs

// mpv publishes its cache state asynchronously, so for a moment after a seek the
// last observed values still describe the range around the *previous* playback
// position. Drawing them makes the seekbar claim content is buffered when it is
// not, until the next update corrects it a second or two later.
//
// `demuxer-cache-state/reader-pts` is the start of the buffered range, so during
// normal playback it tracks the playhead closely. A sample whose reader position
// is far from the playhead was measured before the seek and describes a range
// that no longer exists. The tolerance is wide enough to absorb ordinary drift
// between the demuxer and the reported playback position.
private const val CACHE_STATE_STALE_TOLERANCE_SECONDS = 10f

/**
 * Reports whether a cache sample still describes the range that contains
 * [currentPosition]. An unknown reader position counts as fresh so the buffered
 * indicator still works when mpv does not report it.
 */
internal fun isCacheStateFresh(readerPts: Float?, currentPosition: Float): Boolean {
  if (readerPts == null || !readerPts.isFinite()) return true
  return abs(readerPts - currentPosition) <= CACHE_STATE_STALE_TOLERANCE_SECONDS
}

internal fun bufferedEndPosition(
  currentPosition: Float,
  duration: Float,
  isNetworkMedia: Boolean,
  bufferInvalidated: Boolean,
  cacheEnd: Float?,
  cacheDuration: Float?,
  bufferedSeconds: Float?,
  readerPts: Float? = null,
): Float {
  val current = currentPosition.coerceAtLeast(0f).coerceAtMost(duration.coerceAtLeast(0f))
  if (!isNetworkMedia || bufferInvalidated) return current
  if (!isCacheStateFresh(readerPts, current)) return current

  val end = cacheEnd?.takeIf { it.isFinite() && it >= current }
    ?: cacheDuration?.takeIf { it.isFinite() && it > 0.1f }?.let { current + it }
    ?: bufferedSeconds?.takeIf { it.isFinite() && it > 0.1f }?.let { current + it }
    ?: current
  return end.coerceIn(current, duration.coerceAtLeast(current))
}
