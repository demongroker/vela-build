package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsScreen
import live.mehiz.mpvkt.ui.utils.LocalBackStack
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val resolvedIndex = remember(context) {
      SettingsSearchIndex.items.map {
        SettingsSearch.ResolvedItem(
          destinationKey = it.destinationKey,
          title = context.getString(it.titleRes),
          summary = it.summaryRes?.let(context::getString) ?: "",
        )
      }
    }
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(R.string.pref_preferences)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          item(key = "search_entry") {
            OutlinedTextField(
              value = query,
              onValueChange = { query = it },
              singleLine = true,
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              placeholder = { Text(text = stringResource(R.string.pref_search_hint)) },
              trailingIcon = {
                if (query.isNotEmpty()) {
                  IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.generic_cancel))
                  }
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            )
          }
          if (query.isBlank()) {
            SettingsGroup.entries.forEach { group ->
              item(key = "group_${group.name}") {
                PreferenceCategory(
                  title = { Text(text = stringResource(group.titleRes)) },
                )
              }
              SettingsHub.destinations.filter { it.group == group }.forEach { destination ->
                preference(
                  key = destination.key,
                  title = { Text(text = stringResource(destination.titleRes)) },
                  summary = { Text(text = stringResource(destination.summaryRes)) },
                  icon = { Icon(iconFor(destination.key), null) },
                  onClick = { backstack.add(screenFor(destination.key)) },
                )
              }
            }
          } else {
            val matches = SettingsSearch.search(query, resolvedIndex)
            if (matches.isEmpty()) {
              item(key = "search_empty") {
                Text(
                  text = stringResource(R.string.pref_search_no_results, query),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
              }
            } else {
              matches.forEach { match ->
                val destination = SettingsHub.destinationFor(match.destinationKey)
                preference(
                  key = "search_${match.destinationKey}_${match.title}",
                  title = { Text(text = match.title) },
                  summary = {
                    Text(text = match.summary.ifBlank { stringResource(destination.titleRes) })
                  },
                  icon = { Icon(iconFor(destination.key), null) },
                  onClick = { backstack.add(screenFor(destination.key)) },
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun screenFor(key: String): Screen = when (key) {
  "appearance" -> AppearancePreferencesScreen
  "player" -> PlayerPreferencesScreen
  "gesture" -> GesturePreferencesScreen
  "decoder" -> DecoderPreferencesScreen
  "subtitles" -> SubtitlesPreferencesScreen
  "audio" -> AudioPreferencesScreen
  "customButtons" -> CustomButtonsScreen
  "advanced" -> AdvancedPreferencesScreen
  "about" -> AboutScreen
  else -> error("Unknown settings destination: $key")
}

private fun iconFor(key: String): ImageVector = when (key) {
  "appearance" -> Icons.Outlined.Palette
  "player" -> Icons.Outlined.PlayCircle
  "gesture" -> Icons.Outlined.Gesture
  "decoder" -> Icons.Outlined.Memory
  "subtitles" -> Icons.Outlined.Subtitles
  "audio" -> Icons.Outlined.Audiotrack
  "customButtons" -> Icons.Outlined.Terminal
  "advanced" -> Icons.Outlined.Code
  else -> Icons.Outlined.Info
}