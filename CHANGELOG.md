# Vela changelog

Newest first. Each entry describes one reviewable step.

## 0.1.30

### Changed — dark cards read as panels

- **Card surface `#1B2740` → `#22314F`** (`surfaceVariantDark`, `surfaceContainerHighDark`) and
  every card now carries a **1 dp `#2A3A57` edge** (`outlineDark`, exposed as
  `MaterialTheme`-level `cardBorder`). The fill step alone was 1.26:1 against `#0B1220`;
  at 1.44:1 plus a hairline the library, changelog and player panels no longer merge into one
  dark mass on an AMOLED panel.
- **Danger red `#F85149` → `#FF7469`.** The lighter card dropped refusal text to 3.87:1; it is
  now 4.91:1. Measured, not assumed: body 11.00:1, muted 6.33:1, gold 6.16:1, success 5.10:1,
  warning 5.13:1 on the new card.
- Light mode unchanged: `primaryLight` stays the darkened gold `#7A5F14` (6.04:1 on white).

## 0.1.29

### Fixed — launch crash on 0.1.28

- **`NoSuchMethodError: NavigationEventHandler` on first composition.** `navigation3-ui
  1.0.0-alpha06` is compiled against `navigationevent-compose 1.0.0-alpha05`, but material3
  1.4.0 pulled a newer navigationevent transitively, which no longer has that overload.
  `NavDisplay` called it and the app died before the library appeared. The matching version is
  now pinned in the catalogue **and forced** in `resolutionStrategy`, so no transitive upgrade
  can win again.
- **Crash reporter could not share its own log.** `CrashActivity` writes
  `cacheDir/mpvKt_logs.txt`, but `provider_paths.xml` only exposed `files/crash.log`, so
  `FileProvider.getUriForFile` threw "Failed to find configured root". Added a narrow
  `cache-path` entry for that single file — the provider still exposes nothing else.

## 0.1.28

### Settings regroup + one-thumb player sheet

- **Settings hub: 9 flat entries → 4 grouped sections.** Playback (Player, Audio,
  Subtitles, Decoder), Interface (Appearance, Custom buttons), Gestures, Advanced
  (Advanced, About). Same screens, same keys, same behaviour — only the information
  architecture changed (`SettingsGroups.kt`).
- **Search across every preference.** A search entry at the top of the hub matches
  titles and summaries of ~70 preferences across all screens (case-insensitive,
  every word must match, title hits rank first). Tapping a result opens the screen
  that contains it. Pure matching logic is unit-tested (`SettingsSearchTest`,
  `SettingsGroupsTest`).
- **One-thumb Quick controls sheet.** New bottom-left player button opens a sheet
  with the daily controls in thumb reach: a fixed five-slot transport row
  (previous / −10 s / play-pause / +10 s / next — missing queue neighbours disable
  in place so play/pause never shifts), a grid of ≥56 dp labelled targets (speed,
  aspect, subtitles, audio, decoder, more, lock), and persistent speed/aspect/
  decoder indicator chips that double as shortcuts. Layout contract unit-tested
  (`QuickControlsLayoutTest`); every icon button carries a TalkBack label.
- The aspect cycle (Fit → Stretch → Crop) is now one shared `VideoAspect.next()`
  used by both the bottom-right button and the sheet — behaviour unchanged.

## 0.1.27

### Navy + gold — the shared house style

- **Palette ported** to the same navy + gold tokens as Hermes 0.1.4 and VideoDroid 2.6.0
  (`ui/theme/Color.kt`, all 70 tokens; token names unchanged so `Theme.kt` needed no edit).
  Dark: navy `#0B1220` base, `#111A2B` surface, `#1B2740` cards, gold `#D4AF37` accent.
  Light: white surfaces with a **darkened gold** `#7A5F14` primary — full gold fails contrast
  on white, so it is never used for text or icons there.
- **Material You now defaults OFF.** It was on by default for Android 12+, which replaced the
  app's colours with the wallpaper palette — so the new identity would not have been visible.
  The toggle in Appearance still works, and anyone who already set it keeps their choice.

### Not in this release

