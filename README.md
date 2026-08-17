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
the feature-rich Android video player built on libmpv with a modern Jetpack Compose
interface. This edition was created for people who experience problems with
Stremio's built-in players and want a more reliable external playback experience.

Although this fork is optimized for Stremio, it also works as a regular Android
video player. Users who do not need the Stremio-specific improvements can use the
original [mpvRex](https://github.com/sfsakhawat999/mpvRex), which remains an
excellent choice for general playback.

---

## Showcase

<div class="image-row" align="center">
  <img src="docs/images/p2p-stream-info.jpg" width="92%">
  <p><i>P2P Stream Info</i></p>
</div>

<div class="image-row" align="center">
  <img src="docs/images/http-stream-info.jpg" width="92%">
  <p><i>HTTP Stream Info</i></p>
</div>

<div class="image-row" align="center">
  <img src="docs/images/honest-buffering-percentage.jpg" width="92%">
  <p><i>Honest Buffering Percentage</i></p>
</div>

---

## Extra features

Compared with the original mpvRex, this build adds:

- **Accurate Stremio Continue Watching synchronization** — playback starts from the
  exact position supplied by Stremio. Progress and resume timing remain aligned, so
  Continue Watching works consistently when moving between Stremio and the player.
- **Live stream information** — a dedicated overlay presents useful network details
  while a stream is playing. For P2P streams, it shows buffered seconds, connected
  peers, available seeds, total swarm size, live ping, and live download speed. HTTP
  streams have their own streamlined view with buffered seconds, live ping, and live
  download speed. The overlay appears during startup and can also be opened manually
  with the information button.
- **Honest seekbar buffering** — the seekbar reports how much content is actually
  buffered instead of displaying an inaccurate full buffer. During loading, a clear
  buffering percentage provides a realistic view of playback readiness.
- **Automatic online subtitles** — when a Stremio stream has no embedded subtitle
  track, the player can search for and load a suitable subtitle automatically. This
  feature requires a Wyzie API key; a free key can be redeemed from the
  [Wyzie Store](https://store.wyzie.io/redeem). Streams that already contain embedded
  subtitles do not trigger an additional online search. A manual subtitle search is
  still available whenever another result or language is needed.

## Releases

All pre-built APKs and release notes are published on the
[GitHub Releases page](https://github.com/mezbahzaman/mpvRex/releases).

## Building from source

Follow the original project's build instructions; this fork adds no new build
dependencies or steps.

## Credits

mpvRex has its roots in **[mpvEx](https://github.com/marlboro-advance/mpvEx)**,
which itself builds on **[mpv-android](https://github.com/mpv-android/mpv-android)**.
We are grateful to their contributors for the foundations on which this project is
built.

Additional inspiration and reference:
[mpvKt](https://github.com/abdallahmehiz/mpvKt) ·
[Next Player](https://github.com/anilbeesetti/nextplayer) ·
[Gramophone](https://github.com/FoedusProgramme/Gramophone)

## License

Apache License 2.0. Original work by the
[mpvRex](https://github.com/sfsakhawat999/mpvRex) authors; this fork adds
Stremio-specific patches on top.
