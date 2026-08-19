package xyz.mpv.rex.trakt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MdbListModelsTest {
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
    assertNotNull(request.show)
    assertEquals("tt0903747", request.show?.ids?.imdb)
    assertEquals(5, request.show?.season?.number)
    assertEquals(16, request.show?.season?.episode?.number)
    assertEquals(82.5, request.progress, 0.0)
  }

  @Test
  fun movieRequestHasNoShowOrEpisode() {
    val request = ScrobbleMediaInfo(
      title = "Inception",
      year = 2010,
      imdbId = "tt1375666",
      tmdbId = 27205,
      season = null,
      episode = null,
    ).toScrobbleRequest(45.0)

    assertNull(request.show)
    assertNotNull(request.movie)
    assertEquals("tt1375666", request.movie?.ids?.imdb)
    assertEquals(27205, request.movie?.ids?.tmdb)
    assertEquals(45.0, request.progress, 0.0)

    val encoded = json.encodeToString(request)
    assertTrue(encoded.contains("\"movie\""))
    assertFalse(encoded.contains("\"show\""))
  }

  @Test
  fun progressIsIncludedInPayload() {
    val request = ScrobbleMediaInfo(
      title = "Test Movie",
      year = 2020,
      imdbId = "tt0000000",
      tmdbId = null,
      season = null,
      episode = null,
    ).toScrobbleRequest(75.25)

    assertEquals(75.25, request.progress, 0.0)

    val encoded = json.encodeToString(request)
    assertTrue(encoded.contains("\"progress\":75.25"))
  }

  @Test
  fun episodeIdsAreNestedCorrectly() {
    val request = ScrobbleMediaInfo(
      title = "Stranger Things",
      year = 2016,
      imdbId = "tt4574334",
      tmdbId = 273181,
      season = 1,
      episode = 3,
    ).toScrobbleRequest(50.0)

    val encoded = json.encodeToString(request)
    assertTrue(encoded.contains("\"season\""))
    assertTrue(encoded.contains("\"episode\""))
    assertTrue(encoded.contains("\"number\":1"))
    assertTrue(encoded.contains("\"number\":3"))
  }
}
