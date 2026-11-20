package live.mehiz.mpvkt.ui.preferences

import androidx.annotation.StringRes
import live.mehiz.mpvkt.R

/**
 * The settings hub used to be one flat list of nine screens. It is now four
 * grouped sections (Playback / Interface / Gestures / Advanced) plus a search
 * entry that matches across every preference. The grouping and the search
 * matching live here as pure data + logic so tests can pin the contract
 * without a UI.
 */
enum class SettingsGroup(@StringRes val titleRes: Int) {
  Playback(R.string.pref_group_playback),
  Interface(R.string.pref_group_interface),
  Gestures(R.string.pref_gesture),
  Advanced(R.string.pref_advanced),
}

/** One navigable settings screen, assigned to exactly one group. */
data class SettingsDestination(
  val key: String,
  val group: SettingsGroup,
  @StringRes val titleRes: Int,
  @StringRes val summaryRes: Int,
)

object SettingsHub {
  val destinations: List<SettingsDestination> = listOf(
    // Playback — everything that changes how a file plays.
    SettingsDestination("player", SettingsGroup.Playback, R.string.pref_player, R.string.pref_player_summary),
    SettingsDestination("audio", SettingsGroup.Playback, R.string.pref_audio, R.string.pref_audio_summary),
    SettingsDestination("subtitles", SettingsGroup.Playback, R.string.pref_subtitles, R.string.pref_subtitles_summary),
    SettingsDestination("decoder", SettingsGroup.Playback, R.string.pref_decoder, R.string.pref_decoder_summary),
    // Interface — how the app looks and which buttons exist.
    SettingsDestination(
      "appearance",
      SettingsGroup.Interface,
      R.string.pref_appearance_title,
      R.string.pref_appearance_summary,
    ),
    SettingsDestination(
      "customButtons",
      SettingsGroup.Interface,
      R.string.pref_custom_buttons_title,
      R.string.pref_custom_buttons_summary,
    ),
    // Gestures — the touch language of the player.
    SettingsDestination("gesture", SettingsGroup.Gestures, R.string.pref_gesture, R.string.pref_gesture_summary),
    // Advanced — config files, logs, maintenance, about.
    SettingsDestination("advanced", SettingsGroup.Advanced, R.string.pref_advanced, R.string.pref_advanced_summary),
    SettingsDestination("about", SettingsGroup.Advanced, R.string.pref_about_title, R.string.pref_about_summary),
  )

  fun destinationFor(key: String): SettingsDestination = destinations.first { it.key == key }
}

/**
 * One searchable preference. [destinationKey] says which screen opens when the
 * result is tapped; the title/summary resources are resolved at render time.
 */
data class SettingsSearchItem(
  val destinationKey: String,
  @StringRes val titleRes: Int,
  @StringRes val summaryRes: Int? = null,
)

