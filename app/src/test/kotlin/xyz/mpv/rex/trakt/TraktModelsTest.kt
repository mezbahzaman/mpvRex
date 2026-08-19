package xyz.mpv.rex.trakt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktModelsTest {
  private val json = Json { explicitNulls = false }

  @Test
  fun episodeRequestUsesShowAndEpisodeIdentity() {
    val request = ScrobbleMediaInfo(
      title = "Breaking Bad",
      year = 2008,
      imdbId = "tt0903747",
      tmdbId = null,
      season = 5,
      episode = 16,
    ).toScrobbleRequest(82.5)

    assertNull(request.movie)
    assertEquals("Breaking Bad", request.show?.title)
    assertEquals("tt0903747", request.show?.ids?.imdb)
    assertEquals(5, request.episode?.season)
    assertEquals(16, request.episode?.number)
    assertEquals(82.5, request.progress, 0.0)
  }

  @Test
  fun movieRequestDoesNotInventUnknownYear() {
    val request = ScrobbleMediaInfo(
      title = "Unknown Movie",
      year = null,
      imdbId = "tt1234567",
      tmdbId = null,
      season = null,
      episode = null,
    ).toScrobbleRequest(12.0)

    assertNull(request.show)
    assertNull(request.episode)
    assertNull(request.movie?.year)
    assertEquals("tt1234567", request.movie?.ids?.imdb)

    val encoded = json.encodeToString(request)
    assertTrue(encoded.contains("\"movie\""))
    assertFalse(encoded.contains("\"year\""))
    assertFalse(encoded.contains("\"show\""))
  }
}
