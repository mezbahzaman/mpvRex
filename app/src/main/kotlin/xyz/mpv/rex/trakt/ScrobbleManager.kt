package xyz.mpv.rex.trakt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class ScrobbleManager(
  private val mdbListScrobbler: MdbListScrobbler,
  private val preferences: MdbListPreferences,
  private val scope: CoroutineScope,
) {
  companion object {
    private const val TAG = "ScrobbleManager"
    private const val PROGRESS_UPDATE_INTERVAL_MS = 30_000L
    private const val MIN_SCROBBLE_PROGRESS = 1.0
  }

  private val mutex = Mutex()
  private var periodicUpdateJob: Job? = null
  private var currentMediaInfo: ScrobbleMediaInfo? = null
  private var currentSessionKey: Long? = null
  private var scrobbleStarted = false
  private var lastProgressSent = 0.0
  private var progressProvider: (() -> Double)? = null
  private val latestSessionKey = AtomicLong(0L)
  private val closedSessionKeys = mutableSetOf<Long>()

  fun isEnabled(): Boolean = preferences.isConfigured()

  fun newSessionKey(): Long = latestSessionKey.incrementAndGet()

  suspend fun onStart(
    sessionKey: Long,
    mediaInfo: ScrobbleMediaInfo,
    progressPercent: Double,
    progressProvider: (() -> Double)? = null,
  ) = mutex.withLock {
    if (!isEnabled() || sessionKey != latestSessionKey.get()) return@withLock
    if (closedSessionKeys.remove(sessionKey)) return@withLock

    if (scrobbleStarted && currentSessionKey != sessionKey) {
      stopCurrentScrobble()
    }

    currentSessionKey = sessionKey
    currentMediaInfo = mediaInfo
    lastProgressSent = progressPercent
    this.progressProvider = progressProvider

    val request = mediaInfo.toScrobbleRequest(progressPercent)
    val result = mdbListScrobbler.scrobble("start", request)
    if (!isEnabled() || sessionKey != latestSessionKey.get()) {
      reset()
      return@withLock
    }
    if (result.isSuccess) {
      scrobbleStarted = true
      Log.d(TAG, "Scrobble started: ${mediaInfo.title} at ${progressPercent}%")
    } else {
      Log.w(TAG, "Scrobble start failed: ${result.exceptionOrNull()?.message}")
    }

    if (scrobbleStarted) startPeriodicUpdates()
  }

  suspend fun onPause(sessionKey: Long, progressPercent: Double) = mutex.withLock {
    if (!isEnabled() || !scrobbleStarted || currentSessionKey != sessionKey) return@withLock

    stopPeriodicUpdates()
    lastProgressSent = progressPercent

    val mediaInfo = currentMediaInfo ?: return@withLock
    val request = mediaInfo.toScrobbleRequest(progressPercent)
    val result = mdbListScrobbler.scrobble("pause", request)
    if (result.isSuccess) {
      Log.d(TAG, "Scrobble paused: ${mediaInfo.title} at ${progressPercent}%")
    } else {
      Log.w(TAG, "Scrobble pause failed: ${result.exceptionOrNull()?.message}")
    }
  }

  suspend fun onResume(sessionKey: Long, progressPercent: Double) = mutex.withLock {
    if (!isEnabled() || !scrobbleStarted || currentSessionKey != sessionKey) return@withLock

    lastProgressSent = progressPercent

    val mediaInfo = currentMediaInfo ?: return@withLock
    val request = mediaInfo.toScrobbleRequest(progressPercent)
    val result = mdbListScrobbler.scrobble("start", request)
    if (result.isSuccess) {
      Log.d(TAG, "Scrobble resumed: ${mediaInfo.title} at ${progressPercent}%")
    } else {
      Log.w(TAG, "Scrobble resume failed: ${result.exceptionOrNull()?.message}")
    }

    startPeriodicUpdates()
  }

  suspend fun onStop(sessionKey: Long, progressPercent: Double) = mutex.withLock {
    if (!isEnabled()) return@withLock
    if (!scrobbleStarted || currentSessionKey != sessionKey) {
      if (closedSessionKeys.size >= 32) closedSessionKeys.clear()
      closedSessionKeys += sessionKey
      return@withLock
    }

    stopPeriodicUpdates()
    lastProgressSent = progressPercent

    val mediaInfo = currentMediaInfo ?: return@withLock
    val clampedProgress = progressPercent.coerceAtLeast(MIN_SCROBBLE_PROGRESS)
    val request = mediaInfo.toScrobbleRequest(clampedProgress)
    val result = mdbListScrobbler.scrobble("stop", request)
    if (result.isSuccess) {
      val action = result.getOrNull()?.action
      Log.d(TAG, "Scrobble stopped: ${mediaInfo.title} at ${clampedProgress}% (action=$action)")
    } else {
      Log.w(TAG, "Scrobble stop failed: ${result.exceptionOrNull()?.message}")
    }

    reset()
  }

  private fun startPeriodicUpdates() {
    stopPeriodicUpdates()
    periodicUpdateJob = scope.launch {
      while (true) {
        delay(PROGRESS_UPDATE_INTERVAL_MS)
        sendPeriodicUpdate()
      }
    }
  }

  private fun stopPeriodicUpdates() {
    periodicUpdateJob?.cancel()
    periodicUpdateJob = null
  }

  private suspend fun sendPeriodicUpdate() = mutex.withLock {
    if (!isEnabled()) {
      reset()
      return@withLock
    }
    if (!scrobbleStarted) return@withLock
    val mediaInfo = currentMediaInfo ?: return@withLock
    val progress = progressProvider?.invoke() ?: lastProgressSent
    lastProgressSent = progress
    val request = mediaInfo.toScrobbleRequest(progress)
    mdbListScrobbler.scrobble("start", request).onFailure {
      Log.w(TAG, "Periodic scrobble update failed: ${it.message}")
    }
  }

  private fun reset() {
    currentMediaInfo = null
    currentSessionKey = null
    scrobbleStarted = false
    lastProgressSent = 0.0
    progressProvider = null
    stopPeriodicUpdates()
  }

  fun logout() {
    latestSessionKey.incrementAndGet()
    scope.launch {
      mutex.withLock {
        reset()
        preferences.apiKey.set("")
        preferences.username.set("")
      }
    }
  }

  fun destroy() {
    latestSessionKey.incrementAndGet()
    scope.launch {
      mutex.withLock { reset() }
    }
  }

  fun stopAsync(sessionKey: Long, progressPercent: Double) {
    scope.launch { onStop(sessionKey, progressPercent) }
  }

  private suspend fun stopCurrentScrobble() {
    val mediaInfo = currentMediaInfo ?: return
    val progress = lastProgressSent.coerceAtLeast(MIN_SCROBBLE_PROGRESS)
    mdbListScrobbler.scrobble("stop", mediaInfo.toScrobbleRequest(progress)).onFailure {
      Log.w(TAG, "Previous media scrobble stop failed: ${it.message}")
    }
    reset()
  }
}
