# Changelog — Image to Old Video

All notable changes to this project are documented here.
Versioning follows [Semantic Versioning](https://semver.org): `major.minor.patch`.

---

## [0.2.0] — 2026-04-24

### Added
- New top bar with title, version, and Help button.
- Two gallery tabs on the left: Images and Videos.
- Video Gallery settings dialog for output folder, max storage budget, and max video count.
- Storage utilization indicator with color states:
	- Green: normal usage.
	- Yellow: warning threshold.
	- Red: at/over configured limit.
- Export guard when storage budget is full.
- Generated video naming convention: `<sourceImageName>_<presetName>_<yyyyMMdd_HHmmss>.mp4`.
- Custom preset management in the main UI (Add, Update, Delete) backed by `PresetManager` JSON persistence.
- Effect rows now use compact ON/OFF toggles (green ON, gray OFF).
- Effect-row controls (sliders/value labels) disable and gray out when effect is OFF.
- Mini video preview controls under the preview area:
	- Frame slider with frame number indicator.
	- Step previous/next frame.
	- Play/Pause preview.
	- Loop toggle (loop or one-pass playback).
	- Configurable preview duration (1-10 seconds).
	- Configurable preview FPS (30 or 60).
- Effects-only alpha preview toggle to inspect effect layers without the base image.
- Effects-only export as transparent PNG sequence for compositing workflows.
- Persistent vertical divider position between top preview area and preset/effects section.

### Changed
- Main window reorganized into stacked sections:
	- Top bar.
	- File and preview section.
	- Preset management section.
	- Scrollable effects section.
	- Fixed export/progress section.
	- Bottom status bar.
- Controls compacted for higher information density while preserving readability.

### Documentation
- Updated docs to describe gallery tabs, storage settings behavior, naming convention, and preview workflow.
- Added a full user guide with an updated interface map and screenshot refresh checklist.

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
