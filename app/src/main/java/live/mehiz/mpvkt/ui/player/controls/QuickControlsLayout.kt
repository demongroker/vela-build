package live.mehiz.mpvkt.ui.player.controls

import java.util.Locale

/**
 * Layout contract for the one-thumb Quick controls sheet.
 *
 * The transport row keeps FIVE fixed slots (previous / back 10 / play-pause /
 * forward 10 / next) even when the queue has no previous or next file: the
 * missing buttons render disabled in place, so the play-pause button never
 * shifts under the thumb when the queue changes. Every target is at least
 * [MIN_TARGET_DP] — above the 48 dp accessibility floor — and the action grid
 * drops from five to four columns on narrow screens so labels keep fitting.
 */
enum class QuickTransportSlot {
  Previous,
  Back10,
  PlayPause,
  Forward10,
  Next,
}

object QuickControlsLayout {
  /** Touch targets never shrink below this (accessibility floor is 48 dp). */
  const val MIN_TARGET_DP = 56

  /** The transport row always reserves all five slots, queue or no queue. */
  const val TRANSPORT_SLOT_COUNT = 5

  /** The ± seek step used by the transport row, in seconds. */
  const val SEEK_STEP_SECONDS = 10

  /** Below this sheet width the action grid uses four columns, not five. */
  const val NARROW_WIDTH_DP = 360
  const val COLUMNS_NARROW = 4
  const val COLUMNS_WIDE = 5

  fun actionColumns(widthDp: Int): Int =
    if (widthDp < NARROW_WIDTH_DP) COLUMNS_NARROW else COLUMNS_WIDE

  fun transportSlots(): List<QuickTransportSlot> = QuickTransportSlot.entries

  /** Queue-dependent slots stay in place but disable when there is nothing to play. */
  fun slotEnabled(slot: QuickTransportSlot, hasPrevious: Boolean, hasNext: Boolean): Boolean =
    when (slot) {
      QuickTransportSlot.Previous -> hasPrevious
      QuickTransportSlot.Next -> hasNext
      else -> true
    }

  /** Persistent speed indicator, always two decimals and a stable locale. */
  fun speedLabel(speed: Float): String = String.format(Locale.US, "%.2fx", speed)
}