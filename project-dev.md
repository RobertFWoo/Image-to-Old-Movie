This project is a Java application that turns a still image into a short video with an old-film look.

The first version should stay simple. We are starting with image transformation and basic video generation only. Soundtrack conversion and audio effects will be added later.

## Phase 1 Goal

Build a prototype that:

1. Loads a single image from the user.
2. Applies a vintage visual effect, starting with sepia.
3. Generates a short video from that processed image.
4. Saves the video to the user's computer.
5. Provides a simple UI for selecting the image, previewing the effect, and exporting the video.

## Main Use Case

The main purpose of this project is to take a 1920 x 1080 still image, such as an intermission screen, and convert it into a video that looks like an old film.

The default output should be a 5-second video. The user can change the duration in the settings.

## User Interface

The app should have a simple, functional interface. It does not need to be fancy.

The UI should include:

1. A drag-and-drop area for an image file.
2. A preview area so the user can see the processed image before export.
3. A settings section for effect options and video length.
4. An export action to create the final video.

## Output Location

The app should save the generated video to the D: drive by default.

If D: is not available, it should fall back to the C: drive.

The user must also be able to choose a different output folder in settings.

## Planned Visual Effects

The first effect will be sepia. A "Faded" preset is also available — it keeps the original colors but adds a slight yellow tint, lifted blacks, and film grain to give the image an aged look without a full color replacement.

Later versions can add more old-video styling, such as:

1. Film grain or noise.
2. Color fading or tinting.
3. Flicker or slight frame variation.
4. Presets such as "20s Reel," "50s Film," and "80s VHS."

## Future Phase

Audio is not part of the first prototype.

In a later phase, we can add:

1. Background music.
2. Audio effects that make the soundtrack sound aged or theatrical.
3. Synchronization between the video and processed soundtrack.

## Technical Direction

The project should be modular and easy to maintain.

Suggested responsibilities:

1. Image processing.
2. Video creation.
3. User interface.
4. Audio processing in a later phase.

The first milestone is a working prototype that takes one image, applies a sepia filter, previews the result, and exports a short old-style video.

## Brainstorming - The thoughts below are brainstorming ideas and such. They will be progressive as the user thinks through some issues and features. Update the above sections as needed to reflect new decisions and directions.

Let's reduce the default for now, to 5 seconds.

The sepia is nice, but I want to retain the original colors with maybe a little fading and noise in the image. Make it look aged but not completely sepia. Maybe a "faded" preset that keeps the original colors but adds a slight yellow tint and some grain.

---

The controls need layout improvements. Some inputs are too narrow to display their values clearly, so widen those controls. The duration control should support a full range of 1 to 300 seconds (up to 5 minutes), with 5 seconds as the default.

Add dropdowns for visual effects, each with a `NONE` option so users can disable that effect. Candidate effects:

1. Lens flare / light leak (bright streaks or hotspots).
2. Vignette (darkened edges).
3. Grain (film-like noise).
4. Film scratches (random lines/spots).
5. Tracking variation (small shifts/jitter to mimic projector instability).
6. Flicker (subtle brightness changes like an old projector lamp).

Add a preset dropdown for quick vintage looks, such as "20s Reel," "50s Film," and "80s VHS."

For the "50s Film" preset, target a faded color style with grain and a slight vignette. The look should feel aged while still preserving the original colors.

Each effect dropdown can have two controls underneath:

1. Variance (0 to 100): controls effect strength.
2. Frequency (0 to 100): controls how often/rapidly the effect changes over time.

Example: for flicker, low frequency means slow, subtle brightness changes; high frequency means faster, more noticeable flicker.

Use this as creative guidance, not strict requirements. The goal is a flexible, fun tool that lets users generate old-style videos with customizable effects.

---

Okay, the 20s reel preset should reduce the image to a monochrome, grainy look with a strong vignette. This would be prior to color film, so the colors should be desaturated to black and white, with a lot of grain and a noticeable vignette to give it that old-timey feel.

The other presets should be appropriately styled based on the common visual characteristics of those eras. The 80s VHS preset could have a faded color palette with scan lines and a bit of tracking variation to mimic the look of old VHS tapes.

