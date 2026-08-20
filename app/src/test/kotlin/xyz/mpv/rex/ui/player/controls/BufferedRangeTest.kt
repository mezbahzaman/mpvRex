package xyz.mpv.rex.ui.player.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class BufferedRangeTest {
  @Test
  fun networkBufferUsesAbsoluteCacheEnd() {
    assertEquals(80f, bufferedEndPosition(20f, 120f, true, false, 80f, 10f, 5f))
  }

  @Test
  fun invalidatedBufferDoesNotShowPreSeekCache() {
    assertEquals(20f, bufferedEndPosition(20f, 180f, true, true, 160f, 140f, 140f))
  }

  @Test
  fun cacheDurationIsRelativeAndClampedToMediaDuration() {
    assertEquals(100f, bufferedEndPosition(80f, 100f, true, false, null, 30f, null))
  }

  @Test
  fun localMediaDoesNotShowBufferedRange() {
    assertEquals(20f, bufferedEndPosition(20f, 120f, false, false, 80f, 60f, 60f))
  }
}
