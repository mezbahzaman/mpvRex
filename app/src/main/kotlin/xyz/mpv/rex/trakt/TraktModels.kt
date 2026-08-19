package xyz.mpv.rex.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Scrobble request models ----

@Serializable
data class TraktScrobbleRequest(
  val progress: Double,
  @SerialName("movie") val movie: TraktMovie? = null,
  @SerialName("show") val show: TraktShow? = null,
  @SerialName("episode") val episode: TraktEpisode? = null,
)

@Serializable
data class TraktMovie(
  val title: String,
  val year: Int? = null,
  val ids: TraktMovieIds,
)

@Serializable
data class TraktMovieIds(
  @SerialName("trakt") val trakt: Int? = null,
  @SerialName("imdb") val imdb: String? = null,
  @SerialName("tmdb") val tmdb: Int? = null,
)

@Serializable
data class TraktShow(
  val title: String,
  val year: Int? = null,
  val ids: TraktShowIds,
)

@Serializable
data class TraktShowIds(
  @SerialName("trakt") val trakt: Int? = null,
  @SerialName("tvdb") val tvdb: Int? = null,
  @SerialName("imdb") val imdb: String? = null,
  @SerialName("tmdb") val tmdb: Int? = null,
)

@Serializable
data class TraktEpisode(
  val ids: TraktEpisodeIds? = null,
  @SerialName("season") val season: Int? = null,
  @SerialName("number") val number: Int? = null,
)

@Serializable
data class TraktEpisodeIds(
  @SerialName("trakt") val trakt: Int? = null,
  @SerialName("tvdb") val tvdb: Int? = null,
)

// ---- Scrobble response models ----

@Serializable
data class TraktScrobbleResponse(
  @SerialName("id") val id: Long? = null,
  @SerialName("progress") val progress: Double? = null,
  @SerialName("action") val action: String? = null,
  @SerialName("movie") val movie: TraktMovie? = null,
  @SerialName("episode") val episode: TraktEpisode? = null,
  @SerialName("show") val show: TraktShow? = null,
)

// ---- OAuth models ----

@Serializable
data class TraktTokenRequest(
  @SerialName("code") val code: String,
  @SerialName("client_id") val clientId: String,
  @SerialName("client_secret") val clientSecret: String,
  @SerialName("redirect_uri") val redirectUri: String,
  @SerialName("grant_type") val grantType: String = "authorization_code",
)

@Serializable
data class TraktRefreshTokenRequest(
  @SerialName("refresh_token") val refreshToken: String,
  @SerialName("client_id") val clientId: String,
  @SerialName("client_secret") val clientSecret: String,
  @SerialName("redirect_uri") val redirectUri: String,
  @SerialName("grant_type") val grantType: String = "refresh_token",
)

@Serializable
data class TraktTokenResponse(
  @SerialName("access_token") val accessToken: String,
  @SerialName("token_type") val tokenType: String,
  @SerialName("expires_in") val expiresIn: Int,
  @SerialName("refresh_token") val refreshToken: String,
  @SerialName("scope") val scope: String,
  @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class TraktSettingsResponse(
  @SerialName("user") val user: TraktUserResponse,
)

@Serializable
data class TraktUserResponse(
  @SerialName("username") val username: String? = null,
  @SerialName("ids") val ids: TraktUserIds? = null,
)

@Serializable
data class TraktUserIds(
  @SerialName("slug") val slug: String? = null,
)

// ---- Content identification helper ----

data class ScrobbleMediaInfo(
  val title: String,
  val year: Int?,
  val imdbId: String?,
  val tmdbId: Int?,
  val season: Int?,
  val episode: Int?,
) {
  fun toScrobbleRequest(progress: Double): TraktScrobbleRequest {
    val hasEpisode = season != null && episode != null
    return if (hasEpisode) {
      TraktScrobbleRequest(
        progress = progress,
        show = TraktShow(
          title = title,
          year = year,
          ids = TraktShowIds(imdb = imdbId, tmdb = tmdbId),
        ),
        episode = TraktEpisode(
          season = season,
          number = episode,
        ),
      )
    } else {
      TraktScrobbleRequest(
        progress = progress,
        movie = TraktMovie(
          title = title,
          year = year,
          ids = TraktMovieIds(imdb = imdbId, tmdb = tmdbId),
        ),
      )
    }
  }
}
