package xyz.mpv.rex.ui.browser.videolist

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.mpv.rex.database.entities.PlaybackStateEntity

class VideoListPlaybackStateTest {
  @Test
  fun `folder video uses location playback state before legacy filename`() {
    val pathState = state("/storage/A/clip.mp4", watched = true)
    val legacyState = state("clip.mp4", watched = false)

    assertEquals(
      pathState,
      playbackStateForVideo(listOf(legacyState, pathState), "/storage/A/clip.mp4", "clip.mp4"),
    )
  }

  @Test
  fun `folder video retains legacy filename fallback`() {
    val legacyState = state("clip.mp4", watched = true)

    assertEquals(
      legacyState,
      playbackStateForVideo(listOf(legacyState), "/storage/A/clip.mp4", "clip.mp4"),
    )
  }

  private fun state(title: String, watched: Boolean) = PlaybackStateEntity(
    mediaTitle = title,
    lastPosition = 0,
    playbackSpeed = 1.0,
    sid = -1,
    subDelay = 0,
    subSpeed = 1.0,
    aid = -1,
    audioDelay = 0,
    hasBeenWatched = watched,
  )
}
