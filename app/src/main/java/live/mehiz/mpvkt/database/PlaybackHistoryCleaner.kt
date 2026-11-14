package live.mehiz.mpvkt.database

import live.mehiz.mpvkt.database.dao.PlaybackStateDao
import live.mehiz.mpvkt.database.dao.ResumeStateDao

/**
 * "Clear playback history" in Advanced preferences.
 *
 * History lives in two places: the legacy `PlaybackStateEntity` table (per-title position)
 * and the `resume_state` table (per-file position + last played). Clearing one and not the
 * other is why the button used to look like it did nothing — the screen kept re-reading the
 * rows it never deleted.
 *
 * Both DAOs are injected rather than taken from the database object so the wiring can be
 * tested with fakes: the interesting bug here was that no delete call happened at all.
 */
object PlaybackHistoryCleaner {

  suspend fun clearAll(playbackStateDao: PlaybackStateDao, resumeStateDao: ResumeStateDao) {
    playbackStateDao.clearAllPlaybackStates()
    resumeStateDao.clearAll()
  }
}
