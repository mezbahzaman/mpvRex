# mpvRex — Stremio Edition

This is a patched build of [mpvRex](https://github.com/sfsakhawat999/mpvRex), the
libmpv-based Android video player, made specifically for people who use mpvRex as
the **external player inside Stremio**. It adds Stremio-focused features on top of
the original app. If you don't use Stremio, we recommend the original mpvRex.

## What's new in this build

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

### In-app auto-updates
Patched builds are published through this repository. The app automatically
checks for and installs the latest compatible build.

## Releases

Get the latest build from the
[Releases page](https://github.com/mezbahzaman/mpvRex/releases) of this fork.
Installing it keeps your existing data and playback history.

## Which build should you use?

- **Use Stremio as your external player?** This build is made for you.
- **For everything else**, use the original
  [mpvRex](https://github.com/sfsakhawat999/mpvRex) from its creator.
