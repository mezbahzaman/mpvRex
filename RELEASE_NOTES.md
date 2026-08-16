# mpvRex — Stremio Edition

This is a patched build of [mpvRex](https://github.com/sfsakhawat999/mpvRex), the
libmpv-based Android video player, made specifically for people who use mpvRex as
the **external player inside Stremio**. It adds Stremio-focused features on top of
the original app. If you don't use Stremio, we recommend the original mpvRex.

## What's new in this build

### More reliable automatic subtitles
Automatic Wyzie subtitle loading now recognizes direct files, HLS (`m3u8`), DASH
(`mpd`), HTTP proxy streams, and P2P handoffs. It tries IMDb IDs and multiple
parsed title sources instead of abandoning a stream after one weak title match.

### Live stream information
While a Stremio stream is playing — a peer-to-peer torrent or a direct HTTP
stream — a small overlay shows what the player is doing in real time:

- **Buffered video** — how many seconds are cached ahead
- **Seeders & peers** — live connection counts for torrents
- **Swarm size** — the total seeder count for the torrent's swarm
- **Download speed** — current rate, shown in B/s, KB/s or MB/s

The overlay appears automatically while a stream loads and disappears once
playback begins. A dedicated **"i" button** in the player controls shows or hides
it at any time. It only ever appears for network streams, never for local files.

### Smoother Stremio playback
Network streams receive purpose-built cache and network tuning — read-ahead,
buffer size and timeouts — configured separately for torrents and for HTTP
streams, which reduces stuttering on live and peer-to-peer sources.

### Honest seekbar buffering
The buffered-content indicator now reflects reality: on network streams it shows
the actual amount buffered (and "Buffering X%" while re-buffering) instead of a
fake full buffer; local files show no buffer indicator at all.

### Stremio resume support
When Stremio hands off a stream, playback resumes from the exact position Stremio
provided instead of restarting from the beginning.

### Auto-download subtitles for Stremio streams
When a Stremio stream has no embedded subtitles, the player automatically searches
online (via Wyzie's subtitle database) and loads the best-matching subtitle in your
preferred language. Files that already have embedded subtitles are left untouched.
If Stremio passes its own subtitle links in the handoff, those are used first.
You can turn this off in **Settings → Subtitle settings → Auto-download subtitles
for Stremio streams**. An API key for Wyzie is required (Settings → Subtitle
settings → Wyzie API key).

### In-app auto-updates
Patched builds are published through this repository. The app automatically
checks for and installs the latest compatible build.

### Fixed: update detection
Version comparison now ignores build suffixes, so newer patched releases are
correctly detected even when they share the same major.minor prefix.

### Fixed: crash when launching a video
The player no longer constructs its ViewModel before the MPV core is ready,
which could abort playback before it started (local files and Stremio streams).

### Fixed: auto-subtitle search could not identify the movie
For Stremio streams served through a proxy (e.g. a debrid or cloud addon), the
movie title was read from a generic proxy path like `movie.12147.2018...`, so the
subtitle search failed. The player now detects the real release name embedded in
the stream URL's query parameters (e.g. `KEY5=Hunter.Killer.2018.1080p...`) and
uses that for the search.

## Releases

Get the latest build from the
[Releases page](https://github.com/mezbahzaman/mpvRex/releases) of this fork.
Installing it keeps your existing data and playback history.

## Which build should you use?

- **Use Stremio as your external player?** This build is made for you.
- **For everything else**, use the original
  [mpvRex](https://github.com/sfsakhawat999/mpvRex) from its creator.
