package xyz.mpv.rex.ui.player

import java.net.URLDecoder
import xyz.mpv.rex.utils.media.StreamTuning

internal fun isEligibleLocalAutoSubtitle(
  path: String,
  excludedFolders: Set<String>,
): Boolean {
  if (path.isBlank() || StreamTuning.isNetworkUri(path)) return false
  if (path.startsWith("fd://", ignoreCase = true)) return false

  fun decode(value: String): String =
    runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)

  val decodedPath = decode(path).replace('\\', '/').lowercase()
  if (listOf("/dcim/camera/", "/pictures/camera/", "/camera/").any(decodedPath::contains)) return false

  val fileName = decodedPath.substringAfterLast('/')
  if (Regex("^(img|vid|pxl|dsc|mvimg)[-_]?\\d", RegexOption.IGNORE_CASE).containsMatchIn(fileName)) return false

  return excludedFolders.none { folder ->
    val decodedFolder = decode(folder).replace('\\', '/').lowercase().trimEnd('/')
    val relativeFolder = decodedFolder.substringAfterLast(':').trim('/')
    decodedFolder.isNotBlank() &&
      (decodedPath.startsWith("$decodedFolder/") ||
        (relativeFolder.isNotBlank() && decodedPath.contains("/$relativeFolder/")))
  }
}