The settings regroup (7 flat screens → 4 groups) and the one-thumb player sheet are the next
slice; this release is the palette only.

## Unreleased

Nothing yet.

### 0.1.26 — toolchain refresh: Kotlin 2.4.21, minSdk 23
- **build: minSdk 21 → 23.** androidx media 1.8.0 and koin 4.2.2 require API 23; all
  owner devices are far above it. Drops Android 5.x support.
- **build: jvmTarget migrated to the `kotlin.compilerOptions` DSL** — the old
  `android.kotlinOptions` String form is an error under Kotlin 2.4.
- **deps: Kotlin 2.2.20 → 2.4.21** (Compose compiler plugin follows the Kotlin version),
  koin 4.1.1 → 4.2.2, androidx media 1.7.1 → 1.8.0, kotlinx-serialization 1.9.0 → 1.13.0,
  compose-preference 2.1.0 → 2.2.0.
- **ci: temurin distribution** (setup-java v5 removed `adopt`), checkout 7,
  upload-artifact 7, setup-java 6; dependabot ignores AGP majors and navigation3 alphas
  (both need deliberate migrations).
- No behaviour changes intended; this release ships the audit fixes and merged bumps.

### 0.1.25 — update-integrity and background-playback fixes
- **security(updater): a downloaded APK is only installed when its signing certificate
  overlaps the installed app's.** `Updater.install()` now reads both signer sets
  (`GET_SIGNING_CERTIFICATES`) and refuses on mismatch, on an empty signer set, and on a
  zero-length file; the UI says why instead of opening an installer that cannot work.
  `UpdateLogic.certsMatch()` is the pure, tested comparison; no certificate is hard-coded.
- **fix(preferences): "Clear playback history" now clears it.** It used to launch a coroutine
  that only fetched the DAO — no delete ran, so the rows came back and the toast lied.
  `PlaybackHistoryCleaner` clears both `PlaybackStateEntity` and `resume_state` on IO, then
  reports on the main thread.
- **fix(playback): real foreground service.** Playback only called `notify()`, so Android
  could kill the process in the background. It now calls `startForeground(...)` with
  `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` while playing (plain notification when paused),
  and the manifest declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and
  `POST_NOTIFICATIONS`; the player asks for notification permission once on Android 13+.
- **fix(updater): the universal APK is the default.** `pickApk`/`pickSha` no longer fall back
  to an `-arm64` asset for a device that did not ask for one; the checksum always pairs with
  the chosen APK.
- **security(backup): the library lock and secure-secrets prefs are excluded** from cloud
  backup and device-transfer (`lock_pin_hash`, `lock_pin_salt`, `lock_locked_folders`,
  `vela_secure_secrets`).
- **privacy(library): lock logs carry counts only** — folder names are no longer written to
  logcat when a folder is locked or unlocked.
- **security(files): `provider_paths.xml` narrowed** to the update-APK directory and the
  crash-log file instead of every cache/files/external path.
- **fix(player): crash-proofed intent and config handling** — null-safe `EXTRA_STREAM` /
  `EXTRA_TEXT` and SAF tree lookups in the mpv.conf / input.conf save paths.
- **Android 13+:** OpenSubtitles `User-Agent` now reports the real version from
  `BuildConfig.VERSION_NAME`.
- **Updates no longer start on mobile data by default:** the first tap on a metered
  connection explains the cost, the second tap is the explicit consent.

### 0.1.24 — Next/Previous advance; subtitle search is relevant
- **fix(player): manual Next/Previous no longer gets swallowed by auto-play.** With
  "Play the next file automatically" on (the default), tapping Next called `playFile`,
  which made mpv emit `END_FILE` for the file it was interrupting. The auto-advance
  handler read that as "the file never opened" and skipped straight past the file the
  owner had just chosen — so Next appeared to do nothing (or jump two files). An
  `switchingInPlace` flag now suppresses auto-advance for the switch's own `END_FILE`.
- **fix(player): the Next/Previous buttons now refresh as the queue changes.** Their
  visibility was computed once at the first render and never re-evaluated, because
  `queue` and `resumeFileKey` are plain fields rather than Compose state. After
  advancing, deleting the current file, or toggling shuffle, the buttons could stay
  hidden (or point at a stale position). A `queueVersion` counter now bumps on every
  queue or current-file change and the controls re-read it.
