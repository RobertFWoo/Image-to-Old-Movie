package com.imagetooldvideo.model;

/**
 * A single entry in the image gallery — an absolute file path plus
 * user-supplied display name and optional description.
 */
public record GalleryEntry(
        String id,
        String path,
        String name,
        String description,
        long added) {

    public GalleryEntry withName(String newName) {
        return new GalleryEntry(id, path, newName, description, added);
    }

    public GalleryEntry withDescription(String newDescription) {
        return new GalleryEntry(id, path, name, newDescription, added);
    }
}
