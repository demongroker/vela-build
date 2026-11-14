package live.mehiz.mpvkt.preferences

/**
 * Which storage location a config file belongs to.
 *
 * Both config editors used `mpvConfStorageLocation`, so a custom folder picked for input.conf
 * was ignored and input.conf was written into the mpv.conf tree instead (and the reverse
 * confusion showed up in the save code as a copy-paste of the mpv block).
 *
 * Kept pure so the mapping is testable without an Activity, a SAF picker or Android at all.
 */
object ConfStorage {

  /** A blank result means "keep the copy in filesDir and do not write to external storage". */
  fun treeUriFor(mpvConfUri: String?, inputConfUri: String?, isInputConf: Boolean): String {
    val chosen = if (isInputConf) inputConfUri else mpvConfUri
    return chosen?.trim().orEmpty()
  }
}
