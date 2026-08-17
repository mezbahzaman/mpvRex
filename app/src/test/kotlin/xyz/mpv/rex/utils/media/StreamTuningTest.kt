package xyz.mpv.rex.utils.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamTuningTest {
  @Test
  fun recognizesNetworkUris() {
    assertTrue(StreamTuning.isNetworkUri("https://example.com/video.mp4"))
    assertFalse(StreamTuning.isNetworkUri("/storage/emulated/0/video.mp4"))
  }

  @Test
  fun recognizesStremioTorrentProxyUris() {
    assertTrue(StreamTuning.isStremioTorrentUri("http://127.0.0.1:11470/0123456789012345678901234567890123456789/video.mp4"))
    assertFalse(StreamTuning.isStremioTorrentUri("https://example.com/video.mp4"))
  }
}
