package live.mehiz.mpvkt.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class GhAsset(
  val name: String = "",
  @SerialName("browser_download_url") val url: String = "",
)

@Serializable
data class GhRelease(
  @SerialName("tag_name") val tag: String = "",
  val name: String? = null,
  val body: String? = null,
  @SerialName("published_at") val publishedAt: String? = null,
  val assets: List<GhAsset> = emptyList(),
  val prerelease: Boolean = false,
)

/**
 * Vela updater.
 *
 * Downloads are handed to Android's DownloadManager: it survives process death,
 * retries on connection loss and resumes partial files with HTTP Range requests,
 * so a dropped connection never loses the whole APK.
 */
@Suppress("TooManyFunctions")
object Updater {
  private const val API = "https://api.github.com/repos/demongroker/vela/releases"
  private val json = Json { ignoreUnknownKeys = true }

  private fun open(url: String, accept: String = "application/vnd.github+json"): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = 15_000
      readTimeout = 25_000
      setRequestProperty("Accept", accept)
      setRequestProperty("User-Agent", "Vela-Updater")
    }

  suspend fun latest(): GhRelease? = withContext(Dispatchers.IO) {
    runCatching {
      open("$API/latest").inputStream.bufferedReader().use {
        json.decodeFromString<GhRelease>(it.readText())
      }
    }.getOrNull()
  }

  suspend fun recent(limit: Int = 10): List<GhRelease> = withContext(Dispatchers.IO) {
    runCatching {
      open("$API?per_page=$limit").inputStream.bufferedReader().use {
        json.decodeFromString<List<GhRelease>>(it.readText())
      }
    }.getOrDefault(emptyList())
  }

  /** The CI appends `sha:<commit>` to the release body so a build can tell if it is current. */
  fun releaseSha(release: GhRelease): String? =
    Regex("sha:([0-9a-fA-F]{40})").find(release.body.orEmpty())?.groupValues?.get(1)

  fun apkAsset(release: GhRelease): GhAsset? {
    val name = UpdateLogic.pickApk(release.assets.map { it.name }, preferArm64()) ?: return null
    return release.assets.firstOrNull { it.name == name }
  }

  fun sha256Asset(release: GhRelease): GhAsset? {
    val name = UpdateLogic.pickSha(release.assets.map { it.name }, preferArm64()) ?: return null
    return release.assets.firstOrNull { it.name == name }
  }

  /** Outcome of checking a finished download before it may be installed. */
  enum class Verify { OK, TRUNCATED, HASH_MISMATCH, NO_HASH }

  /**
   * A download is only safe to hand to the installer when it is a plausible size
   * and its SHA-256 matches the hash published with the release. GitHub serves
   * assets from a CDN that happily hands out truncated files; installing one
   * fails with "package appears to be invalid".
   */
  suspend fun verify(file: File, release: GhRelease): Verify {
    val sizeOk = file.exists() && UpdateLogic.plausibleSize(file.length())
    val expected = if (sizeOk) expectedSha(release) else null
    val actual = if (expected != null) sha256Of(file) else null
    return when {
      !sizeOk -> Verify.TRUNCATED
      expected == null -> Verify.NO_HASH
      UpdateLogic.matches(expected, actual) -> Verify.OK
      else -> Verify.HASH_MISMATCH
    }
  }

  fun deleteDownload(context: Context, release: GhRelease) {
    runCatching { downloadedFile(context, release).delete() }
  }

  /** Starts (or resumes) the download. Returns the DownloadManager id. */
  /**
   * True when the current connection is unmetered (Wi-Fi/Ethernet/VPN), false on mobile
   * data. Default policy: a ~70 MB update does not start itself on mobile data.
   */
  fun isUnmetered(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && !cm.isActiveNetworkMetered
  }.getOrDefault(false)

  /** [allowMetered] is the user's explicit "use mobile data" tap, never a default. */
  fun startDownload(context: Context, release: GhRelease, allowMetered: Boolean = false): Long? {
    if (!allowMetered && !isUnmetered(context)) return null
    val asset = apkAsset(release) ?: return null
    val fileName = "vela-${release.tag}.apk"
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(Uri.parse(asset.url)).apply {
      setTitle("Vela ${release.tag}")
      setDescription("Downloading update")
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
      setAllowedOverMetered(true)
      setAllowedOverRoaming(false)
    }
    return runCatching { dm.enqueue(request) }.getOrNull()
  }

  fun downloadedFile(context: Context, release: GhRelease): File =
    File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "vela-${release.tag}.apk")

  suspend fun expectedSha(release: GhRelease): String? = withContext(Dispatchers.IO) {
    val asset = sha256Asset(release) ?: return@withContext null
    runCatching {
      open(asset.url, "application/octet-stream").inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()?.substringBefore(' ')?.trim()?.lowercase()
  }

  fun sha256Of(file: File): String? = runCatching {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
      val buffer = ByteArray(1 shl 16)
      while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        digest.update(buffer, 0, read)
      }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
  }.getOrNull()

  /**
   * Hand the downloaded APK to the system installer — but only if it is signed by the
   * same key as the app doing the installing.
   *
   * Fail closed: an APK whose signer does not overlap with the installed package's signer
   * is either a corrupted download or somebody else's build, and installing it would
   * either fail at package-manager level or replace the app with foreign code. Returns
   * false so the caller can say why instead of showing an installer that cannot work.
   */
  fun install(context: Context, file: File): Boolean {
    val usable = file.exists() && file.length() > 0L &&
      UpdateLogic.certsMatch(
        signerDigests(context, context.packageName),
        signerDigests(context, file.absolutePath),
      )
    if (!usable) return false
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { context.startActivity(intent) }.isSuccess
  }

  /**
   * SHA-256 digests of an APK's signing certificates, lowercase hex.
   *
   * [packageOrPath] is a package name (the installed app) or an absolute APK path (the
   * download). Empty list = "cannot tell", which [UpdateLogic.certsMatch] treats as a
   * refusal.
   */
  @Suppress("DEPRECATION")
  private fun signerDigests(context: Context, packageOrPath: String): List<String> = runCatching {
    val pm = context.packageManager
    val info = if (File(packageOrPath).isAbsolute) {
      pm.getPackageArchiveInfo(packageOrPath, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
      pm.getPackageInfo(packageOrPath, PackageManager.GET_SIGNING_CERTIFICATES)
    } ?: return emptyList()
    val signers = info.signingInfo?.apkContentsSigners ?: return emptyList()
    signers.map { certDigest(it.toByteArray()) }
  }.getOrDefault(emptyList())

  /**
   * SHA-256 of a DER certificate, in the same normalised form [UpdateLogic.certsMatch]
   * compares. Public for tests: this is the only place the digest format is decided.
   */
  fun certDigest(der: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(der).joinToString("") { "%02x".format(it) }
}

/** True when this device can install the small arm64-only build. */
private fun preferArm64(): Boolean = Build.SUPPORTED_ABIS.contains("arm64-v8a")
