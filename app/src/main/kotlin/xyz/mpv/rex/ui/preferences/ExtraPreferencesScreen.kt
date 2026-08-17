package xyz.mpv.rex.ui.preferences

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.ExtraPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.preferences.components.SwitchPreference
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.utils.media.OpenDocumentTreeContract

@Serializable
object ExtraPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<ExtraPreferences>()
    val autoStremioSubtitles by preferences.autoStremioSubtitles.collectAsState()
    val streamInfoRefreshSeconds by preferences.streamInfoRefreshSeconds.collectAsState()
    val pingHost by preferences.pingHost.collectAsState()
    val autoLocalSubtitles by preferences.autoLocalSubtitles.collectAsState()
    val excludedFolders by preferences.localSubtitleExcludedFolders.collectAsState()

    var numberDialog by remember { mutableStateOf<NumberSetting?>(null) }
    var showPingHostDialog by remember { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        preferences.localSubtitleExcludedFolders.set(excludedFolders + uri.toString())
      }
    }

    numberDialog?.let { setting ->
      ValidatedNumberDialog(
        title = stringResource(setting.titleRes),
        initialValue = setting.value,
        minimumValue = setting.minimumValue,
        onDismiss = { numberDialog = null },
        onSave = {
          setting.save(it)
          numberDialog = null
        },
      )
    }
    if (showPingHostDialog) {
      ValidatedTextDialog(
        title = stringResource(R.string.pref_extra_ping_host_title),
        initialValue = pingHost,
        onDismiss = { showPingHostDialog = false },
        onSave = {
          preferences.pingHost.set(it)
          showPingHostDialog = false
        },
      )
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_extra_settings_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backStack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        val navBarHeight = xyz.mpv.rex.ui.browser.LocalNavigationBarHeight.current
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = navBarHeight + 16.dp),
        ) {
          item { PreferenceSectionHeader(stringResource(R.string.pref_extra_streaming_section)) }
          item {
            PreferenceCard {
              SwitchPreference(
                value = autoStremioSubtitles,
                onValueChange = preferences.autoStremioSubtitles::set,
                title = { Text(stringResource(R.string.pref_subtitles_auto_stremio_title)) },
                summary = { Text(stringResource(R.string.pref_subtitles_auto_stremio_summary)) },
              )
              PreferenceDivider()
              ValuePreference(
                title = stringResource(R.string.pref_extra_stream_info_refresh_title),
                summary = stringResource(R.string.pref_extra_seconds_value, streamInfoRefreshSeconds),
                onClick = {
                  numberDialog =
                    NumberSetting(
                      R.string.pref_extra_stream_info_refresh_title,
                      streamInfoRefreshSeconds,
                      1,
                      preferences.streamInfoRefreshSeconds::set,
                    )
                },
              )
              PreferenceDivider()
              ValuePreference(
                title = stringResource(R.string.pref_extra_ping_host_title),
                summary = pingHost,
                onClick = { showPingHostDialog = true },
              )
            }
          }

          item { PreferenceSectionHeader(stringResource(R.string.pref_extra_local_subtitles_section)) }
          item {
            PreferenceCard {
              SwitchPreference(
                value = autoLocalSubtitles,
                onValueChange = preferences.autoLocalSubtitles::set,
                title = { Text(stringResource(R.string.pref_extra_auto_local_subtitles_title)) },
                summary = { Text(stringResource(R.string.pref_extra_auto_local_subtitles_summary)) },
              )
            }
          }
          if (autoLocalSubtitles) {
            item { PreferenceSectionHeader(stringResource(R.string.pref_extra_excluded_folders_title)) }
            item {
              PreferenceCard {
                Preference(
                  title = { Text(stringResource(R.string.pref_extra_add_excluded_folder_title)) },
                  summary = { Text(stringResource(R.string.pref_extra_add_excluded_folder_summary)) },
                  icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                  onClick = { folderPicker.launch(null) },
                )
                excludedFolders.sorted().forEach { folder ->
                  PreferenceDivider()
                  Row(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = folder,
                      modifier = Modifier.weight(1f),
                      style = MaterialTheme.typography.bodyMedium,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { preferences.localSubtitleExcludedFolders.set(excludedFolders - folder) }) {
                      Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.pref_extra_remove_excluded_folder),
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class NumberSetting(
  val titleRes: Int,
  val value: Int,
  val minimumValue: Int,
  val save: (Int) -> Unit,
)

@Composable
private fun ValuePreference(
  title: String,
  summary: String,
  onClick: () -> Unit,
) {
  Preference(
    title = { Text(title) },
    summary = { Text(summary, color = MaterialTheme.colorScheme.outline) },
    onClick = onClick,
  )
}

@Composable
private fun ValidatedNumberDialog(
  title: String,
  initialValue: Int,
  minimumValue: Int,
  onDismiss: () -> Unit,
  onSave: (Int) -> Unit,
) {
  var text by remember(initialValue) { mutableStateOf(initialValue.toString()) }
  val value = text.toIntOrNull()
  val valid = value != null && value >= minimumValue
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter(Char::isDigit) },
        singleLine = true,
        isError = text.isNotEmpty() && !valid,
        supportingText =
          if (!valid) {
            {
              Text(
                stringResource(
                  if (minimumValue == 0) {
                    R.string.pref_extra_non_negative_number_error
                  } else {
                    R.string.pref_extra_positive_number_error
                  },
                ),
              )
            }
          } else {
            null
          },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
    },
    confirmButton = {
      TextButton(onClick = { onSave(value!!) }, enabled = valid) { Text(stringResource(R.string.generic_ok)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } },
  )
}

@Composable
private fun ValidatedTextDialog(
  title: String,
  initialValue: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember(initialValue) { mutableStateOf(initialValue) }
  val normalized = text.trim()
  val valid = normalized.isNotEmpty() && normalized.none(Char::isWhitespace)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        isError = text.isNotEmpty() && !valid,
        supportingText =
          if (!valid) {
            { Text(stringResource(R.string.pref_extra_host_error)) }
          } else {
            null
          },
      )
    },
    confirmButton = {
      TextButton(onClick = { onSave(normalized) }, enabled = valid) { Text(stringResource(R.string.generic_ok)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } },
  )
}
