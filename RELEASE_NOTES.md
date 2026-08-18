# mpvRex Stremio Edition 4.5.11

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