- **feat(subtitles): rank hits by how well they match the query.** `rank()` now scores
  each hit by token overlap with the cleaned query before falling back to
  format→rating→downloads, so a titled search surfaces the actual film instead of a
  more-downloaded fuzzy match (the "One Piece 190" problem). Numeric-only queries are
  non-discriminating and fall back to popularity.
- **fix(subtitles): "no results" is no longer reported as a provider failure.** The
  sheet now says "No subtitles found for <title>" when the provider answered fine but
  had nothing, instead of "Couldn't reach the subtitle provider".



### 0.1.23 — half-size download on arm64 phones
- **build: publish an arm64-only APK beside the universal one.** The release now ships
  `vela-<version>.apk` (universal, for any device) and `vela-<version>-arm64.apk` (arm64 only,
  roughly half the bytes). Both come from the same source and carry the **same versionCode**,
  so a device can move between them without the split-upgrade trap that kept per-ABI splits
  disabled.
- **feat(update): the updater prefers the small build on arm64 devices** and takes the
  checksum that belongs to the APK it actually downloaded, so a mismatched pair can never be
  verified against the wrong hash.

### 0.1.22 — a forgotten PIN is no longer a dead end
- **feat(lock): "Forgot PIN" escape hatch.** The PIN prompt now offers Forgot PIN, which
  explains exactly what will happen before it happens: the PIN is cleared and every locked
  folder becomes visible again. It cannot be undone without setting a new PIN, which is why
  it asks first and says so plainly.
- Before this, a forgotten PIN left folders hidden until app data was cleared by hand.

### 0.1.21 — see and edit exactly which folders are locked
- **feat(library): Manage locked folders.** The + menu now lists every locked folder by
  name (behind the PIN) with a Remove button per row, so a folder can be made visible again
  without unlocking the rest. Before this, the only control was all-or-nothing: the count in
  the menu told you how many were locked but not which, and nothing could be un-locked
  individually.
- Locking one folder still locks **only that folder** — long-press a folder row → "Lock
  folder (PIN)". The count accumulates across everything ever locked, which is what the
  manager now makes visible.

### 0.1.20 — unlocking is repeatable
- **fix(library): unlocking a second time did nothing.** The + menu had one entry whose job
  changed depending on state ("Locked folders" vs "Lock them again"), so after a re-lock the
  tap landed on the wrong action. It is now two explicit entries: "Unlock locked folders (n)"
  always opens the PIN dialog, and "Lock them again (n)" only appears while a folder is open.
- **fix(library): the exclusion set is refreshed by the action itself**, not only by watching
  state, so the library repaints even when the unlock state happens to look unchanged.
- Diagnostics: every lock/unlock step logs under `VelaLock` (menu taps, PIN result, unlock
  count, refreshed exclusion count) for `adb logcat -s VelaLock` if anything still misbehaves.

### 0.1.19 — unlocks actually come back
- **fix(library): an unlocked folder stayed hidden.** The screen cached the exclusion set
  once at load, so unlocking only changed half the picture: the cached half kept hiding the
  folder. The exclusion source is now re-asked whenever a folder is unlocked or locked, so
  the library repaints immediately after the PIN is accepted.
- **fix(privacy): locked files no longer appeared in Continue watching.** The strip was built
  from the raw video list instead of the exclusion-filtered one, so a half-watched file inside
  a locked folder showed up at the top of the library. It now uses the filtered list.

### 0.1.18 — locked folders
- **feat(library): lock a folder behind a PIN.** Long-press a folder → "Lock folder (PIN)".
  Locked folders leave the library entirely — they are filtered at the same single
  exclusion point hidden folders already use, so search, Continue watching, queues and
  the folder list all lose them together. Unlock from the + menu ("Locked folders (n)"),
  and they re-lock themselves after the timeout (default 5 min) or immediately via
  "Lock them again".
- **The PIN is never stored.** Only a salted PBKDF2 hash is written (PBKDF2WithHmacSHA256,
  with the SHA1 PRF as the fallback for API 21–25), verified in constant time; the PIN
  itself is asked twice when created and never logged. Nothing about an unlock survives
  the app closing.
