package xyz.mpv.rex.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSubtitleOptOutTest {
  private val media = "http://127.0.0.1:11470/stream.mkv"
  private val subtitle = "file:///data/subs/movie.en.srt"

  @Test
  fun automaticLoadingIsAllowedBeforeAnyDeletion() {
    val optOut = AutoSubtitleOptOut()
    assertTrue(optOut.allowsAutomaticSubtitle(subtitle, media))
    assertTrue(optOut.allowsAutomaticSearch(media))
  }

  @Test
  fun deletedSubtitleIsNotLoadedAgain() {
    val optOut = AutoSubtitleOptOut()
    optOut.markRemovedByUser(listOf(subtitle), media)
    assertFalse(optOut.allowsAutomaticSubtitle(subtitle, media))
    assertFalse(optOut.allowsAutomaticSearch(media))
  }

  @Test
  fun deletionBlocksEveryAliasOfTheSameSubtitle() {
    val optOut = AutoSubtitleOptOut()
    val mpvPath = "/data/subs/movie.en.srt"
    optOut.markRemovedByUser(listOf(subtitle, mpvPath), media)
    assertFalse(optOut.allowsAutomaticSubtitle(mpvPath, media))
  }

  @Test
  fun deletionOfAnEmbeddedTrackStillStopsAutomaticSearch() {
    val optOut = AutoSubtitleOptOut()
    optOut.markRemovedByUser(emptyList(), media)
    assertFalse(optOut.allowsAutomaticSearch(media))
  }

  @Test
  fun deletionDoesNotAffectOtherMedia() {
    val optOut = AutoSubtitleOptOut()
    optOut.markRemovedByUser(listOf(subtitle), media)
    val otherMedia = "http://127.0.0.1:11470/other.mkv"
    assertTrue(optOut.allowsAutomaticSearch(otherMedia))
    assertTrue(optOut.allowsAutomaticSubtitle(subtitle, otherMedia))
  }

  @Test
  fun manualChoiceOverridesAnEarlierDeletion() {
    val optOut = AutoSubtitleOptOut()
    optOut.markRemovedByUser(listOf(subtitle), media)
    optOut.markChosenByUser(subtitle, media)
    assertTrue(optOut.allowsAutomaticSubtitle(subtitle, media))
    assertTrue(optOut.allowsAutomaticSearch(media))
  }

  @Test
  fun unknownMediaPathNeverBlocksLoading() {
    val optOut = AutoSubtitleOptOut()
    optOut.markRemovedByUser(listOf(subtitle), null)
    assertTrue(optOut.allowsAutomaticSubtitle(subtitle, null))
    assertTrue(optOut.allowsAutomaticSubtitle(subtitle, media))
  }
}
