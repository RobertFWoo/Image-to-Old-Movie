package com.imagetooldvideo.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists video gallery management preferences to {@code gallery-settings.json}.
 * All values are saved immediately whenever a setter is called.
 */
public class GallerySettings {

    private static final String SETTINGS_FILE  = "gallery-settings.json";
    /** 5 GB default space budget. */
    public  static final long   DEFAULT_SPACE  = 5L * 1024 * 1024 * 1024;
    public  static final int    DEFAULT_COUNT  = 50;

    private final Path filePath;
    private long maxSpaceBytes;   // -1 = unlimited
    private int  maxVideoCount;   // -1 = unlimited

    public GallerySettings() {
        this.filePath      = Path.of(SETTINGS_FILE);
        this.maxSpaceBytes = DEFAULT_SPACE;
        this.maxVideoCount = DEFAULT_COUNT;
        load();
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    /** Maximum bytes permitted for the video gallery.  {@code -1} = unlimited. */
    public long getMaxSpaceBytes()  { return maxSpaceBytes; }

    /** Maximum number of videos to keep.  {@code -1} = unlimited. */
    public int  getMaxVideoCount()  { return maxVideoCount; }

    public void setMaxSpaceBytes(long v)  { this.maxSpaceBytes = v; save(); }
    public void setMaxVideoCount(int  v)  { this.maxVideoCount = v; save(); }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void save() {
        try {
            String json = "{\"maxSpaceBytes\":" + maxSpaceBytes
                        + ",\"maxVideoCount\":" + maxVideoCount + "}";
            Files.writeString(filePath, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("GallerySettings save failed: " + ex.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(filePath)) return;
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            long sp = extractLong(json, "maxSpaceBytes");
            int  mc = (int) extractLong(json, "maxVideoCount");
            // 0 means the key was absent — leave default in place
            if (sp != 0) maxSpaceBytes = sp;
            if (mc != 0) maxVideoCount = mc;
        } catch (IOException ex) {
            System.err.println("GallerySettings load failed: " + ex.getMessage());
        }
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int start = idx + search.length();
        int end   = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }
}
