package com.imagetooldvideo.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages image storage and history for the application.
 * Images are stored in a configurable folder (default: images/custom).
 * Maintains a history of recently loaded images.
 */
public class ImageHistory {
    private static final String HISTORY_FILE = "image-history.json";
    private static final String DEFAULT_IMAGE_FOLDER = "images/custom";
    private static final int MAX_HISTORY_ITEMS = 20;

    private final List<ImageHistoryItem> history = new ArrayList<>();
    private Path imageFolder;
    private final Path appDir;

    public record ImageHistoryItem(
            String id,
            String filename,
            String path,
            long timestamp,
            int width,
            int height) {
    }

    public ImageHistory(Path appDir) {
        this.appDir = appDir;
        this.imageFolder = appDir.resolve(DEFAULT_IMAGE_FOLDER);
        loadHistory();
    }

    /**
     * Get the current image storage folder.
     */
    public Path getImageFolder() {
        return imageFolder;
    }

    /**
     * Set a custom image folder.
     */
    public void setImageFolder(Path folder) {
        this.imageFolder = folder;
        persistHistory();
    }

    /**
     * Add or update an image in history.
     */
    public void addImage(String filename, Path filePath, int width, int height) {
        // Remove existing entry if present
        history.removeIf(item -> item.filename.equals(filename));

        // Add new entry
        String id = UUID.randomUUID().toString();
        ImageHistoryItem item = new ImageHistoryItem(
                id,
                filename,
                filePath.toString(),
                System.currentTimeMillis(),
                width,
                height
        );
        history.add(0, item); // Add to front

        // Keep only last MAX_HISTORY_ITEMS
        while (history.size() > MAX_HISTORY_ITEMS) {
            history.remove(history.size() - 1);
        }

        persistHistory();
    }

    /**
     * Get image history list (most recent first).
     */
    public List<ImageHistoryItem> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Get an image from history by filename.
     */
    public ImageHistoryItem getHistoryItem(String filename) {
        return history.stream()
                .filter(item -> item.filename.equals(filename))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove an image from history.
     */
    public void removeFromHistory(String filename) {
        history.removeIf(item -> item.filename.equals(filename));
        persistHistory();
    }

    /**
     * Create a backup of an image when it's being replaced.
     */
    public void createImageBackup(Path originalPath, String imageName) {
        try {
            Files.createDirectories(imageFolder);
            Path backupPath = imageFolder.resolve(imageName);
            Files.copy(originalPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error creating image backup: " + e.getMessage());
        }
    }

    /**
     * Load history from file.
     */
    private void loadHistory() {
        Path historyPath = appDir.resolve(HISTORY_FILE);
        if (!Files.exists(historyPath)) {
            return;
        }

        try {
            String json = Files.readString(historyPath);
            parseHistoryJson(json);
        } catch (IOException e) {
            System.err.println("Error loading image history: " + e.getMessage());
        }
    }

    /**
     * Parse history JSON.
     */
    private void parseHistoryJson(String json) {
        // Simple parsing
        json = json.replace("\"imageFolder\": \"", "");
        int folderEnd = json.indexOf("\"");
        if (folderEnd > 0) {
            String folder = json.substring(0, folderEnd);
            try {
                imageFolder = Path.of(folder);
            } catch (Exception e) {
                // Keep default
            }
        }

        int itemsStart = json.indexOf("[");
        int itemsEnd = json.lastIndexOf("]");
        if (itemsStart == -1 || itemsEnd == -1) return;

        String itemsArray = json.substring(itemsStart + 1, itemsEnd);
        List<String> items = splitJsonObjects(itemsArray);

        for (String item : items) {
            try {
                ImageHistoryItem parsed = parseHistoryItem(item.trim());
                if (parsed != null) {
                    history.add(parsed);
                }
            } catch (Exception e) {
                System.err.println("Error parsing history item: " + e.getMessage());
            }
        }
    }

    /**
     * Parse a single history item.
     */
    private ImageHistoryItem parseHistoryItem(String json) {
        Map<String, String> values = extractJsonFields(json);
        return new ImageHistoryItem(
                values.get("id"),
                values.get("filename"),
                values.get("path"),
                parseLong(values, "timestamp", System.currentTimeMillis()),
                parseInt(values, "width", 1920),
                parseInt(values, "height", 1080)
        );
    }

    /**
     * Split JSON objects in array.
     */
    private List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;

            current.append(c);

            if (c == '}' && depth == 0) {
                objects.add(current.toString());
                current = new StringBuilder();
                if (i + 1 < arrayContent.length() && arrayContent.charAt(i + 1) == ',') {
                    i++;
                }
                while (i + 1 < arrayContent.length() && Character.isWhitespace(arrayContent.charAt(i + 1))) {
                    i++;
                }
            }
        }
        if (current.length() > 0) {
            objects.add(current.toString());
        }
        return objects;
    }

    /**
     * Extract fields from JSON object.
     */
    private Map<String, String> extractJsonFields(String json) {
        Map<String, String> fields = new HashMap<>();
        json = json.replaceAll("[{}\\[\\]]", "");

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            pair = pair.trim();
            int colonIdx = pair.indexOf(":");
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim().replaceAll("\"", "");
                String value = pair.substring(colonIdx + 1).trim().replaceAll("\"", "");
                fields.put(key, value);
            }
        }
        return fields;
    }

    private int parseInt(Map<String, String> map, String key, int defaultValue) {
        try {
            return Integer.parseInt(map.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long parseLong(Map<String, String> map, String key, long defaultValue) {
        try {
            return Long.parseLong(map.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Persist history to file.
     */
    private void persistHistory() {
        try {
            Files.createDirectories(appDir);
            Path historyPath = appDir.resolve(HISTORY_FILE);
            String json = buildHistoryJson();
            Files.writeString(historyPath, json);
        } catch (IOException e) {
            System.err.println("Error saving image history: " + e.getMessage());
        }
    }

    /**
     * Build JSON string from history.
     */
    private String buildHistoryJson() {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append(String.format("  \"imageFolder\": \"%s\",\n", imageFolder.toString().replace("\\", "\\\\")));
        sb.append("  \"history\": [\n");

        List<String> itemLines = new ArrayList<>();
        for (ImageHistoryItem item : history) {
            itemLines.add(buildHistoryItemJson(item));
        }

        sb.append(String.join(",\n", itemLines));
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    /**
     * Build JSON for a single history item.
     */
    private String buildHistoryItemJson(ImageHistoryItem item) {
        return String.format(
                "    {\"id\": \"%s\", \"filename\": \"%s\", \"path\": \"%s\", \"timestamp\": %d, \"width\": %d, \"height\": %d}",
                item.id,
                item.filename.replace("\"", "\\\""),
                item.path.replace("\\", "\\\\").replace("\"", "\\\""),
                item.timestamp,
                item.width,
                item.height
        );
    }
}
