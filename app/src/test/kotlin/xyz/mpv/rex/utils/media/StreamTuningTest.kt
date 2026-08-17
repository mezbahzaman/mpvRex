package xyz.mpv.rex.utils.media

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamTuningTest {
  @Test
  fun positiveLimitsAreAppliedTogether() {
    assertEquals(StreamTuning.CacheLimits("200MiB", "180"), StreamTuning.cacheLimits(200, 180))
  }

  @Test
  fun zeroByteLimitDisablesOnlyByteThreshold() {
    assertEquals("180", StreamTuning.cacheLimits(0, 180).readaheadSeconds)
    assertEquals("4611686018427387903", StreamTuning.cacheLimits(0, 180).maximumBytes)
  }

  @Test
  fun zeroSecondLimitDisablesOnlyTimeThreshold() {
    assertEquals("200MiB", StreamTuning.cacheLimits(200, 0).maximumBytes)
    assertEquals("1.7976931348623157e308", StreamTuning.cacheLimits(200, 0).readaheadSeconds)
  }

  @Test
  fun bothZeroLimitsDisableBothThresholds() {
    assertEquals(
      StreamTuning.CacheLimits("4611686018427387903", "1.7976931348623157e308"),
      StreamTuning.cacheLimits(0, 0),
    )
  }
}
