package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.theme.DarkMode

class AppearancePreferences(preferenceStore: PreferenceStore) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  // Default OFF so the app shows its own navy + gold identity (shared with Hermes and
  // VideoDroid) instead of the wallpaper-derived Material You palette. Still user-toggleable
  // in Appearance; existing users who set it keep their choice.
  val materialYou = preferenceStore.getBoolean("material_you", false)
}