The variance is the intensity of the effect, so we need a randomness slider (the third slider) that controls how random are the effects. Randomness of 0 would be like the effect is the same each time it occurs, while randomness of 100 would make the effect completely different each time it occurs. This would allow users to create more dynamic and varied old-style videos, or keep the effects consistent for a more uniform look. Another way of thinking of randomness is that it can use the settings of the other two sliders as a baseline and randomly vary the effect around that baseline. So if you have a high variance but low randomness, the effect would be strong but consistent. If you have high variance and high randomness, the effect would be strong and very varied throughout the video. This gives users even more control over the final look of their old-style videos. Actually, randomness should override the other two sliders when the randomness is set, there would be that much randomness. I'm not sure how to describe what I'm thinking, but I would like for these effects to be more like an analogue change rather than yes or no. I would think of an audio wave that varies without digital on/off states, but instead has a continuous range of values that can be adjusted with the sliders. So the variance slider would control the overall intensity of the effect, while the randomness slider would add variation to that intensity over time. This way, users can create more nuanced and dynamic old-style videos that have a more authentic and less digital feel. So, let's think of variance as intensity, such as the amount the screen will brighten or darken for the flicker. The frequency would show how often the event occurs, and randomness will give variation to both those baselines. So if you have a high variance but low randomness, the effect would be strong but consistent. If you have high variance and high randomness, the effect would be strong and very varied throughout the video. This gives users even more control over the final look of their old-style videos. The randomness slider could also have a visual representation in the UI, such as a waveform or noise pattern that changes based on the slider value, to help users understand how it will affect the video. This would make it more intuitive for users to grasp how the randomness will influence the effects in their old-style videos.

---

By default, image-to-old-video\images\sample.jpg is a sample image 1920 x 1080 for this app. Use it unless the user drags another. or clicks on the browse button to select a different image. This way, users can quickly test the app without needing to find their own image first. The sample image should be a good representation of the type of content users might want to convert into an old-style video, such as a colorful landscape or a portrait. This allows users to immediately see the effects and features of the app in action, making it more engaging and easier to understand how to use the tool effectively.

---

Okay, looks good. but let's add another artifact to the mix. This will be dust specks and lint, which are common in old film. We can randomly generate small white or black specks that appear and disappear throughout the video, with controls for how many specks (density) and how long they last on screen (duration). This would add another layer of authenticity to the old-film look, as these imperfections are a hallmark of aged footage. The user could adjust the density to create a more heavily "damaged" look with lots of specks, or a cleaner look with fewer imperfections. The duration control would allow users to create either quick flashes of dust or longer-lasting specks that linger on the screen, further enhancing the vintage aesthetic of their old-style videos.
Also, the film scratches look too canned. I would like the see some crawl from one place to another over time from frame to frame. The way it is now, they just appear and vanish, but it should seem like a continuous scratch that moves across the screen, mimicking the way real scratches would appear on old film as it runs through a projector. This would make the scratches feel more natural and less like a digital overlay, adding to the overall authenticity of the old-film effect in the generated videos. It can just go a little ways or a longer ways and vary the direction and duration, so sometimes you get a quick scratch that zips across the screen, and other times you get a longer scratch that slowly crawls from one side to the other. This would create a more dynamic and realistic representation of film scratches in the old-style videos produced by the app.

---

The specks just flash on and off, but they need to be randomly located on the image, maybe yes, maybe no. A couple at a time here and there. It must look random, completely random. Also the scratch lines look bad. They are just small diagonal lines that pop up on the screen here and there. A Scratch on a frame should be a line fronm top to bottom, and depending on the crawl, it should move across the screen over time. So, let's say that the scratch is a vertical line at 90 degrees from the bottom edge. It will not move much, but the duration should be random. Just a few frames. As the angle increases either way, the speed at which it crawls will increase. They don't just appear and disappear, they typically crawl to one side or another depending on the angle of the scratch. So, let's say that the scratch begins in the center of the image as the start point, and is angled 45 degrees to the right, we will make a line to the top of the screen at that angle. Then the next frame will start at the bottom of the frame at the horizontal position of the previous frame's end point, and will continue at the same angle. This will create a more natural and dynamic representation of film scratches, as they will appear to move across the screen in a way that mimics real scratches on old film. The randomness of the duration and angle will add to the authenticity of the effect, making it feel less like a digital overlay and more like an organic imperfection in the footage. But these angled scratches should have a start point, maybe a gouge in the film, not too severe. Then it crawls toward one end or the other, but randomly, it may end before it reached the side. In this case, the line tapers to nothing at the random end point. Rather than a white line, it should be a partially opaque line that does not obscure the image completely but gives the impression of an artifact on the film. The opacity could vary based on the severity of the scratch, with some scratches being more faint and others being more pronounced, adding to the realism of the effect in the old-style videos produced by the app. This would create a more nuanced and authentic representation of film scratches, enhancing the vintage aesthetic of the generated videos while still allowing the underlying image to be visible through the imperfections.