- Lock state is session-only: locked automatically on restart, which is the point.

### 0.1.17 — continue watching, remembered volume
- **feat(library): "Continue watching" strip** at the top of the library. Files that were
  started and not finished appear once, most recently played first, above the folders;
  tapping one opens it with the strip as the queue, so Next walks what you were watching.
  Finished files (95 %+) and barely-started ones (under 10 s) stay out. Hidden while a
  search is active.
- **feat(player): the volume level is remembered between sessions.** Saved when the player
  pauses or leaves, applied once per session on start, clamped to mpv's legal range. New
  switch under Preferences → Player → "Remember volume between sessions" (default on);
  turning it off leaves mpv's own default alone instead of forcing a value.
- build: `.kotlin/` is ignored.

### 0.1.16 — the queue stays in the player
- **fix(next): Next and Previous no longer drop you back to the library.** They used
  to start a second PlayerActivity and finish the current one. The file is now
  swapped inside the running session: the player stays on screen, the new file starts
  from the top (its own saved position is applied), the queue and the shuffle state
  are kept, and resume/delete/rotation all follow the new file.
- **fix(next): a file that will not open no longer freezes the player.** If no
  `FILE_LOADED` arrives within 4 s the item is marked failed, a short message is
  shown, and the following item is tried — up to the existing skip cap. The session
  is never finished.
- **fix(play/pause): the centre icon follows mpv, not the last tap.** It is driven by
  mpv's `pause` property (Pause while playing, Play while paused), refreshed on every
  property change, on file load and on return from background, so gesture pauses and
  system media buttons are reflected too. The glyph only flips once mpv confirms.
- Diagnostics: every Next/Previous tap logs `VelaQueue … current=<key> target=<key>
  activityFinished=false queue=<n>`, failures log `VelaQueue open failed: …`, and the
  pause state is logged under `VelaPause` on load and on resume.

### 0.1.15 — player UI fixes
- **fix(more sheet): labels are no longer squeezed.** The title has its own line and
  the actions sit in a wrapping row, each with a minimum width, wrapping at word
  boundaries over at most two lines — so "Delete file" can no longer render one
  letter per line on a narrow portrait screen at large font scale.
- **feat(player): Previous file button** in the centre row, shown only when the
  queue has an item before the current one, mirroring Next (which moved from the
  top-right row into the centre row so the pair is symmetric). Double-tap seek
  (10 s back / forward) is untouched.
- **feat(player): optional skip buttons.** Preferences → Player gains "Show skip
  ±10 s buttons" (default off), which puts back/forward 10 s beside Play, plus the
  missing "Play the next file automatically" and "Loop the folder" switches.
- **fix(a11y): the Picture-in-picture and Video aspect ratio buttons now announce
  themselves** to TalkBack; behaviour and position are unchanged.

### 0.1.14 — playback fixes
- **fix(rotate): the button now actually changes orientation.** Auto mode asks for
  `SCREEN_ORIENTATION_SENSOR` (sensor-driven, follows the screen); Locked mode sets
  the current orientation explicitly. The value the activity keeps is logged after
  every change, and the state is read from preferences whenever the player starts.
  When Auto is on but the system's own auto-rotate is off, the player says
  "Turn on auto-rotate in Quick Settings to follow the screen" instead of doing
  nothing silently.
- **fix(next): the next file plays.** `MPV_EVENT_END_FILE` is logged with its
  reason (EOF / STOP / ERROR) and the queue position. On EOF the next item is
  found by key, then by uri, path or name, so a resolved descriptor no longer
  breaks advancement; without a queue the folder is looked up and rebuilt from the
  library. A file that fails to open is skipped (up to three) instead of freezing
  on the last frame, and a URL with no folder simply stops.
- **fix(overlay): the brightness/volume slider hides 1.5 s after the last
  gesture** (it waited 2 s before and could stick), and tapping anywhere on the
  video dismisses it immediately.

