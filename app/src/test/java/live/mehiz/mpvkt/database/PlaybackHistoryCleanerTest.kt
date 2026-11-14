package live.mehiz.mpvkt.database

import kotlinx.coroutines.runBlocking
import live.mehiz.mpvkt.database.dao.PlaybackStateDao
import live.mehiz.mpvkt.database.dao.ResumeStateDao
import live.mehiz.mpvkt.database.entities.PlaybackStateEntity
import live.mehiz.mpvkt.database.entities.ResumeStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Clear playback history" must clear BOTH stores.
 *
 * The bug this locks down: the preference's onConfirm launched a coroutine that only fetched
 * the DAO — no delete ever ran, so the toast said "cleared" while every row stayed. Fakes are
 * enough here because the failure was a missing call, not a broken SQL statement.
 */
class PlaybackHistoryCleanerTest {

  private class FakePlaybackStateDao : PlaybackStateDao {
    var cleared = 0
    private val rows = mutableListOf<PlaybackStateEntity>()

    override suspend fun upsert(playbackStateEntity: PlaybackStateEntity) {
      rows += playbackStateEntity
    }

    override suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity? =
      rows.firstOrNull { it.mediaTitle == mediaTitle }

    override suspend fun clearAllPlaybackStates() {
      cleared++
      rows.clear()
    }

    fun count() = rows.size
  }

  private class FakeResumeStateDao : ResumeStateDao {
    var cleared = 0
    private val rows = mutableListOf<ResumeStateEntity>()

    override suspend fun upsert(state: ResumeStateEntity) {
      rows += state
    }

    override suspend fun get(fileKey: String): ResumeStateEntity? =
      rows.firstOrNull { it.fileKey == fileKey }

    override suspend fun getAll(): List<ResumeStateEntity> = rows.toList()

    override suspend fun recentlyPlayed(since: Long, limit: Int): List<ResumeStateEntity> =
      rows.filter { it.lastPlayedAt > since }.sortedByDescending { it.lastPlayedAt }.take(limit)

    override suspend fun delete(fileKey: String) {
      rows.removeAll { it.fileKey == fileKey }
    }

    override suspend fun clearAll() {
      cleared++
      rows.clear()
    }

    fun count() = rows.size
  }

  @Test
  fun clearsBothStores() = runBlocking {
    val playback = FakePlaybackStateDao()
    val resume = FakeResumeStateDao()

    PlaybackHistoryCleaner.clearAll(playback, resume)

    assertEquals("playback history must be deleted, not just fetched", 1, playback.cleared)
    assertEquals("resume history must be deleted too", 1, resume.cleared)
    assertEquals(0, playback.count())
    assertEquals(0, resume.count())
  }

  @Test
  fun aSecondClearIsHarmless() = runBlocking {
    val playback = FakePlaybackStateDao()
    val resume = FakeResumeStateDao()

    PlaybackHistoryCleaner.clearAll(playback, resume)
    PlaybackHistoryCleaner.clearAll(playback, resume)

    assertEquals(2, playback.cleared)
    assertEquals(2, resume.cleared)
    assertFalse(playback.count() > 0)
    assertTrue(resume.count() == 0)
  }
}
