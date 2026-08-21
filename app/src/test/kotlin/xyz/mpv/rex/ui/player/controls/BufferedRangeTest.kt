package xyz.mpv.rex.ui.player.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

  @Test
  fun cacheMeasuredBeforeBackwardSeekIsIgnored() {
    // Seeked back to 20s while mpv still reports the cache it filled around 600s.
    assertEquals(
      20f,
      bufferedEndPosition(20f, 1800f, true, false, 640f, 40f, 40f, readerPts = 600f),
    )
  }

  @Test
  fun cacheMeasuredBeforeForwardSeekIsIgnored() {
    // Seeked forward to 900s; the sample still describes the range around 100s.
    assertEquals(
      900f,
      bufferedEndPosition(900f, 1800f, true, false, 140f, 40f, 40f, readerPts = 100f),
    )
  }

  @Test
  fun cacheMeasuredAtPlayheadIsUsed() {
    assertEquals(
      640f,
      bufferedEndPosition(600f, 1800f, true, false, 640f, 40f, 40f, readerPts = 601f),
    )
  }

  @Test
  fun missingReaderPositionKeepsBufferVisible() {
    assertEquals(
      640f,
      bufferedEndPosition(600f, 1800f, true, false, 640f, 40f, 40f, readerPts = null),
    )
  }

  @Test
  fun cacheFreshnessToleratesNormalReadaheadDrift() {
    assertTrue(isCacheStateFresh(602f, 600f))
    assertTrue(isCacheStateFresh(596f, 600f))
    assertTrue(isCacheStateFresh(null, 600f))
    assertTrue(isCacheStateFresh(Float.NaN, 600f))
    assertFalse(isCacheStateFresh(600f, 20f))
    assertFalse(isCacheStateFresh(20f, 600f))
  }

  @Test
  fun seekSettlementRequiresReaderPositionAtTarget() {
    assertTrue(isCacheStateReadyAfterSeek(902f, 900f, 900f))
    assertFalse(isCacheStateReadyAfterSeek(null, 900f, 900f))
    assertFalse(isCacheStateReadyAfterSeek(100f, 900f, 900f))
    assertFalse(isCacheStateReadyAfterSeek(902f, 850f, 900f))
  }
}
