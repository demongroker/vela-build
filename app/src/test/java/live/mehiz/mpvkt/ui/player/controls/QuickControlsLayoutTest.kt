package live.mehiz.mpvkt.ui.player.controls

import live.mehiz.mpvkt.ui.player.VideoAspect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickControlsLayoutTest {
  @Test
  fun everyTargetIsAboveTheAccessibilityFloor() {
    assertTrue(QuickControlsLayout.MIN_TARGET_DP >= 48)
  }

  @Test
  fun theTransportRowKeepsFiveFixedSlotsRegardlessOfQueue() {
    val slots = QuickControlsLayout.transportSlots()
    assertEquals(QuickControlsLayout.TRANSPORT_SLOT_COUNT, slots.size)
    assertEquals(5, slots.size)
    // Play/pause sits in the middle slot, so it never shifts under the thumb.
    assertEquals(QuickTransportSlot.PlayPause, slots[2])
    assertEquals(QuickTransportSlot.Previous, slots.first())
    assertEquals(QuickTransportSlot.Next, slots.last())
  }

  @Test
  fun missingQueueNeighboursDisableInPlaceInsteadOfDisappearing() {
    assertFalse(QuickControlsLayout.slotEnabled(QuickTransportSlot.Previous, hasPrevious = false, hasNext = true))
    assertFalse(QuickControlsLayout.slotEnabled(QuickTransportSlot.Next, hasPrevious = true, hasNext = false))
    assertTrue(QuickControlsLayout.slotEnabled(QuickTransportSlot.Previous, hasPrevious = true, hasNext = false))
    assertTrue(QuickControlsLayout.slotEnabled(QuickTransportSlot.Next, hasPrevious = false, hasNext = true))
    // The middle three always work, even with an empty queue.
    listOf(QuickTransportSlot.Back10, QuickTransportSlot.PlayPause, QuickTransportSlot.Forward10).forEach {
      assertTrue(QuickControlsLayout.slotEnabled(it, hasPrevious = false, hasNext = false))
    }
  }

  @Test
  fun theActionGridDropsAColumnOnNarrowScreens() {
    assertEquals(QuickControlsLayout.COLUMNS_NARROW, QuickControlsLayout.actionColumns(320))
    assertEquals(QuickControlsLayout.COLUMNS_NARROW, QuickControlsLayout.actionColumns(359))
    assertEquals(QuickControlsLayout.COLUMNS_WIDE, QuickControlsLayout.actionColumns(360))
    assertEquals(QuickControlsLayout.COLUMNS_WIDE, QuickControlsLayout.actionColumns(412))
  }

  @Test
  fun theSpeedIndicatorIsStableAndTabular() {
    assertEquals("1.00x", QuickControlsLayout.speedLabel(1f))
    assertEquals("0.25x", QuickControlsLayout.speedLabel(0.25f))
    assertEquals("2.50x", QuickControlsLayout.speedLabel(2.5f))
    // Two decimals always, so the chip width never jumps between 1x and 1.5x.
    assertTrue(QuickControlsLayout.speedLabel(1f).length == QuickControlsLayout.speedLabel(1.5f).length)
  }

  @Test
  fun theSeekStepMatchesTheExistingPlayerButtons() {
    assertEquals(10, QuickControlsLayout.SEEK_STEP_SECONDS)
  }

  @Test
  fun theAspectCycleIsUnchanged() {
    assertEquals(VideoAspect.Stretch, VideoAspect.Fit.next())
    assertEquals(VideoAspect.Crop, VideoAspect.Stretch.next())
    assertEquals(VideoAspect.Fit, VideoAspect.Crop.next())
    // Three steps return to the start.
    assertEquals(VideoAspect.Fit, VideoAspect.Fit.next().next().next())
  }
}