---

Scratch behavior should be continuous across frames, not a new random line each frame.

For each scratch event:

1. Randomly choose how many frames the scratch will live.
2. Randomly choose a start point (x, y), scratch angle, and damage severity.
3. Draw the scratch from its start toward the top edge of the current frame at that angle.

Frame-to-frame continuation rules:

1. In the next frame, continue the same scratch from the bottom edge at the horizontal position where the previous frame's scratch reached the top edge.
2. Keep the same angle while the event is alive, so the line appears to crawl naturally left or right over time.
3. Near-vertical scratches should crawl slowly; less steep angles should crawl faster.

How a scratch ends:

1. A scratch may leave the screen on either side.
2. Or it may end on-screen in the final frame and taper to nothing at a random y-position.

Visual style by severity:

1. Minor damage: thinner, tapered, lower opacity.
2. Severe damage: thicker gouge, stronger opacity.
3. In all cases, scratches must remain partially transparent so the image is still visible underneath.

---

This is so much better scratches. 
---

## Phase 2: App Scaling - Progress Update (April 24, 2026)

### Completed

1. **Fixed Preset Dropdown Text Visibility** - Improved styling for better contrast
2. **Window Sizing** - Increased from 1240x780 to 1260x920 to accommodate new controls
3. **PresetManager Class** - Created for managing custom presets with JSON persistence (presets.json)
4. **ImageHistory Class** - Created for tracking image history and backups
5. **ProjectManager Class** - Created for managing projects (image + settings combinations)
6. **Image/Video Gallery Tabs** - Added separate gallery tabs with thumbnail previews and interactions
7. **Video Gallery Storage Management** - Added settings for output folder, max space, max count, plus capacity indicator
8. **Filename Convention** - Generated videos now include source image + preset + timestamp
9. **Preset Management UI** - Added Add/Update/Delete custom preset controls
10. **Compact Effect Toggles** - Replaced mode dropdowns with ON/OFF toggles and disabled states
11. **Persistent Split Divider** - Divider between preview section and preset/effects section is persisted
12. **Mini Video Preview Viewer** - Added frame stepping, play/pause, loop/one-pass, FPS (30/60), and 1-10 second preview range

### Next Steps

The infrastructure and core UI integration are now in place. Current priorities are refinement and quality:

**High Priority:**
- Add automated visual regression checks for key UI sections
- Add robust validation for custom preset names (duplicates/invalid chars)
- Expand preview performance tuning for larger images and high FPS mode

**Medium Priority:**
- Add project management UI (using the ProjectManager infrastructure)
- Implement unsaved changes detection and prompt when switching projects/images
- Add optional screenshot automation workflow for docs refresh

**Implementation Notes:**
- Core manager classes (PresetManager, ImageHistory, ProjectManager) build cleanly
- PresetManager is now integrated in MainWindow for custom preset lifecycle
- Storage-policy enforcement blocks export when at capacity
- Preview now supports temporal inspection, not only static image preview
- Divider state is persisted through Java Preferences

The updated UI architecture is implemented and documented in `USER_GUIDE.md`.

---

I need to update the JAVA_HOME env variable. I do this by going to System Properties > Environment Variables, then under System Variables, I find JAVA_HOME and update its value to the path of my JDK installation. For example, if my JDK is installed at C:\Program Files\Java\jdk-17, I would set JAVA_HOME to that path. After updating JAVA_HOME, I also need to update the PATH variable to include %JAVA_HOME%\bin so that I can run Java commands from the command line. Once I've made these changes, I should restart any open command prompts or IDEs to ensure they pick up the new environment variable settings. 

---

