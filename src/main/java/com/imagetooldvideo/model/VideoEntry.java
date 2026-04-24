package com.imagetooldvideo.model;

/**
 * Immutable snapshot of a generated video tracked by the video gallery.
 */
public record VideoEntry(String id, String path, String name, String sourceImagePath,
                         long sizeBytes, long createdAt) {

    public VideoEntry withName(String newName) {
        return new VideoEntry(id, path, newName, sourceImagePath, sizeBytes, createdAt);
    }
}
