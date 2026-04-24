package com.imagetooldvideo.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoSettings {

    private static final int DEFAULT_DURATION = 5;
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 300;

    private int durationSeconds = DEFAULT_DURATION;
    private Path outputDirectory;

    public VideoSettings() {
        this.outputDirectory = resolveDefaultOutputDirectory();
    }

    /**
     * Resolves the default output folder.
     * Prefers D:\ImageToOldVideo; falls back to C:\ImageToOldVideo if D: is unavailable.
     */
    private Path resolveDefaultOutputDirectory() {
        Path dDrive = Paths.get("D:\\ImageToOldVideo");
        if (Files.exists(Paths.get("D:\\"))) {
            return dDrive;
        }
        return Paths.get("C:\\ImageToOldVideo");
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        if (durationSeconds < MIN_DURATION || durationSeconds > MAX_DURATION) {
            throw new IllegalArgumentException(
                    "Duration must be between " + MIN_DURATION + " and " + MAX_DURATION + " seconds.");
        }
        this.durationSeconds = durationSeconds;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory must not be null.");
        }
        this.outputDirectory = outputDirectory;
    }
}