Now, let's make the progress bar turn green when the image is fully rendered, and clicking on it will launch the video using the default video player in the operating system.

---

Okay, now that that is done. Let's talk about tracking. Tracking is a common issue in old film, where the image can shift slightly from frame to frame due to imperfections in the film or projector. To mimic this effect, we can implement a tracking variation that randomly shifts the image horizontally and/or vertically by a small amount each frame. The user can control the intensity of the tracking variation with a slider, which would determine how much the image shifts on average. This would add another layer of authenticity to the old-film look, as it would create a more dynamic and less static video that feels more like real aged footage. The tracking variation could be subtle, with small shifts that are barely noticeable, or more pronounced for a more dramatic effect, depending on the user's preference. The randomness of the tracking shifts would also contribute to the vintage aesthetic, as it would create a more organic and less digital feel to the generated videos, further enhancing the overall old-style effect. This would allow users to create old-style videos that not only look aged but also have the characteristic imperfections of real old film, making the final product feel more authentic and engaging. Mostly the tracking wil be a slight jitter, as if the sprockets that hold the film in place are too small for the film's holes, causing it to shift slightly as it runs through the projector. This would create a subtle but effective tracking variation that adds to the vintage feel of the generated videos without being too distracting or overwhelming for viewers. The user could adjust the intensity of the tracking variation to find the right balance for their desired old-style look, allowing for a more personalized and customizable video creation experience. there is also an up and down motion caused by the film speed or shudder variations that cause the image to move vertically over time. This could be implemented as a vertical tracking variation that randomly shifts the image up and down by a small amount each frame, in addition to the horizontal tracking variation. This would further enhance the authenticity of the old-film effect, as it would mimic the common imperfections found in aged footage, creating a more dynamic and engaging video that feels truly vintage. The user could control both horizontal and vertical tracking variations independently, allowing for even more customization and personalization of the final old-style videos produced by the app.

---

I would like a Randomize button that will randomize all of the settings for the current setup. This is not tied to a particular preset but a feature that will allow the user to experiment with different combinations of effects and settings to quickly find interesting and unique old-style looks for their videos. When the user clicks the Randomize button, it would generate random values for all the effect settings (such as variance, frequency, randomness, tracking intensity, etc.) based on reasonable ranges for each setting. This would create a completely new and unexpected combination of effects each time, encouraging users to explore different styles and discover new ways to make their old-style videos look authentic and engaging. The Randomize feature would be a fun and creative tool that adds an element of surprise and experimentation to the video creation process, allowing users to easily generate unique vintage looks without having to manually adjust each setting. It would also be a great way for users to quickly test out different combinations of effects and find inspiration for their old-style videos, making the app more enjoyable and user-friendly. Also, as a button to reset all settings to zero.

---

I'm thinking that the Randomness slider might be obsolete, so let's define the variance and frenquency better. I think we can do away with randomness, because variance may cover the intensity of the effect, which frequency will cover the rate at which the effect occurs. So, for example, if you have a high variance for flicker, it would create strong brightness changes, while a low variance would create more subtle flicker. The frequency would determine how often those brightness changes occur, with a low frequency creating slow, gradual flicker and a high frequency creating fast, rapid flicker. This way, users can still achieve a wide range of dynamic and varied old-style looks by adjusting the variance and frequency sliders without needing an additional randomness control. The combination of variance and frequency would allow users to create nuanced and customizable effects that feel authentic and engaging in their old-style videos, while keeping the interface simpler and more intuitive without the need for a separate randomness slider.

---

I think I want the left top panel to be a gallery for stored images. The user can drag an image file into that panel to add it to the gallery. He can rename it, give it a description. When he clicks on an image in the gallery, it will load that image into the preview and apply the current settings to it. This way, users can easily manage and organize their images within the app, creating a personalized gallery of content that they can quickly access and use for their old-style video creations. The ability to rename and describe images in the gallery would also help users keep track of their content and find specific images more easily, enhancing the overall user experience and making the app more enjoyable to use. The gallery feature would add a new dimension to the app, allowing users to not only create old-style videos but also manage their image assets in a convenient and organized way, further enhancing the functionality and appeal of the tool.

---