object SettingsSearchIndex {
  /** Every screen, then the individual preferences inside each screen. */
  val items: List<SettingsSearchItem> = SettingsHub.destinations.map {
    SettingsSearchItem(it.key, it.titleRes, it.summaryRes)
  } + listOf(
    // Appearance
    SettingsSearchItem("appearance", R.string.pref_appearance_category_theme),
    SettingsSearchItem("appearance", R.string.pref_appearance_darkmode_system),
    SettingsSearchItem("appearance", R.string.pref_appearance_darkmode_light),
    SettingsSearchItem("appearance", R.string.pref_appearance_darkmode_dark),
    SettingsSearchItem(
      "appearance",
      R.string.pref_appearance_material_you_title,
      R.string.pref_appearance_material_you_summary,
    ),
    // Player
    SettingsSearchItem("player", R.string.pref_player_orientation),
    SettingsSearchItem("player", R.string.pref_player_draw_over_cutout),
    SettingsSearchItem("player", R.string.pref_player_save_position_on_quit),
    SettingsSearchItem("player", R.string.pref_player_seeking_title),
    SettingsSearchItem("player", R.string.pref_player_show_seekbar_when_seeking),
    SettingsSearchItem(
      "player",
      R.string.pref_player_precise_seeking_title,
      R.string.pref_player_precise_seeking_summary,
    ),
    SettingsSearchItem("player", R.string.pref_player_double_tap_seek_duration),
    SettingsSearchItem("player", R.string.show_icon_on_double_tap_to_seek),
    SettingsSearchItem("player", R.string.show_splash_ovals_on_double_tap_to_seek),
    SettingsSearchItem("player", R.string.show_time_on_double_tap_to_seek),
    SettingsSearchItem("player", R.string.pref_player_automatically_enter_pip),
    SettingsSearchItem("player", R.string.pref_player_close_after_eof),
    SettingsSearchItem("player", R.string.pref_player_remember_brightness),
    SettingsSearchItem("player", R.string.pref_player_remember_volume),
    SettingsSearchItem("player", R.string.pref_player_gestures),
    SettingsSearchItem("player", R.string.pref_player_gestures_seek),
    SettingsSearchItem("player", R.string.pref_player_gestures_brightness),
    SettingsSearchItem("player", R.string.pref_player_gestures_volume),
    SettingsSearchItem("player", R.string.pref_player_gestures_hold_for_multiple_speed),
    SettingsSearchItem("player", R.string.swap_the_volume_and_brightness_slider),
    SettingsSearchItem("player", R.string.pref_player_controls),
    SettingsSearchItem("player", R.string.pref_player_controls_display_volume_as_percentage),
    SettingsSearchItem("player", R.string.pref_player_controls_show_chapter_indicator),
    SettingsSearchItem(
      "player",
      R.string.pref_player_controls_show_chapters_button,
      R.string.pref_player_controls_show_chapters_summary,
    ),
    SettingsSearchItem("player", R.string.pref_player_controls_show_loading_circle),
    SettingsSearchItem("player", R.string.pref_player_controls_allow_gestures_in_panels),
    SettingsSearchItem("player", R.string.pref_player_display),
    SettingsSearchItem("player", R.string.pref_player_display_hide_player_control_time),
    SettingsSearchItem(
      "player",
      R.string.pref_player_display_panel_opacity,
      R.string.pref_player_display_panel_transparency_summary,
    ),
    SettingsSearchItem("player", R.string.pref_player_display_reduce_player_animation),
    SettingsSearchItem("player", R.string.pref_player_display_show_status_bar),
    // Audio
    SettingsSearchItem("audio", R.string.pref_audio_channels),
    SettingsSearchItem(
      "audio",
      R.string.pref_audio_pitch_correction_title,
      R.string.pref_audio_pitch_correction_summary,
    ),
    SettingsSearchItem("audio", R.string.pref_audio_preferred_language),
    SettingsSearchItem("audio", R.string.pref_audio_volume_boost_cap),
    SettingsSearchItem("audio", R.string.pref_preferred_languages),
    // Subtitles
    SettingsSearchItem("subtitles", R.string.pref_subtitles_autoload_title, R.string.pref_subtitles_autoload_summary),
    SettingsSearchItem("subtitles", R.string.pref_subtitles_fonts_dir),
    SettingsSearchItem("subtitles", R.string.pref_subtitles_preferred_language),
    SettingsSearchItem("subtitles", R.string.pref_preferred_languages),
    // Decoder
    SettingsSearchItem("decoder", R.string.pref_decoder_try_hw_dec_title),
    SettingsSearchItem("decoder", R.string.pref_decoder_gpu_next_title, R.string.pref_decoder_gpu_next_summary),
    SettingsSearchItem("decoder", R.string.pref_decoder_yuv420p_title, R.string.pref_decoder_yuv420p_summary),
    SettingsSearchItem("decoder", R.string.pref_decoder_debanding_title),
    // Gestures
    SettingsSearchItem("gesture", R.string.pref_gesture_double_tap_title),
    SettingsSearchItem("gesture", R.string.pref_gesture_double_tap_left_title),
    SettingsSearchItem("gesture", R.string.pref_gesture_double_tap_center_title),
    SettingsSearchItem("gesture", R.string.pref_gesture_double_tap_right_title),
    SettingsSearchItem("gesture", R.string.pref_player_double_tap_seek_duration),
    SettingsSearchItem("gesture", R.string.pref_gesture_media_title),
    SettingsSearchItem("gesture", R.string.pref_gesture_media_previous),
    SettingsSearchItem("gesture", R.string.pref_gesture_media_play),
    SettingsSearchItem("gesture", R.string.pref_gesture_media_next),
    // Advanced
    SettingsSearchItem("advanced", R.string.pref_advanced_mpv_conf),
    SettingsSearchItem("advanced", R.string.pref_advanced_input_conf),
    SettingsSearchItem("advanced", R.string.pref_advanced_mpv_conf_storage_location),
    SettingsSearchItem(
      "advanced",
      R.string.pref_advanced_verbose_logging_title,
      R.string.pref_advanced_verbose_logging_summary,
    ),
    SettingsSearchItem("advanced", R.string.pref_advanced_dump_logs_title, R.string.pref_advanced_dump_logs_summary),
    SettingsSearchItem("advanced", R.string.pref_advanced_clear_playback_history),
    SettingsSearchItem("advanced", R.string.pref_advanced_clear_fonts_cache),
    SettingsSearchItem("advanced", R.string.pref_advanced_clear_mpv_conf_cache),
  )
}

/** Pure search matching over resolved (already-localised) preference text. */
object SettingsSearch {
  data class ResolvedItem(
    val destinationKey: String,
    val title: String,
    val summary: String,
  )

  private const val TITLE_WEIGHT = 2
  private const val SUMMARY_WEIGHT = 1

  /**
   * Every query token must appear in the title or the summary (AND, substring,
   * case-insensitive). Title hits rank above summary-only hits; ties keep the
   * index order. A blank query returns nothing — the hub shows the groups then.
   */
  fun search(query: String, items: List<ResolvedItem>): List<ResolvedItem> {
    val tokens = tokenize(query)
    if (tokens.isEmpty()) return emptyList()
    val scored = items.mapNotNull { item -> score(item, tokens)?.let { item to it } }
    return scored
      .sortedByDescending { it.second }
      .map { it.first }
      .distinctBy { it.destinationKey to it.title }
  }

  private fun tokenize(query: String): List<String> =
    query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

  private fun score(item: ResolvedItem, tokens: List<String>): Int? {
    val title = item.title.lowercase()
    val summary = item.summary.lowercase()
    var total = 0
    for (token in tokens) {
      val inTitle = title.contains(token)
      val inSummary = summary.contains(token)
      if (!inTitle && !inSummary) return null
      total += if (inTitle) TITLE_WEIGHT else SUMMARY_WEIGHT
    }
    return total
  }
}