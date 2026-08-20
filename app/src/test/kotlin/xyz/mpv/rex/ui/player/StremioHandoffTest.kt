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
  fun resultPreservesPreciseIntMillisecondContract() {
    val result = StremioHandoff.result(positionMs = 125_750L, durationMs = 3_600_250L)

    assertEquals(125_750, result.positionMs)
    assertEquals(3_600_250, result.durationMs)
  }

  @Test
  fun resultRejectsNegativeValuesAndClampsIntOverflow() {
    val result = StremioHandoff.result(positionMs = -1L, durationMs = Long.MAX_VALUE)

    assertNull(result.positionMs)
    assertEquals(Int.MAX_VALUE, result.durationMs)
  }

  @Test
  fun secondsAreConvertedWithoutLosingSubSecondPrecision() {
    assertEquals(125_750L, StremioHandoff.secondsToMilliseconds(125.75))
    assertNull(StremioHandoff.secondsToMilliseconds(Double.NaN))
  }
}
