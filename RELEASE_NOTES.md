# mpvRex-Stable (Stremio-resume-sync patched build)

A patched build of mpvRex that adds live stream information and playback
stability improvements for Stremio users, plus resume support for the
Stremio external-player handoff. This release is automatically rebuilt on
top of every new mpvRex upstream update (see the `rebuild.yml` workflow).

## New: Live Stream Info overlay (Stremio P2P + HTTP)

A real-time stats overlay for network streams — both Stremio P2P torrents
and direct HTTP addon streams — so you can see exactly what the player is
doing while the video loads or when it stalls:

- **Buffered seconds** — the actual amount of video the mpv demuxer has
  cached ahead, from `demuxer-cache-duration`.
- **Seeders & peers** — live counts for torrents, read from Stremio's local
  streaming engine (`stats.json`): connected seeders are counted from the
  engine's active wire connections, and the peer count is the engine's
  reported value.
- **Global swarm size** — the total seeder count for the torrent's swarm,
  queried directly from the torrent's HTTP(S) trackers (bencoded announce
  requests), polled at most once a minute. Shown as "N in swarm" when
  available.
- **Download speed** — the engine's reported download rate (falling back to
  mpv's `cache-speed` for HTTP streams), formatted as B/s, KB/s or MB/s.
- **Graceful degradation** — if the stats endpoint can't be reached the
  overlay falls back to the HTTP-only view; seed counts are retained rather
  than collapsing to a misleading 0 while the stream is idle/buffered.

### How it behaves

- Shown **automatically once** at the start of each stream (one-shot): it
  appears while the stream is loading/buffering and disappears the moment
  the video actually begins playing. It will not keep popping back up on
  later stalls.
- A new **"i" (Stream Info) button** in the player controls bar lets you
  show or dismiss the overlay at any time.
- Works for **both** torrent and HTTP streams, and only ever appears for
  network streams (never local files).

## New: Per-stream playback tuning (Stremio stability)

`StreamTuning` applies purpose-built mpv cache/network tuning when a network
stream is loaded, tuned separately for torrents and for plain HTTP streams
(demuxer read-ahead, demuxer max bytes, and network timeouts). This is what
removed the mid-stream stuttering on live HTTP addon streams and keeps
buffering smooth during torrent play. Tuning is applied both in the normal
player and in headless/background playback.

## New: Honest seekbar buffering

The buffered-content indicator on the seekbar now reflects reality:

- **Network streams** — shows the real amount of demuxer-cache buffered, and
  a "Buffering X%" status while (re)buffering. The previous behaviour faked a
  1-minute buffer while the stream was actually stalled.
- **Local files** — the buffer indicator is hidden entirely, since local
  playback always reads ahead fully and a full buffer bar is meaningless.

## Stremio resume position support

When Stremio hands a stream off to mpvRex it includes a `position` extra in
the launch intent. This build honors that position as authoritative — the
stream resumes exactly where Stremio left off, instead of being reset to 0 or
overridden by the app's own saved position. Launch-intent extras are logged
for debugging the Stremio handoff.

## In-app auto-update (from this fork)

The update check has been repointed at this fork's releases, so the app
automatically finds and offers newer patched builds
(`api.github.com/repos/mezbahzaman/mpvRex/releases/latest`) and installs the
correct arm64-v8a APK for the device.

## Build

- Release builds produce a single **arm64-v8a** APK (no universal APK).
- Tagged releases are auto-built from the latest upstream mpvRex via the
  included GitHub Actions workflow (`rebuild.yml`, runs daily plus on
  demand), signed with the same release keystore, and published to this
  repo's releases page.

## Install

```
adb install -r mpvRex-<version>-arm64-v8a-stremio-sync-patched.apk
```

Same signature as previous releases, so data and saved positions are kept.
