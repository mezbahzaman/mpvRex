# mpvRex Stremio Edition 4.5.9

## Removed

- Removed the forced DASH and HLS demuxer override from stream tuning.
- Removed the demuxer-override test and related URL-based format selection code.

## Added

- Added Product Sans Regular and Medium to the bundled subtitle font choices.

## Changed

- Buffering percentage display now falls back to MPV's reported cached range when
  `cache-buffering-state` is unavailable or invalid.
- Network cache and read-ahead settings remain isolated from local-file playback.
