package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.presentation.components.PlayerSheet
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.PlayerControlLabels
import live.mehiz.mpvkt.ui.player.controls.QuickControlsLayout
import live.mehiz.mpvkt.ui.player.controls.QuickTransportSlot
import live.mehiz.mpvkt.ui.theme.spacing

/**
 * One-thumb controls sheet: the daily transport actions in a fixed five-slot
 * row plus a grid of large labelled targets, all inside the bottom sheet so
 * nothing needs a reach to the top corners. The speed / aspect / decoder
 * indicators stay visible while the sheet is open.
 */
@Composable
fun QuickControlsSheet(
  isPlaying: Boolean,
  onPlayPause: () -> Unit,
  onSeekRelative: (Int) -> Unit,
  hasPrevious: Boolean,
  hasNext: Boolean,
  onPlayPrevious: () -> Unit,
  onPlayNext: () -> Unit,
  speed: Float,
  aspect: VideoAspect,
  decoder: Decoder,
  onCycleAspect: () -> Unit,
  onOpenSheet: (Sheets) -> Unit,
  onLockControls: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    BoxWithConstraints {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(MaterialTheme.spacing.medium)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        Text(
          text = stringResource(R.string.player_sheets_quick_title),
          style = MaterialTheme.typography.headlineMedium,
        )
        // Persistent indicators: speed, aspect and decoder never hide while
        // the sheet is open, and each one is also the shortcut to change it.
        Row(
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          AssistChip(
            onClick = { onOpenSheet(Sheets.PlaybackSpeed) },
            label = { Text(QuickControlsLayout.speedLabel(speed)) },
          )
          AssistChip(
            onClick = onCycleAspect,
            label = { Text(stringResource(aspect.titleRes)) },
          )
          AssistChip(
            onClick = { onOpenSheet(Sheets.Decoders) },
            label = { Text(decoder.title) },
          )
        }
        // Fixed five-slot transport row: missing queue neighbours disable in
        // place instead of removing themselves, so play/pause never moves.
        Row(modifier = Modifier.fillMaxWidth()) {
          QuickControlsLayout.transportSlots().forEach { slot ->
            val enabled = QuickControlsLayout.slotEnabled(slot, hasPrevious, hasNext)
            Box(
              modifier = Modifier
                .weight(1f)
                .height((QuickControlsLayout.MIN_TARGET_DP + 16).dp),
              contentAlignment = Alignment.Center,
            ) {
              IconButton(
                onClick = {
                  when (slot) {
                    QuickTransportSlot.Previous -> onPlayPrevious()
                    QuickTransportSlot.Back10 -> onSeekRelative(-QuickControlsLayout.SEEK_STEP_SECONDS)
                    QuickTransportSlot.PlayPause -> onPlayPause()
                    QuickTransportSlot.Forward10 -> onSeekRelative(QuickControlsLayout.SEEK_STEP_SECONDS)
                    QuickTransportSlot.Next -> onPlayNext()
                  }
                },
                enabled = enabled,
                modifier = Modifier.size(QuickControlsLayout.MIN_TARGET_DP.dp),
              ) {
                Icon(
                  imageVector = transportIcon(slot, isPlaying),
                  contentDescription = transportLabel(slot),
                  modifier = Modifier.size(32.dp),
                )
              }
            }
          }
        }
        val actions = listOf(
          QuickAction(Icons.Default.Speed, R.string.player_quick_speed, PlayerControlLabels.PLAYBACK_SPEED) {
            onOpenSheet(Sheets.PlaybackSpeed)
          },
          QuickAction(
            Icons.Default.AspectRatio,
            R.string.player_quick_aspect,
            PlayerControlLabels.VIDEO_ASPECT_RATIO,
            onCycleAspect,
          ),
          QuickAction(Icons.Default.Subtitles, R.string.player_quick_subtitles, PlayerControlLabels.SUBTITLE_TRACKS) {
            onOpenSheet(Sheets.SubtitleTracks)
          },
          QuickAction(Icons.Default.Audiotrack, R.string.player_quick_audio, PlayerControlLabels.AUDIO_TRACKS) {
            onOpenSheet(Sheets.AudioTracks)
          },
          QuickAction(Icons.Default.Memory, R.string.player_quick_decoder, PlayerControlLabels.DECODER) {
            onOpenSheet(Sheets.Decoders)
          },
          QuickAction(Icons.Default.MoreHoriz, R.string.player_quick_more, PlayerControlLabels.MORE_OPTIONS) {
            onOpenSheet(Sheets.More)
          },
          QuickAction(
            Icons.Default.Lock,
            R.string.player_quick_lock,
            PlayerControlLabels.LOCK_CONTROLS,
            onLockControls,
          ),
        )
        val columns = QuickControlsLayout.actionColumns(maxWidth.value.toInt())
        actions.chunked(columns).forEach { rowActions ->
          Row(modifier = Modifier.fillMaxWidth()) {
            rowActions.forEach { action ->
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                IconButton(
                  onClick = action.onClick,
                  modifier = Modifier.size(QuickControlsLayout.MIN_TARGET_DP.dp),
                ) {
                  Icon(action.icon, contentDescription = action.accessibilityLabel)
                }
                Text(
                  text = stringResource(action.labelRes),
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            repeat(columns - rowActions.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

private data class QuickAction(
  val icon: ImageVector,
  @StringRes val labelRes: Int,
  val accessibilityLabel: String,
  val onClick: () -> Unit,
)

private fun transportIcon(slot: QuickTransportSlot, isPlaying: Boolean): ImageVector = when (slot) {
  QuickTransportSlot.Previous -> Icons.Default.SkipPrevious
  QuickTransportSlot.Back10 -> Icons.Default.Replay10
  QuickTransportSlot.PlayPause -> if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
  QuickTransportSlot.Forward10 -> Icons.Default.Forward10
  QuickTransportSlot.Next -> Icons.Default.SkipNext
}

private fun transportLabel(slot: QuickTransportSlot): String = when (slot) {
  QuickTransportSlot.Previous -> PlayerControlLabels.PREVIOUS_FILE
  QuickTransportSlot.Back10 -> PlayerControlLabels.SKIP_BACK_10
  QuickTransportSlot.PlayPause -> PlayerControlLabels.PLAY_PAUSE
  QuickTransportSlot.Forward10 -> PlayerControlLabels.SKIP_FORWARD_10
  QuickTransportSlot.Next -> PlayerControlLabels.NEXT_FILE
}