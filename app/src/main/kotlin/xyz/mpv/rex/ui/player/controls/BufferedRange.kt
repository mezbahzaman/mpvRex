package xyz.mpv.rex.ui.player.controls

internal fun bufferedEndPosition(
  currentPosition: Float,
  duration: Float,
  isNetworkMedia: Boolean,
  bufferInvalidated: Boolean,
  cacheEnd: Float?,
  cacheDuration: Float?,
  bufferedSeconds: Float?,
): Float {
  val current = currentPosition.coerceAtLeast(0f).coerceAtMost(duration.coerceAtLeast(0f))
  if (!isNetworkMedia || bufferInvalidated) return current

  val end = cacheEnd?.takeIf { it.isFinite() && it >= current }
    ?: cacheDuration?.takeIf { it.isFinite() && it > 0.1f }?.let { current + it }
    ?: bufferedSeconds?.takeIf { it.isFinite() && it > 0.1f }?.let { current + it }
    ?: current
  return end.coerceIn(current, duration.coerceAtLeast(current))
}
