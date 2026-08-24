# mpvRex Stremio Edition 4.5.30

## Added

- Decoder priority settings (Settings > Decoder): pick the default decoder
  1st and 2nd priority from HW+ / HW / SW; the remaining decoder becomes an
  automatic last resort, giving a full three-step fallback chain
  (for example HW+ → HW → SW). Applies to new playback sessions; the
  in-player per-video decoder override still wins when used.

## Fixed

- Audio focus is now released correctly after a clean grant; previously it
  was held until process death and could delay other apps' audio.
- Ending a background playlist no longer leaves the foreground service and a
  stale notification running indefinitely.
- Stremio progress snapshots are written off the main thread through an
  ordered writer; crash durability is preserved and per-second UI stalls are
  gone.
- Bounded demuxer cache floors: setting a zero limit now selects a small
  bounded cache instead of an unbounded one that could grow until OOM.
- Thumbnail, tracker, whois, and ping responses are size-bounded reads.
- Picture-in-Picture control receiver can no longer be registered twice.

## Changed

- Memory: thumbnails decode at the size actually displayed instead of always
  1024 px, heavy extraction is capped at two concurrent jobs, folder
  generation stops when its screen closes, disk thumbnails use JPEG q85,
  embedded-art/frame caches are byte-budgeted, and devices with ≤4 GB RAM get
  a smaller default network download budget (96 MiB) unless configured.
- Battery/CPU: precise-position polling dropped from ~60 fps to ~20 fps with
  identical seekbar smoothness, and seek-settling logic no longer restarts on
  every position tick.
- Network streams start more resiliently: larger stream buffer plus silent
  lavf reconnection on dropped HTTP connections for P2P and HTTP playback.
- Default decoder chain is now HW+ → HW → SW (previously HW+ → SW), so a
  failing direct hardware decode falls back to copy-mode hardware before
  software.

## Previous Release

## Fixed

- All stream-info reads now run under one cancellable active job.
- Closing or auto-dismissing the overlay cancels the immediate read as well as
  the repeating cycle; no untracked ping, torrent, swarm, or IP operation is
  started in the background.
- New playback paths reset the startup info state and begin a fresh active read.

## Previous Release

## Fixed

- Fixed the ping probe timeout so ping updates participate in the same
  one-second stream-info cycle as buffer, seeds, peers, and speed.
- Manual overlay activation now starts a fresh IP lookup and immediate stats
  reads.
- Closing or auto-dismissing the overlay cancels the active swarm lookup and
  leaves no stream-info network reader running in the background.

## Previous Release

## Changed

- Restored IP, country, and country-flag stream information.
- Stream-info network features now activate only during startup’s visible
  information phase or while the panel is manually enabled.
- ipwho.is is queried once per stream activation instead of every minute.
- After the overlay auto-dismisses at 5 seconds buffered, ping, torrent, swarm,
  and IP lookups become idle until the panel is manually opened again.
- The stream-info polling coroutine itself is cancelled while idle and resumes
  immediately for a new stream or manual panel open.

## Previous Release

## Changed

- Removed the ipwho.is public-IP, country, and flag lookup implementation and
  its stream-info fields.
- Kept stream protocol and local playback metrics in the overlay without an
  external geolocation dependency.

## Fixed

- Reduced battery and network overhead from stream statistics polling.
- Expensive ping and torrent-stat requests are now throttled while the overlay
  is visible and slowed further when it is hidden.
- One-second local metric updates no longer launch a ping process or perform a
  torrent HTTP request on every cycle.

## Previous Release

## Added

- Added independent HTTP and P2P stream-info placement controls under Player
  More Options > Aesthetics.
- Each directional button moves its stream-info panel by exactly one pixel.
- Placement offsets persist across launches and are included in settings export
  and import.

## Previous Release

## Fixed

- Removed the vertical guide line from the stream information panel.
- Moved the panel right and slightly upward to avoid the display notch and
  bottom-left playback controls.
- Split the stream identity into separate protocol, IP, and country rows.
- Reordered the remaining fields into protocol, IP, country, buffer, seeds,
  peers, swarm, ping, and speed order.

## Previous Release

## Changed

- Redesigned stream information as a compact, left-aligned vertical panel with
  a subtle vertical guide line.
- Centered the panel in the safe space between the player control areas so it
  does not overlap the top bar or bottom controls.
- Reduced the stream information text size and background opacity to match the
  existing protocol identity styling.
