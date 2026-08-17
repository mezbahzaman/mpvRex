package xyz.mpv.rex.domain.thumbnail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekPreviewRepositoryTest {
  @Test
  fun `frame count stays small and bounded`() {
    assertEquals(12, SeekPreviewRepository.frameCount(60f))
    assertEquals(12, SeekPreviewRepository.frameCount(3600f))
    assertEquals(32, SeekPreviewRepository.frameCount(20_000f))
  }

  @Test
  fun `sample positions cover the timeline`() {
    val positions = SeekPreviewRepository.samplePositions(durationSeconds = 100f, count = 5)

    assertEquals(0f, positions.first(), 0.001f)
    assertEquals(99.75f, positions.last(), 0.001f)
    assertTrue(positions.zipWithNext().all { (first, second) -> first < second })
  }

  @Test
  fun `nearest frame clamps positions to timeline`() {
    assertEquals(0, SeekPreviewRepository.nearestFrameIndex(-10f, 100f, 5))
    assertEquals(2, SeekPreviewRepository.nearestFrameIndex(50f, 100f, 5))
    assertEquals(4, SeekPreviewRepository.nearestFrameIndex(150f, 100f, 5))
  }

  @Test
  fun `progressive order contains every frame once`() {
    val order = SeekPreviewRepository.progressiveOrder(17)

    assertEquals(17, order.size)
    assertEquals((0 until 17).toSet(), order.toSet())
    assertEquals(8, order.first())
  }
}
