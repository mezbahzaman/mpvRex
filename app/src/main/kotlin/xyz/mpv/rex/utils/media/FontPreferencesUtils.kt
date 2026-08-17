package xyz.mpv.rex.utils.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.k1rakishou.fsaf.FileManager
import com.yubyf.truetypeparser.TTFFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val bundledSubtitleFontAssets =
  listOf(
    "bundled_fonts/roboto/Roboto-Variable.ttf",
    "bundled_fonts/lato/Lato-Regular.ttf",
    "bundled_fonts/noto_sans/NotoSans-Variable.ttf",
  )

/** Adds bundled fonts to the user-visible font directory without replacing user files. */
fun copyBundledSubtitleFonts(context: Context) {
  runCatching {
    val destinationDir = File(context.filesDir, "fonts").apply { mkdirs() }
    bundledSubtitleFontAssets.forEach { assetPath ->
      val destination = File(destinationDir, assetPath.substringAfterLast('/'))
      if (destination.createNewFile()) {
        try {
          context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
          }
        } catch (e: Exception) {
          destination.delete()
          throw e
        }
      }
    }
  }.onFailure { e ->
    Log.e("SubtitleFonts", "Error copying bundled fonts", e)
  }
}

/** Copies font files from the selected directory to the app's internal storage. */
@SuppressLint("UseKtx")
fun copyFontsFromDirectory(
  context: Context,
  fileManager: FileManager,
  uriString: String,
) {
  runCatching {
    val destinationPath = context.filesDir.path + "/fonts"
    val destinationDir = fileManager.fromPath(destinationPath)

    if (!fileManager.exists(destinationDir)) {
      File(destinationPath).mkdirs()
    }

    val sourceDir = fileManager.fromUri(Uri.parse(uriString))
    if (sourceDir != null && fileManager.exists(sourceDir)) {
      fileManager.listFiles(sourceDir).forEach { file ->
        if (fileManager.isFile(file)) {
          val fileName = fileManager.getName(file)
          if (fileName.lowercase().matches(".*\\.[ot]tf$".toRegex())) {
            val inputStream = fileManager.getInputStream(file) ?: return@forEach
            val outputFile = File(destinationPath, fileName)
            outputFile.outputStream().use { outputStream ->
              inputStream.use { it.copyTo(outputStream) }
            }
          }
        }
      }
    }
  }.onFailure { e ->
    Log.e("SubtitlesPreferences", "Error copying fonts", e)
  }
}

// getSimplifiedPathFromUri is defined in AdvancedPreferencesScreen.kt within this package.

/** Represents a custom font discovered in the app's internal fonts directory. */
data class CustomFontEntry(
  val familyName: String,
  val file: File,
)

/**
 * Loads custom fonts (family name + file) from the app's internal fonts directory for previews.
 * Returns one entry per family name (first match kept if duplicates exist).
 */
suspend fun loadCustomFontEntries(context: Context): List<CustomFontEntry> =
  withContext(Dispatchers.IO) {
    val fontsDir = File(context.filesDir, "fonts")
    if (!fontsDir.exists()) return@withContext emptyList()

    val fontFiles =
      fontsDir
        .listFiles()
        ?.filter { it.isFile && it.name.lowercase(Locale.ROOT).matches(".*\\.[ot]tf$".toRegex()) }
        .orEmpty()

    val entries = mutableListOf<CustomFontEntry>()
    val seenFamilies = mutableSetOf<String>()

    for (fontFile in fontFiles) {
      val familyName =
        runCatching {
          fontFile.inputStream().use { input ->
            TTFFile
              .open(input)
              .families
              .values
              .firstOrNull()
          }
        }.getOrNull()

      if (!familyName.isNullOrBlank() && seenFamilies.add(familyName)) {
        entries += CustomFontEntry(familyName, fontFile)
      }
    }

    entries.sortedBy { it.familyName }
  }