### 0.1.13 — playback controls
- **feat(delete): delete a file for good, from the library or from playback.** The
  in-app confirmation ("Delete <name> permanently?", red button) comes first, then
  `MediaStore.createDeleteRequest` makes the system ask too. Only a RESULT_OK from
  the system dialog removes the file's resume row, its library row and its queue
  entry; cancelling either dialog changes nothing.
- **feat(player): auto-play next.** When a file plays to its end the next file in
  the folder starts, in the folder's current sort order — or the shuffled order
  when shuffle is on. Toggle in Preferences → Player (default on), a Next button in
  the player, and playback stops at the end of the folder unless "loop folder" is
  on.
- **feat(player): shuffle the queue.** Shuffle reorders the folder being played
  only — the library's sort is never touched — and the state is remembered per
  folder with a seed, so the order is stable between sessions.
- **feat(player): rotate button.** Cycles auto (sensor-driven, follows the device)
  and locked to the current orientation; the icon shows which is active and the
  choice persists. The existing Orientation preference keeps working and its
  default is unchanged.

## Released

### 0.1.12 — the library becomes the home screen
- **feat(resume): per-file positions in their own table.** `resume_state` is keyed
  by MediaStore id plus a path hash (so same-named files in two folders stay
  apart) and stores position, duration, last-played and a completed flag.
  Database version 6 with a migration; the player saves on pause, on stop and
  every 5 seconds, and seeks back to the stored position when it is 10 s+ in and
  not finished. Tests cover the write/read contract and the migration DDL.
- **feat(library): folder-first home.** Folders with a cover thumbnail and item
  count, expandable into a grid of videos that show thumbnail, duration and a
  resume bar. Pin a folder to the top, hide it, or sort by name, date, size or
  last played; search filters folders and videos at once.
- **feat(library): one repository, one exclusion point.** All media queries go
  through `MediaStoreLibraryRepository`; `LibraryExclusions` (hidden folders, plus
  file keys) is the single filter, and `LibraryExclusionSource` is the seam the
  locked-folder feature will bind to later. Nothing else knows what locking is.
- **feat(library): lazy folder paging and an LRU thumbnail cache** so a large
  library does not decode the same frame twice.
- **feat(home): URL playback and the file pickers moved behind the + button.**

### 0.1.11
- **fix(update): the banner no longer offers the version you already have.**
  The release tag is parsed into major.minor.patch plus the `-vela<N>` build and
  compared numerically with the installed build (versionName, git tag, and the
  versionCode as a fallback). The banner appears only when the release is
  strictly newer, and the check runs fresh on every launch — nothing about
  "update available" is cached.
- **fix(home): neutral URL field.** The address box no longer shows "Invalid
  protocol" on a fresh screen; the message appears only once what you typed
  cannot be played. Validation moved into `UrlValidation` with unit tests.
- **fix(home): one way to open a file.** "Pick a file" and "Open file picker"
  were two buttons doing nearly the same thing. The document picker stays the
  primary action and folder browsing is now a labelled secondary action.
- **fix(home): update banner no longer moves the screen.** It is drawn over the
  home content instead of inside it, and can be dismissed for that version
  (remembered in preferences, so it stays gone until the next release).
- **feat(subtitles): pluggable provider with a shared error language.** The
  keyless OpenSubtitles endpoint stays the default; OpenSubtitles.com can be
  switched on in Preferences → Subtitles with an API key (username and password
  too, because its download endpoint needs an account). Credentials are sealed
  with an Android Keystore AES-GCM key, never stored in the clear and never in
  source. Both providers report `ProviderUnavailable`, `RateLimited` or
  `KeyMissing`, so the player shows the same sentence either way. Results now
  show rating next to download count and format.

## 0.1.10
- **feat(library): browse the device library by folder** — folder grid with
  cover thumbnail and item count, folder opens its own grid, back returns to the
  folder list first.
- **feat(update): verify every download** against the release SHA-256 before the
  installer is opened, so a truncated download retries instead of failing with
  "package appears to be invalid".

## 0.1.9
- **feat(subtitles): download subtitles from inside the player** — search by the
  playing file's own name, no account needed, loaded as the selected track.
- **feat(update): unit-tested updater decisions** and CI that runs them.
