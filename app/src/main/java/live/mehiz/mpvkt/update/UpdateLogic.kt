package live.mehiz.mpvkt.update

/**
 * Pure update decisions — no Android, no network, so they can be unit tested.
 * Kept out of [Updater] on purpose: a wrong answer here means the phone either
 * misses an update or installs a truncated APK.
 */
@Suppress("TooManyFunctions")  // small, pure, single-purpose helpers
object UpdateLogic {

  /** A version parsed out of a release tag, a versionName or a git tag. */
  data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
    /** The `-vela<N>` build number, or 0 when the string carries none. */
    val build: Int,
  )

  private val VERSION = Regex("""^[vV]?(\d+)\.(\d+)\.(\d+)(?:\D*?vela(\d+))?.*$""", RegexOption.IGNORE_CASE)

  /**
   * "v0.1.10-vela10" -> 0.1.10 build 10
   * "0.1.11"         -> 0.1.11 build 0
   * Returns null for anything that is not a version — a tag we cannot read must
   * never be treated as an update.
   */
  fun parseVersion(raw: String?): VersionNumber? {
    val match = VERSION.find(raw?.trim().orEmpty()) ?: return null
    val major = match.groupValues[1].toIntOrNull() ?: return null
    val minor = match.groupValues[2].toIntOrNull() ?: return null
    val patch = match.groupValues[3].toIntOrNull() ?: return null
    val build = match.groupValues[4].toIntOrNull() ?: 0
    return VersionNumber(major, minor, patch, build)
  }

  /** Numeric comparison of major.minor.patch only; negative when [a] is older. */
  fun compareSemver(a: VersionNumber, b: VersionNumber): Int {
    if (a.major != b.major) return a.major - b.major
    if (a.minor != b.minor) return a.minor - b.minor
    return a.patch - b.patch
  }

  /**
   * Should the update banner be shown?
   *
   * Compares the release tag with what is installed, numerically: first
   * major.minor.patch, then (for the same version) the `-vela<N>` build number
   * against the installed tag's build, falling back to the installed versionCode.
   * Equal or older never shows a banner — the old code compared the tag to the
   * build's git sha, which kept offering an update to the version already
   * installed.
   */
  fun isUpdateAvailable(
    remoteTag: String?,
    installedVersionName: String?,
    installedTag: String?,
    installedVersionCode: Int = 0,
  ): Boolean {
    val remote = parseVersion(remoteTag) ?: return false
    val installed = parseVersion(installedVersionName) ?: return false
    val bySemver = compareSemver(remote, installed)
    if (bySemver != 0) return bySemver > 0
    val installedBuild = parseVersion(installedTag)?.build?.takeIf { it > 0 } ?: installedVersionCode
    return remote.build > installedBuild
  }

  /** "v0.1.10-vela10" -> [0, 1, 10] ; ignores the leading "v" and any "-suffix". */
  fun versionFromTag(tag: String): List<Int> =
    tag.trim()
      .removePrefix("v")
      .substringBefore('-')
      .split('.')
      .mapNotNull { it.trim().toIntOrNull() }

  /** True when [releaseTag] is a strictly newer version than [currentVersionName]. */
  fun isNewer(releaseTag: String, currentVersionName: String): Boolean {
    val remote = parseVersion(releaseTag)
    val installed = parseVersion(currentVersionName)
    if (remote != null && installed != null) return compareSemver(remote, installed) > 0
    val a = versionFromTag(releaseTag)
    val b = versionFromTag(currentVersionName)
    val firstDifference = (0 until maxOf(a.size, b.size))
      .firstOrNull { a.getOrElse(it) { 0 } != b.getOrElse(it) { 0 } }
    return if (a.isEmpty() || firstDifference == null) {
      false
    } else {
      a.getOrElse(firstDifference) { 0 } > b.getOrElse(firstDifference) { 0 }
    }
  }

  private fun isSha(name: String): Boolean = name.endsWith(".apk.sha256")

  private fun isArm64(name: String): Boolean = !isSha(name) &&
    (name.endsWith("-arm64.apk") || name.endsWith("arm64-v8a-release.apk"))

  /**
   * Pick the APK to download.
   *
   * [preferArm64] = true  -> the small arm64-only build when the release has one.
   * [preferArm64] = false -> the UNIVERSAL build (`vela-<ver>.apk` / `app-release.apk`),
   *                          never an -arm64 asset. The old code fell through to the arm64
   *                          asset when no universal name matched, which installed an
   *                          ABI-specific APK on devices that never asked for one.
   */
  fun pickApk(names: List<String>, preferArm64: Boolean = false): String? {
    val apks = names.filter { it.endsWith(".apk") && !isSha(it) }
    if (preferArm64) {
      apks.firstOrNull(::isArm64)?.let { return it }
    }
    return apks.firstOrNull { !isArm64(it) }
      ?: apks.firstOrNull(::isArm64) // nothing but arm64 published: better than nothing
  }

  /**
   * The checksum that belongs to the APK picked for this device. Never returns an
   * arm64 checksum when the chosen APK is universal (and vice versa) — a mismatched
   * pair would fail the hash check on a perfectly good download.
   */
  fun pickSha(names: List<String>, preferArm64: Boolean = false): String? {
    val apk = pickApk(names, preferArm64)
    val paired = apk?.let { chosen -> names.firstOrNull { it == "$chosen.sha256" } }
    // No matching APK name (older releases published checksums only): honour the same
    // arm64/universal preference rather than always assuming universal.
    return paired
      ?: names.firstOrNull { it.endsWith(".apk.sha256") && (it.contains("arm64") == preferArm64) }
      ?: names.firstOrNull { it.endsWith(".apk.sha256") }
  }

  /**
   * Fail-closed signing-certificate check.
   *
   * [pinned] is the cert SHA-256 published for the release (or read from the installed
   * APK); [candidate] is what the downloaded APK actually carries. Anything empty, and
   * any pair with no certificate in common, is a refusal: an updater that installs an APK
   * signed by somebody else is worse than one that installs nothing.
   */
  fun certsMatch(pinned: List<String>, candidate: List<String>): Boolean {
    val want = pinned.map(::normaliseCert).filter { it.isNotEmpty() }.toSet()
    val have = candidate.map(::normaliseCert).filter { it.isNotEmpty() }.toSet()
    if (want.isEmpty() || have.isEmpty()) return false
    return want.any { it in have }
  }

  /** Cert digests are compared as lowercase hex without separators or labels. */
  private fun normaliseCert(raw: String): String =
    raw.trim().lowercase().replace(":", "").replace(" ", "").removePrefix("sha-256:").removePrefix("sha256:")

  /** A download may only be installed when its hash matches the published one. */
  fun matches(expectedSha: String?, actualSha: String?): Boolean {
    val e = expectedSha?.trim()?.lowercase()?.substringBefore(' ')
    val a = actualSha?.trim()?.lowercase()
    return !e.isNullOrEmpty() && !a.isNullOrEmpty() && e == a
  }

  /** Anything under this is a truncated download, whatever the hash says. */
  fun plausibleSize(bytes: Long): Boolean = bytes > 10L * 1024 * 1024
}
