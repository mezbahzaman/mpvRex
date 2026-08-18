package xyz.mpv.rex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoSubtitleDurationTest {

  private val minimumSeconds = 10 * 60.0

  @Test
  fun decideAutoSubtitleDuration_unknownDuration_remainsPending() {
    assertEquals(AutoSubtitleDurationDecision.PENDING, decideAutoSubtitleDuration(null, minimumSeconds))
    assertEquals(AutoSubtitleDurationDecision.PENDING, decideAutoSubtitleDuration(Double.NaN, minimumSeconds))
    assertEquals(
      AutoSubtitleDurationDecision.PENDING,
      decideAutoSubtitleDuration(Double.POSITIVE_INFINITY, minimumSeconds),
    )
  }

  @Test
  fun decideAutoSubtitleDuration_knownDuration_appliesMinimumRuntime() {
    assertEquals(
      AutoSubtitleDurationDecision.SKIP,
      decideAutoSubtitleDuration(minimumSeconds - 1, minimumSeconds),
    )
    assertEquals(
      AutoSubtitleDurationDecision.CONTINUE,
      decideAutoSubtitleDuration(minimumSeconds, minimumSeconds),
    )
    assertEquals(
      AutoSubtitleDurationDecision.CONTINUE,
      decideAutoSubtitleDuration(minimumSeconds + 1, minimumSeconds),
    )
  }
}
