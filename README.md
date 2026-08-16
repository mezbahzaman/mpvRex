# mpvRex — Stremio Edition

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" />
</p>

<p align="center">
  <b>A Stremio-focused build of the libmpv-based Android video player.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.3.10-purple.svg" />
  <img src="https://img.shields.io/github/downloads/mezbahzaman/mpvRex/total?logo=Github"/>
</p>

## About this project

This repository is a **fork of [mpvRex](https://github.com/sfsakhawat999/mpvRex)**,
the feature-rich Android video player built on libmpv with a modern Jetpack
Compose interface. It is maintained specifically for people who use mpvRex as the
**external player inside [Stremio](https://stremio.com)** and adds Stremio-focused
improvements on top of the original application.

**If you are not a Stremio user, please use the original
[mpvRex](https://github.com/sfsakhawat999/mpvRex).** This fork exists to serve a
single purpose — a better Stremio playback experience — and the original remains
the recommended player for general use.

## Extra features

Compared with the original mpvRex, this build adds:

- **Live stream information** — while a Stremio stream plays, an overlay shows
  buffered seconds, connected seeders and peers, the torrent swarm size and the
  current download speed. It appears automatically during loading and can be
  toggled any time with a dedicated "i" button. Network streams only.
- **Smoother Stremio playback** — purpose-built cache and network tuning (read-ahead,
  buffer size, timeouts) for torrent and HTTP streams, reducing stuttering on live
  and peer-to-peer sources.
- **Honest seekbar buffering** — the buffered indicator reflects the actual amount
  cached on network streams (including a "Buffering X%" state), instead of a
  misleading full buffer.
- **Stremio resume support** — streams handed off by Stremio resume from the exact
  position Stremio provided.
- **In-app auto-updates** — new patched builds are delivered through this
  repository's releases; the app checks for and installs the latest compatible
  build automatically.

## Releases

Pre-built APKs are published to the
[Releases page](https://github.com/mezbahzaman/mpvRex/releases). The release
notes describe exactly which extra features each build carries.

## Building from source

Follow the original project's build instructions; this fork adds no new build
dependencies or steps.

## License

Apache License 2.0. Original work by the
[mpvRex](https://github.com/sfsakhawat999/mpvRex) authors; this fork adds
Stremio-specific patches on top.
