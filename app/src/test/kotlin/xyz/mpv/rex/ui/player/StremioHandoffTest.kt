package xyz.mpv.rex.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StremioHandoffTest {
  @Test
  fun positionAcceptsCommonNumericTypes() {
    assertEquals(125_500L, StremioHandoff.positionMs(125_500L))
    assertEquals(125_500L, StremioHandoff.positionMs(125_500.9))
    assertEquals(125_500L, StremioHandoff.positionMs("125500"))
  }

  @Test
  fun malformedOrNegativePositionIsIgnored() {
    assertNull(StremioHandoff.positionMs("nope"))
    assertNull(StremioHandoff.positionMs(-1L))
    assertNull(StremioHandoff.positionMs(Double.NaN))
  }

  @Test
  fun resultIsClampedToKnownDuration() {
    val result = StremioHandoff.normalizeResult(125_000L, 100_000L)

    assertEquals(100_000L, result.positionMs)
    assertEquals(100_000L, result.durationMs)
  }

  @Test
  fun secondsAreConvertedToPreciseMilliseconds() {
    assertEquals(12_346L, StremioHandoff.millisecondsFromSeconds(12.3456))
    assertNull(StremioHandoff.millisecondsFromSeconds(Double.POSITIVE_INFINITY))
  }
}
