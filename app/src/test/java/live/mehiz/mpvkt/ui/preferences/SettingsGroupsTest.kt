package live.mehiz.mpvkt.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGroupsTest {
  @Test
  fun theHubHasExactlyFourGroups() {
    assertEquals(4, SettingsGroup.entries.size)
    assertEquals(
      listOf(SettingsGroup.Playback, SettingsGroup.Interface, SettingsGroup.Gestures, SettingsGroup.Advanced),
      SettingsGroup.entries.toList(),
    )
  }

  @Test
  fun everyFormerFlatScreenLandsInExactlyOneGroup() {
    val keys = SettingsHub.destinations.map { it.key }
    assertEquals(9, keys.size)
    assertEquals(keys.size, keys.toSet().size)
    assertEquals(
      setOf("appearance", "player", "gesture", "decoder", "subtitles", "audio", "customButtons", "advanced", "about"),
      keys.toSet(),
    )
  }

  @Test
  fun noGroupIsEmpty() {
    SettingsGroup.entries.forEach { group ->
      assertTrue(
        "group $group has no screens",
        SettingsHub.destinations.any { it.group == group },
      )
    }
  }

  @Test
  fun playbackGroupsThePlaybackScreens() {
    val playback = SettingsHub.destinations.filter { it.group == SettingsGroup.Playback }.map { it.key }
    assertEquals(setOf("player", "audio", "subtitles", "decoder"), playback.toSet())
  }

  @Test
  fun everySearchItemPointsAtAKnownScreen() {
    val keys = SettingsHub.destinations.map { it.key }.toSet()
    assertTrue(SettingsSearchIndex.items.all { it.destinationKey in keys })
  }

  @Test
  fun everyScreenIsSearchableByItsOwnTitle() {
    val titleResByDestination = SettingsSearchIndex.items
      .filter { it.summaryRes != null }
      .map { it.destinationKey to it.titleRes }
    SettingsHub.destinations.forEach { destination ->
      assertTrue(
        "${destination.key} is not in the search index",
        (destination.key to destination.titleRes) in titleResByDestination,
      )
    }
  }

  @Test
  fun theIndexHasNoDuplicateEntries() {
    val pairs = SettingsSearchIndex.items.map { it.destinationKey to it.titleRes }
    assertEquals(pairs.size, pairs.toSet().size)
  }

  @Test
  fun destinationLookupResolvesEveryKey() {
    SettingsHub.destinations.forEach {
      assertEquals(it, SettingsHub.destinationFor(it.key))
    }
  }
}