- P2P streams show seeds, peers, and swarm count; HTTP streams show only the
  fields relevant to HTTP playback.

## Previous Release

## Fixed

- Stream information now wraps within the available overlay width instead of
  clipping long identity and metrics lines.
- Stream metrics use fixed one-second deadlines so slow network probes do not
  add their duration to the next refresh interval.
- Ping and torrent statistics requests run concurrently with bounded timeouts,
  keeping the visible stream information responsive.

## Previous Release

## Changed

- Stream information metrics now refresh together on one synchronized update.
- Public IP and country details refresh every minute, while the stream protocol is
  captured once per playback session.
- Transfer speeds now use KB/s or MB/s, with values below 1 KB/s shown as `0 KB/s`.
- Removed the RexShorts feature, its preferences, media data, and database table.
- Reduced background polling and retained the latest Stremio playback progress in
  durable storage for crash recovery.

## Previous Release

## Fixed

- Both HTTP and P2P stream information now show the device/network's public IP
  and its country from ipwhois.io. HTTP streams no longer show the remote server
  IP, so the identity line is consistent across both stream types.

## Previous Release

## Fixed

- Stream information no longer displays the stream URL host. Its compact first
  line now shows protocol, public IP, country, and country flag using a cached
  ipwhois.io lookup, for example `P2P • 178.866.79.168 • Singapore (🇸🇬)`.
- Public HTTP stream hosts are resolved to their server IP. Local/private P2P
  streaming endpoints use the device's public IP reported by ipwhois.io.

## Previous Release

## Fixed

- Restore the seekbar buffered-content hint on streams where mpv does not
  provide `reader-pts`. The hint still rejects the pre-seek range, then becomes
  visible after mpv publishes the next cache update at the seek target.

## Previous Release

## Fixed

- The seekbar's buffered-content hint now stays collapsed after seeking until
  mpv confirms that its cache reader has reached the new playback location.
  Missing or partially updated post-seek cache values can no longer expose the
  stale pre-seek buffered range.

## Previous Release

## Fixed

- Deleting a subtitle now stops automatic subtitle loading from adding it back
  seconds later, for both online streams and local files. Manual subtitle search,
  download, and file picking continue to work and override an earlier deletion.
- The seekbar no longer shows buffered seconds that have not been downloaded yet
  after tapping or dragging it. mpv's cached range is now checked against its read
  position, so pre-seek cache samples are discarded instead of being drawn until
  the next update corrects them.
- Swipe-to-seek now suppresses the stale buffered range the same way seekbar
  seeking already did.
- The buffering percentage overlay no longer reports progress from a pre-seek
  cache sample.

## Added

- Added tests covering subtitle deletion opt-out behavior and buffered-range
  freshness across backward and forward seeks.

## Previous Release

## Fixed

- Return precise, latest playback position and duration to Stremio when closing movies or episodes.
- Preserve stream-buffer visualization across seeks without displaying stale pre-seek cache ranges.
- Refresh network ping independently every second and distinguish unavailable results from `0 ms`.
- Harden P2P statistics classification, polling cancellation, and metric parsing.
- Remove stale Trakt/MDBList scrobbling integration and its Extra Settings controls.

## Changed

- Stream details now show protocol and host information and use a compact two-line layout.
- Task removal no longer force-kills the process before playback progress can be returned.

## Added

- Added focused tests for precise Stremio result values, stream URL validation, and seekbar buffer ranges.

## Previous Release

## Fixed

- Accept Stremio's `startfrom` playback-position extra when launching external streams.

## Added

- Added focused coverage for `startfrom` parsing and position precedence.

## Previous Release

## Removed

- Removed the forced DASH and HLS demuxer override from stream tuning.
- Removed the demuxer-override test and related URL-based format selection code.

## Added

- Added Product Sans Regular and Medium to the bundled subtitle font choices.
- Added focused tests for Stremio position parsing and result reporting.

## Changed

- Buffering percentage display now falls back to MPV's reported cached range when
  `cache-buffering-state` is unavailable or invalid.
- Network cache and read-ahead settings remain isolated from local-file playback.
- Stremio playback positions accept safe numeric intent types and use millisecond normalization.
- Restored the original unconditional `Int` millisecond result payload for compatibility
  with Stremio external-player progress handling.
- Playback-state writes are serialized so older lifecycle snapshots cannot overwrite newer saves.
