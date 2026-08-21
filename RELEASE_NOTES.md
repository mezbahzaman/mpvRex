# mpvRex Stremio Edition 4.5.14

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
