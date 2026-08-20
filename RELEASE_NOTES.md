# mpvRex 4.6.10

Stremio reliability batch:

- Continue Watching: the left-off position is now reliably reported back to Stremio when the player closes (fixes occasional missing/old resume position).
- Stream info overlay: ping now updates every second and is measured against the stream's own host (reliable even when ICMP is blocked); peer/seed/ping values no longer flicker to 0 on a single empty poll.
- Seekbar: fixed the inaccurate buffered-ahead indicator shown while tapping/dragging the seekbar (now uses mpv's absolute buffered timestamp and is hidden during an active seek).
- Automatic subtitles now wait for MPV to report stream duration before applying the ten-minute minimum-runtime rule.

See CHANGELOG-stremio-fixes.md for full technical detail.
