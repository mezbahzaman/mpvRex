package xyz.mpv.rex.utils.media

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamStatsFetcherTest {
  @Test
  fun normalizesTrackerSources() {
    assertEquals(
      listOf("udp://tracker.example:6969/announce", "https://tracker.example/announce"),
      StreamStatsFetcher.normalizeTrackerUrls(listOf(
        "tracker:udp://tracker.example:6969/announce",
        "tracker:https://tracker.example/announce",
        "dht:ignored",
      )),
    )
  }

  @Test
  fun formatsZeroAndMegabyteSpeeds() {
    assertEquals("0 KB/s", StreamStatsFetcher.formatSpeed(0))
    assertEquals("1.3 MB/s", StreamStatsFetcher.formatSpeed(1_300_000))
  }
}
