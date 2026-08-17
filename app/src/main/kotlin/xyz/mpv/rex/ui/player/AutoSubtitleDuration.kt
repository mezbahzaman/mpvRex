package xyz.mpv.rex.ui.player

internal enum class AutoSubtitleDurationDecision {
  PENDING,
  SKIP,
  CONTINUE,
}

internal fun decideAutoSubtitleDuration(
  duration: Double?,
  minimumSeconds: Double,
): AutoSubtitleDurationDecision {
  if (duration == null || !duration.isFinite()) return AutoSubtitleDurationDecision.PENDING
  if (duration < minimumSeconds) return AutoSubtitleDurationDecision.SKIP
  return AutoSubtitleDurationDecision.CONTINUE
}
