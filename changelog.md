# Changelog — Image to Old Video

All notable changes to this project are documented here.
Versioning follows [Semantic Versioning](https://semver.org): `major.minor.patch`.

---

## [0.1.0] — 2026-04-22

### Added
- Initial prototype.
- JavaFX UI with a drag-and-drop image input zone.
- Sepia tone filter applied to the loaded image.
- Live preview of the sepia-processed image.
- MP4 video export using Jcodec (pure Java H.264 encoder).
- Configurable video duration (default 20 seconds, range 1–300).
- Output folder defaults to `D:\ImageToOldVideo`, falls back to `C:\ImageToOldVideo`.
- Browse button to choose a custom output folder.
- Background thread for video encoding so the UI stays responsive.
- Progress bar and status label during export.
