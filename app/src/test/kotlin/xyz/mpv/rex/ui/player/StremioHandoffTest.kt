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
  fun startfromIsAcceptedAsTheStremioPositionAlias() {
    assertEquals(1_134_245L, StremioHandoff.positionMs(null, 1_134_245L))
  }

  @Test
  fun positionTakesPrecedenceOverStartfrom() {
    assertEquals(125_500L, StremioHandoff.positionMs(125_500L, 1_134_245L))
  }

  @Test
  fun resultUsesOriginalIntMillisecondContract() {
    val result = StremioHandoff.result(positionSeconds = 125, durationSeconds = 3600)

    assertEquals(125_000, result.positionMs)
    assertEquals(3_600_000, result.durationMs)
  }
}
