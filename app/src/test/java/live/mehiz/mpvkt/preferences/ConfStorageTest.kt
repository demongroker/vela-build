package live.mehiz.mpvkt.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * mpv.conf and input.conf each have their own storage location.
 *
 * The bug: every input.conf read/write used `mpvConfStorageLocation`, so picking a folder for
 * input.conf did nothing and the file landed in the mpv.conf tree.
 */
class ConfStorageTest {

  private val mpv = "content://tree/mpv-folder"
  private val input = "content://tree/input-folder"

  @Test
  fun eachConfFileUsesItsOwnTree() {
    assertEquals(mpv, ConfStorage.treeUriFor(mpvConfUri = mpv, inputConfUri = input, isInputConf = false))
    assertEquals(input, ConfStorage.treeUriFor(mpvConfUri = mpv, inputConfUri = input, isInputConf = true))
  }

  @Test
  fun inputConfNeverFallsBackToTheMpvTree() {
    // Blank input location means "filesDir copy only", NOT "use the mpv folder".
    assertEquals("", ConfStorage.treeUriFor(mpvConfUri = mpv, inputConfUri = "", isInputConf = true))
    assertEquals("", ConfStorage.treeUriFor(mpvConfUri = mpv, inputConfUri = null, isInputConf = true))
  }

  @Test
  fun nullAndWhitespaceAreTreatedAsUnset() {
    assertEquals("", ConfStorage.treeUriFor(null, null, isInputConf = false))
    assertEquals("", ConfStorage.treeUriFor("   ", input, isInputConf = false))
    assertEquals(input, ConfStorage.treeUriFor("   ", "  $input  ", isInputConf = true))
  }
}
