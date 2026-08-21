package xyz.mpv.rex.ui.player

/**
 * Remembers that the user deleted a subtitle so automatic loading cannot bring
 * it back while the same media keeps playing.
 *
 * Deleting a subtitle track is an explicit "I do not want this subtitle" signal,
 * so it blocks both the exact subtitle file and any further automatic online
 * search for the media it was removed from. Manual actions — the file picker and
 * the online search sheet — clear the block again, so a user who changes their
 * mind is never locked out.
 *
 * Entries are scoped to the media path they were recorded for, so a different
 * file or stream always starts with automatic loading enabled again. Access is
 * synchronized because deletions arrive on IO dispatchers while the automatic
 * loader reads this from its own coroutine.
 */
internal class AutoSubtitleOptOut {
  private val removedSubtitles = mutableSetOf<Pair<String, String>>()
  private val optedOutMediaPaths = mutableSetOf<String>()

  /**
   * Records a user deletion. [identifiers] holds every name the subtitle is
   * known by (its original URI and the path handed to mpv) so a later automatic
   * add is recognized regardless of which one it uses.
   */
  @Synchronized
  fun markRemovedByUser(identifiers: Collection<String?>, mediaPath: String?) {
    val media = mediaPath?.takeIf { it.isNotBlank() } ?: return
    identifiers.filterNotNull().filter { it.isNotBlank() }.forEach { removedSubtitles += media to it }
    optedOutMediaPaths += media
  }

  /** Records a deliberate user choice, undoing a previous deletion of [identifier]. */
  @Synchronized
  fun markChosenByUser(identifier: String, mediaPath: String?) {
    val media = mediaPath?.takeIf { it.isNotBlank() } ?: return
    removedSubtitles -= media to identifier
    optedOutMediaPaths -= media
  }

  @Synchronized
  fun allowsAutomaticSubtitle(identifier: String?, mediaPath: String?): Boolean {
    val media = mediaPath?.takeIf { it.isNotBlank() } ?: return true
    if (media in optedOutMediaPaths) return false
    val name = identifier?.takeIf { it.isNotBlank() } ?: return true
    return (media to name) !in removedSubtitles
  }

  @Synchronized
  fun allowsAutomaticSearch(mediaPath: String): Boolean = mediaPath !in optedOutMediaPaths
}
