package xyz.mpv.rex.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MdbListScrobbleRequest(
  val progress: Double,
  @SerialName("movie") val movie: MdbListMovie? = null,
  @SerialName("show") val show: MdbListShow? = null,
  @SerialName("app_version") val appVersion: String = "mpvRex/4.5",
)

@Serializable
data class MdbListMovie(
  val ids: MdbListMovieIds,
)

@Serializable
data class MdbListMovieIds(
  @SerialName("imdb") val imdb: String? = null,
  @SerialName("tmdb") val tmdb: Int? = null,
)

@Serializable
data class MdbListShow(
  val ids: MdbListShowIds,
  @SerialName("season") val season: MdbListSeason,
)

@Serializable
data class MdbListShowIds(
  @SerialName("imdb") val imdb: String? = null,
  @SerialName("tmdb") val tmdb: Int? = null,
  @SerialName("tvdb") val tvdb: Int? = null,
)

@Serializable
data class MdbListSeason(
  @SerialName("number") val number: Int,
  @SerialName("episode") val episode: MdbListEpisode,
)

@Serializable
data class MdbListEpisode(
  @SerialName("number") val number: Int,
)

@Serializable
data class MdbListScrobbleResponse(
  @SerialName("id") val id: Long? = null,
  @SerialName("action") val action: String? = null,
  @SerialName("progress") val progress: Double? = null,
)

data class ScrobbleMediaInfo(
  val title: String,
  val year: Int?,
  val imdbId: String?,
  val tmdbId: Int?,
  val season: Int?,
  val episode: Int?,
) {
  fun toScrobbleRequest(progress: Double): MdbListScrobbleRequest {
    val hasEpisode = season != null && episode != null
    return if (hasEpisode) {
      MdbListScrobbleRequest(
        progress = progress,
        show = MdbListShow(
          ids = MdbListShowIds(imdb = imdbId, tmdb = tmdbId),
          season = MdbListSeason(
            number = season,
            episode = MdbListEpisode(number = episode),
          ),
        ),
      )
    } else {
      MdbListScrobbleRequest(
        progress = progress,
        movie = MdbListMovie(
          ids = MdbListMovieIds(imdb = imdbId, tmdb = tmdbId),
        ),
      )
    }
  }
}
