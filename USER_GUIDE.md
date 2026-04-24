# Image to Old Video - User Guide

## Version
- App version: 0.2.0
- Runtime: Java 25

## Quick Start
1. Run the app with `mvn javafx:run`.
2. Add an image from the Images tab or drag/drop onto the preview pane.
3. Choose a preset and adjust effect toggles/sliders.
4. Use the mini preview controls to inspect frame-by-frame output.
5. Click Export Video.
6. Open generated videos from the Videos tab.

## Interface Overview
The main form is organized into these stacked sections:
1. Top Bar: Help button, app title, version.
2. File and Preview Section: image/video galleries on the left and preview pane on the right.
3. Preset Section: built-in preset picker + custom preset controls.
4. Effects Section: scrollable panel of effect controls.
5. Export Section: Export Video button and progress bar (anchored near bottom).
6. Status Bar: message line at the bottom.

The divider between the upper preview area and the preset/effects section is resizable and persisted between launches.

## Gallery Tabs
### Images Tab
- Add images through Add button or drag/drop into the panel.
- Click an image to load it into preview.
- Right-click entries to rename, edit description, or remove from gallery.

### Videos Tab
- Lists generated videos with source-image thumbnails and play overlay.
- Double-click or use context menu to play in your OS default video player.
- Drag videos from the app to Explorer to save or reorganize.
- Use Settings to manage output folder and retention/storage policy.

## Storage Settings
Open Videos -> Settings to configure:
1. Output folder for generated videos.
2. Max storage space (GB), where -1 means unlimited.
3. Max video count, where -1 means unlimited.

Storage indicator behavior:
- Green: normal.
- Yellow: near limit.
- Red: at/over limit.

When the storage policy is at capacity, Export Video is disabled.

## Presets and Effects
### Built-in Presets
- None
- 20s Reel
- 50s Film
- 80s VHS
- Faded
- Sepia

### Custom Presets
Use the Custom preset row controls:
- Add: save current effect setup as a new custom preset.
- Update: overwrite selected custom preset with current settings.
- Delete: remove selected custom preset.

### Effect Toggles
Each effect row uses a compact ON/OFF toggle:
- ON = green.
- OFF = gray.

When OFF, that row's sliders and value fields are disabled.

## Mini Video Preview Controls
The preview section includes a mini playback control strip under the image.

Controls:
1. Previous frame and next frame buttons.
2. Play/Pause button.
3. Loop checkbox:
   - On: loops continuously.
   - Off: one-pass playback and stops at final frame.
4. Preview seconds spinner (1-10 seconds).
5. FPS selector (30 or 60).
6. Frame slider for direct scrubbing.
7. Frame indicator label (`Frame X / N`).
8. Effects-only Alpha checkbox to show only effect contribution over transparency.

How it works:
- The preview renders processed frames using the current effect configuration.
- Total preview frames = preview seconds * selected FPS.
- You can scrub, step, or play to inspect temporal behavior before exporting.
- Effects-only Alpha mode helps isolate and tune effect layers without the source image.

## Transparent Effects Export
- Use `Export Effects Alpha` to render only effect contribution as transparent PNG frames.
- Output is written to a timestamped sequence folder in the configured output directory.
- This sequence can be imported as an overlay in video editors.
- Note: The current app exports transparent overlays as PNG sequence (not alpha MP4).

## Output Naming Convention
Generated video filenames use:
- `<sourceImageName>_<presetName>_<yyyyMMdd_HHmmss>.mp4`

Example:
- `sample_fifties-film_20260424_182305.mp4`

## Screenshot / Visual Aid Refresh Checklist
Use this checklist when updating docs visuals:
1. Capture top bar with Help, title, version.
2. Capture Images and Videos tabs side-by-side with preview pane.
3. Capture Videos settings dialog showing output folder + storage options.
4. Capture effects panel with ON/OFF toggles and disabled row example.
5. Capture mini preview controls with frame indicator visible.
6. Capture anchored export section + status bar at bottom.

Recommended screenshot notes:
- Use the sample image loaded.
- Include one screenshot where storage indicator is yellow or red.
- Include one screenshot showing frame scrubbing in progress.
