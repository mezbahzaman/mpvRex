# Stremio Edition — reliability fixes

This branch focuses on making the Stremio external-player experience as
reliable as possible. Summary of every change in this batch.

## 1. Continue-Watching resume is reported reliably on exit (top priority)

**Problem:** mpvRex correctly *received* the left-off position from Stremio and
resumed from it, but when the player closed it did not *always* hand the new
position back, so Stremio's Continue Watching list sometimes kept the old (or a
zero) position.

**Cause:** `setReturnIntent()` read `viewModel.pos` / `viewModel.duration`,
which are `StateFlow`-collected values. During activity teardown those flows can
momentarily emit `null`, so the `position` / `duration` result extras were
silently dropped.

**Fix:**
- Added a last-known-good position/duration cache in `PlayerViewModel`
  (`resolveReportPositionSec()` / `resolveReportDurationSec()`). Resolution
  order: fresh live MPV read → collected flow → cached last-valid value.
- The position/duration flows now continuously feed that cache while playing.
- `setReturnIntent()` uses the resolver, so the resume point is handed back to
  Stremio even if the flows read null at the moment of exit. Unit stays the same
  (seconds × 1000 = ms), so it is fully compatible with Stremio.
- Reporting is independent of the "save position on quit" preference (Stremio
  owns its own resume list), so the hand-back always happens.

Files: `PlayerViewModel.kt`, `PlayerActivity.kt`.

## 2. Trakt / MDBList scrobbling

Checked the entire repository (all branches, all files, the `patched.patch`,
docs and website) — there is **no** Trakt or MDBList / scrobbling code anywhere.
That earlier experiment was never committed, so it was lost on a fresh clone.
Nothing to remove; the codebase is already clean of it.

## 3. Stream-info overlay is more reliable; ping updates every second

**P2P (torrent) streams** — buffered seconds, peers, seeds, swarm, ping, speed:
- **Ping now refreshes every second** (was every 5 s).
- Ping is measured as a **TCP connect to the stream's own host** instead of an
  ICMP ping to google.com. This is reliable even where ICMP is blocked and
  reflects the real latency to the server actually serving the video.
- Peer and seed counts are **retained across a single empty poll** so the
  overlay no longer flickers to 0 when one stats fetch comes back empty.
- Ping value is retained on a transient failed probe instead of dropping to 0.

**HTTP streams** — buffered seconds, ping, speed: same 1 s TCP-based ping and
retention behaviour.

Files: `StreamStatsFetcher.kt` (new `fetchStreamPingMs` / `hostPortOf`),
`PlayerViewModel.kt`.

## 4. Seekbar buffered-seconds visual glitch on tap / drag

**Problem:** while tapping or dragging the seekbar, the buffered (read-ahead)
indicator briefly showed an inaccurate amount buffered ahead of an unbuffered
position, then corrected itself a second later.

**Cause:** the buffered end was computed as `currentPos + demuxerCacheDuration`.
On a seek, `currentPos` jumps immediately but the demuxer's relative cache length
is still describing the *old* region for a moment — mixing the two drew a fake
buffer.

**Fix:**
- The buffered end now uses mpv's **absolute** `demuxer-cache-time` (the true end
  timestamp of buffered content), which never has to be added to the play head,
  so it stays correct the instant the position changes. Falls back to the old
  relative calculation only when that property is unavailable.
- The buffer indicator is **suppressed while the user is actively scrubbing**, so
  nothing misleading is drawn during a seek; it reappears accurately once
  playback settles.
- Registered `demuxer-cache-time` as an observed MPV property so it updates live.

Files: `PlayerControls.kt`, `MPVView.kt`.

## 5. Readability / decoration

Added explanatory KDoc/comments on all touched hot paths (resume resolver,
ping probing, buffered-end calculation) and removed a stale/misplaced doc
comment above the result-intent section in `PlayerActivity.kt`.
