package live.mehiz.mpvkt.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
  private val items = listOf(
    SettingsSearch.ResolvedItem("player", "Precise seeking", "Seek to exact positions instead of keyframes (slow)"),
    SettingsSearch.ResolvedItem("audio", "Volume boost cap", "Maximum volume boost"),
    SettingsSearch.ResolvedItem("subtitles", "Autoload subtitles", "Load matching subtitle files automatically"),
    SettingsSearch.ResolvedItem("decoder", "Try hardware decoding", "Use the hardware decoder when possible"),
    SettingsSearch.ResolvedItem("advanced", "Verbose logging", "Write detailed mpv logs"),
  )

  @Test
  fun blankQueryShowsNoResultsSoTheGroupsStayVisible() {
    assertTrue(SettingsSearch.search("", items).isEmpty())
    assertTrue(SettingsSearch.search("   ", items).isEmpty())
  }

  @Test
  fun matchingIsCaseInsensitiveAndSubstringBased() {
    val results = SettingsSearch.search("PRECISE", items)
    assertEquals(listOf("player"), results.map { it.destinationKey })
    assertEquals(listOf("audio"), SettingsSearch.search("boost", items).map { it.destinationKey })
  }

  @Test
  fun everyTokenMustMatchAnd() {
    // "seek" + "hardware" share no single entry.
    assertTrue(SettingsSearch.search("seek hardware", items).isEmpty())
    // Both tokens hit the same entry across title and summary.
    val results = SettingsSearch.search("seeking keyframes", items)
    assertEquals(listOf("player"), results.map { it.destinationKey })
  }

  @Test
  fun titleHitsRankAboveSummaryOnlyHits() {
    val results = SettingsSearch.search("subtitle", items)
    // "Autoload subtitles" (title) must come before any summary-only match.
    assertEquals("subtitles", results.first().destinationKey)
  }

  @Test
  fun noMatchReturnsEmpty() {
    assertTrue(SettingsSearch.search("bluetooth", items).isEmpty())
  }

  @Test
  fun duplicateTitlesForTheSameScreenCollapse() {
    val duplicated = items + SettingsSearch.ResolvedItem("player", "Precise seeking", "Another summary")
    val results = SettingsSearch.search("precise", duplicated)
    assertEquals(1, results.size)
  }

  @Test
  fun tiesKeepIndexOrder() {
    val results = SettingsSearch.search("the", items)
    // "Try hardware decoding" and "Use the hardware decoder when possible" both
    // match; the title hit scores higher, summary-only entries keep index order.
    assertEquals("decoder", results.first().destinationKey)
  }
}