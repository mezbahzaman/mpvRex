package xyz.mpv.rex.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSubtitleSourceTest {
  @Test
  fun localMoviesAreEligible() {
    assertTrue(isEligibleLocalAutoSubtitle("/storage/emulated/0/Movies/Example.2026.mkv", emptySet()))
  }

  @Test
  fun cameraVideosAreNeverEligible() {
    assertFalse(isEligibleLocalAutoSubtitle("/storage/emulated/0/DCIM/Camera/VID_20260817.mp4", emptySet()))
    assertFalse(isEligibleLocalAutoSubtitle("/storage/emulated/0/Movies/PXL_20260817.mp4", emptySet()))
  }

  @Test
  fun unidentifiedFileDescriptorsAreNotEligible() {
    assertFalse(isEligibleLocalAutoSubtitle("fd://42", emptySet()))
  }

  @Test
  fun contentUrisRetainCameraIdentity() {
    assertFalse(
      isEligibleLocalAutoSubtitle(
        "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FCamera%2FVID_20260817.mp4",
        emptySet(),
      ),
    )
  }

  @Test
  fun userExcludedFoldersAreNotEligible() {
    val excluded = setOf("content://com.android.externalstorage.documents/tree/primary%3AMovies%2FPersonal")
    assertFalse(isEligibleLocalAutoSubtitle("/storage/emulated/0/Movies/Personal/video.mkv", excluded))
  }
}
