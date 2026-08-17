# mpvRex Stremio Edition 4.5.2

mpvRex Stremio Edition is a specialized build of
[mpvRex](https://github.com/sfsakhawat999/mpvRex) for viewers who prefer to use an
external player with Stremio. It retains the complete mpvRex playback experience
and can also be used as a regular Android video player.

## Fixes in 4.5.2

- A zero network cache size or buffered-duration setting now disables only that
  threshold. Positive size and time thresholds still stop read-ahead when either
  limit is reached, and setting both to zero leaves read-ahead unrestricted by
  these two settings.
- Local playback state is keyed by the full file location, so files with the same
  name in different folders no longer share watched status or resume data.
  Existing filename-based playback records remain available as a fallback.
- Watched and unwatched folder counts refresh after playback state is saved rather
  than retaining scanner results for up to three minutes.
- Network seekbars use MPV's buffered endpoint when available, with its buffered
  duration as a fallback, so the range tracks the demuxer's current cache.

## Stremio integration

### Accurate Continue Watching synchronization

- Playback starts at the exact position supplied by Stremio, including position
  zero, rather than allowing an unrelated internal resume point to override it.
- Resume state is kept separate for each media item so timing from a previous
  stream cannot carry over to the next one.
- Stremio's Continue Watching position remains aligned when playback moves between
  Stremio and mpvRex.

### Reliable external-player handoff

- Direct HTTP files, Stremio P2P streams, HLS playlists, DASH manifests, and common
  proxy or debrid URLs are recognized as network handoffs.
- Playback state, stream statistics, and subtitle work are reset and validated for
  each media path, preventing stale information or background work from crossing
  between streams.
- Player initialization waits until the MPV core is ready, avoiding startup failures
  when opening either Stremio streams or local files.

## Live stream information

The Stream Info overlay provides a clear real-time view of network playback. It is
shown automatically during startup and remains visible until at least five seconds
of video are buffered. After that, it can be opened or closed at any time with the
dedicated information button. It is limited to network streams and does not appear
for local files.

### P2P stream details

- Buffered video in seconds
- Connected seed count
- Connected peer count
- Total torrent swarm size, queried through both HTTP and UDP trackers
- Live network ping
- Live download speed in B/s, KB/s, or MB/s

### HTTP stream details

- Buffered video in seconds
- Live network ping
- Actual per-second received network data in B/s, KB/s, or MB/s
- An immediate `0 KB/s` reading when traffic stops, playback is paused, or the
  stream is already fully buffered, instead of retaining an outdated speed

If a ping probe fails or times out, the overlay reports `0 ms` rather than leaving
an old latency value visible.

All stream information now follows one configurable refresh interval, including
buffered seconds, speed, peers, seeds, swarm data, and ping. The default is one
second. The ping destination can also be changed from its default, `google.com`.

## Streaming and buffering

### Network-specific playback tuning

- P2P and HTTP streams receive separate cache, read-ahead, buffer-size, and timeout
  settings designed for their different delivery methods.
- Decoder selection follows a resilient fallback order: hardware-copy mode first,
  standard hardware decoding second, and software decoding last.
- Stream state is isolated by media path to reduce stale data, incorrect speed
  readings, and playback instability when changing videos.
- **Extra Settings** allows the maximum network download cache and maximum buffered
  duration to be changed without editing `mpv.conf`. Defaults are `200 MiB` and
  `180 seconds`, and apply consistently to HTTP and P2P playback.

### Honest seekbar buffering

- Network streams display the amount of media that is genuinely cached instead of
  showing a misleading fully buffered seekbar.
- Rebuffering displays a clear `Buffering X%` status based on MPV's real buffering
  state.
- The percentage label uses a stable width and fixed-width digits so rapid
  single-digit updates do not overlap or visually merge.
- Local files do not show a network-style buffer indicator because the entire local
  file is already seekable.

## Automatic subtitles

### Wyzie subtitle search

- When a Stremio video has no embedded subtitle track, mpvRex can automatically
  search Wyzie and attach the best match in the preferred language.
- A Wyzie API key is required. A free key can be redeemed at
  [store.wyzie.io/redeem](https://store.wyzie.io/redeem) and entered under
  **Settings > Subtitle settings > Wyzie API key**.
- Automatic loading can be enabled or disabled under
  **Settings > Extra Settings > Auto-download subtitles for Stremio streams**.
- Manual online subtitle search remains available whenever a different language or
  result is preferred.

### Embedded and Stremio-provided subtitles

- Videos that already contain subtitle tracks do not trigger an unnecessary online
  search.
- If Stremio includes subtitle URLs in its handoff, those tracks are loaded first.
- When several embedded tracks exist, mpvRex restores and selects the preferred
  subtitle track according to the player's normal language and track preferences.

### Optional subtitles for local files

- Automatic Wyzie search can also be enabled for local videos that have no embedded
  or already loaded subtitle track. This option is disabled by default.
- Common camera folders and camera-style filenames are always skipped.
- Additional folders can be excluded with a multi-folder blacklist in
  **Extra Settings**. The blacklist is available only while local automatic
  subtitles are enabled.

### Accurate title matching

- Searches can derive movie and episode information from direct filenames, URL
  paths, query parameters, nested encoded URLs, Stremio metadata, IMDb IDs, release
  names, years, seasons, and episode numbers.
- Proxy and debrid links are inspected for the real release filename instead of
  relying on a generic proxy path.
- Multiple title candidates are attempted when the first metadata source is weak or
  produces no results.
- Downloaded subtitles are discarded if the active media changes before the
  download finishes.

### Runtime safeguards

- Automatic online search is reserved for videos with a confirmed runtime of at
  least ten minutes, preventing unnecessary subtitle requests for short clips and
  live streams.
- Unknown duration remains pending until MPV reports a valid value; it is not
  mistaken for a short or live stream.

## Extra Settings

A new **Extra Settings** screen is available under **Advanced & About**. It contains
the Stremio automatic subtitle switch, local-file automatic subtitle controls,
folder exclusions, maximum buffered seconds, maximum network download size, stream
information refresh interval, and ping host.

## Subtitle fonts

- Roboto, Lato, and Noto Sans are bundled as ready-to-use subtitle font families.
- Existing custom font-folder support remains available, and user-provided font
  files are never overwritten by bundled fonts.

## Updates and compatibility

- The built-in updater recognizes release suffixes correctly when comparing
  versions and can discover newer compatible Stremio Edition builds.
- Releases are built for `arm64-v8a` and published as signed APKs with SHA-256
  checksum files.
- Installing an update over an existing Stremio Edition installation preserves app
  data, preferences, and playback history.

## Download

All signed APKs and checksum files are available from the
[GitHub Releases page](https://github.com/mezbahzaman/mpvRex/releases).
