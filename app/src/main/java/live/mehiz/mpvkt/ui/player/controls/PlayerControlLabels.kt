package live.mehiz.mpvkt.ui.player.controls

/**
 * Accessibility labels for the player's icon-only buttons.
 *
 * `ControlsButton` passes its `title` to `Icon(contentDescription = …)`, so these
 * strings are what TalkBack reads. They are constants so a test can prove none of
 * them is blank or duplicated.
 */
object PlayerControlLabels {
  const val PICTURE_IN_PICTURE = "Picture in picture"
  const val VIDEO_ASPECT_RATIO = "Video aspect ratio"

  const val PREVIOUS_FILE = "Previous file"
  const val NEXT_FILE = "Next file"
  const val SKIP_BACK_10 = "Skip back 10 seconds"
  const val SKIP_FORWARD_10 = "Skip forward 10 seconds"
  const val AUTO_ROTATE = "Auto rotate"
  const val LOCK_ROTATION = "Lock rotation"
  const val SHUFFLE = "Shuffle queue"

  const val QUICK_CONTROLS = "Quick controls"
  const val PLAY_PAUSE = "Play or pause"
  const val PLAYBACK_SPEED = "Playback speed"
  const val SUBTITLE_TRACKS = "Subtitle tracks"
  const val AUDIO_TRACKS = "Audio tracks"
  const val DECODER = "Decoder"
  const val MORE_OPTIONS = "More options"
  const val LOCK_CONTROLS = "Lock controls"

  val all: List<String> = listOf(
    PICTURE_IN_PICTURE,
    VIDEO_ASPECT_RATIO,
    PREVIOUS_FILE,
    NEXT_FILE,
    SKIP_BACK_10,
    SKIP_FORWARD_10,
    AUTO_ROTATE,
    LOCK_ROTATION,
    SHUFFLE,
    QUICK_CONTROLS,
    PLAY_PAUSE,
    PLAYBACK_SPEED,
    SUBTITLE_TRACKS,
    AUDIO_TRACKS,
    DECODER,
    MORE_OPTIONS,
    LOCK_CONTROLS,
  )
}