Now, let's have an image gallery tab and a video gallery tab. The video gallery tab will show the videos that have been generated. Also include a setting menu to allow user to change the default output folder, the maximum drive space to use and/or the maximum number of videos to keep in the gallery before deleting old ones. This way, users can easily manage their generated videos and control how much storage space the app uses on their computer, ensuring that they can keep their video gallery organized and clutter-free while still having access to their old-style video creations. The settings menu would provide users with the flexibility to customize their experience and manage their content in a way that works best for them, enhancing the overall usability and appeal of the app. also have an indicator, a percentage of how full the budgetted storage space is, so users can keep track of how much space they have left for their generated videos and manage their content accordingly. make it yellow when the storage is almost full, and red when it is full. When it is full, the export video button will not work. Also in this video gallery, the user can drag items out of the gallery into another folder on their computer to save them permanently, or they can click on a video to play it in the default video player. This would make it easy for users to access and manage their generated videos, allowing them to quickly find and enjoy their old-style video creations while also keeping their storage organized and under control. This is just an idea. I want you to be free to add to this feature anything I might have missed in my description. I want this to be intuitive and include any necessary modifications to make this app useful and user-friendly, while still providing the core functionality of generating old-style videos from images and managing the resulting content effectively.

---

I like the way this app is turning out. I want the effects dropdown to be a white background with black text. Make all of the dropdowns the same style. I want the videos file names to include the original image name, preset used, and a timestamp. This way, users can easily identify the source image and settings used for each generated video, making it easier to organize and find specific videos in their gallery. The consistent styling of the dropdowns would also enhance the overall aesthetic of the app, creating a more cohesive and visually appealing user interface that complements the vintage theme of the tool. By including detailed information in the video file names, users can quickly understand the context of each video, making it more convenient to manage their content and share their old-style video creations with others.

---

in the video tab, I want as a thumbnail, the image used with a video symbol overlay (like a play button) to indicate that it is a video. When the user clicks on the thumbnail, it will play the video in the default video player. This would make it easy for users to visually identify their generated videos in the gallery and quickly access them for viewing, enhancing the overall user experience and making the app more enjoyable to use. The thumbnail with the video symbol overlay would provide a clear and intuitive way for users to navigate their video gallery, allowing them to easily find and enjoy their old-style video creations with just a click.   

---

We have made some changes. So we need to update the documentation to reflect the new features and UI changes. This includes updating the user guide to explain the new image and video gallery tabs, the settings menu for managing storage space, and the new file naming convention for generated videos. We also need to update any screenshots or visual aids in the documentation to match the current state of the app, ensuring that users have accurate and up-to-date information on how to use the tool effectively. Additionally, we should review the documentation for clarity and completeness, making sure that all new features are well-explained and that users can easily understand how to navigate the updated interface and manage their content within the app.
Then, Persist the location of the divider between the the preview and preset sections. Also, the preview should show not just the image but a sample frame that the user can advance forward or backward to get a sample of how the effects might look frame by frame. So we would need about one second worth of frames, i.e. 60 or 30. that the user can scroll forward and backward and do a quick preview when he plays the video. Let the preview be a mini video viewer that can move frame by frame or loop or one-pass. The controls for this should be under the preview itself with the frame number indicated so that when the preview cycles, it shows the frame number being shown at that time. Make this a cool preview of video footage with 1 to 10 seconds worth of frames to preview.

---

I would like to have an alpha channel opacity version of the preview that just shows the effects without the underlying image, so users can see the effect layers separately. This would allow users to better understand how each effect contributes to the final look of the video, giving them more insight into the creative process and helping them fine-tune their settings for the desired vintage aesthetic. The alpha channel preview could be toggled on and off, allowing users to easily switch between viewing the full preview with the image and effects combined, and the separate effects layer to analyze the individual contributions of each effect. This feature would enhance the user experience by providing a deeper level of control and understanding over the video creation process, making it easier for users to achieve their desired old-style look with precision and creativity. We should also be able to export only the effects with a transparent background without the image. This way, uses can overlay this transparent background video on top of other footage. We can also allow the user to import a video instead of an image and have the app apply the effects to the video, creating an old-style version of their original footage. This would expand the functionality of the app, allowing users to not only create old-style videos from still images but also transform their existing video content with vintage effects, further enhancing the versatility and appeal of the tool for a wider range of creative projects.