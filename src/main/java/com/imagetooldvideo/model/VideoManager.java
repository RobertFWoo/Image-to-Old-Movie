package com.imagetooldvideo.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Tracks generated videos and enforces the gallery retention policy.
 * Persists the entry list to {@code video-gallery.json} in the working directory.
 * Uses hand-rolled JSON to avoid any external dependency.
 *
 * <p>The list is kept newest-first.  Retention enforcement deletes the oldest
 * entries until both the count limit and the space limit are satisfied.
 */
public class VideoManager {

    private static final String GALLERY_FILE = "video-gallery.json";

    private final Path filePath;
    private final List<VideoEntry> entries = new ArrayList<>();

    public VideoManager() {
        this.filePath = Path.of(GALLERY_FILE);
        load();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns an unmodifiable snapshot of the gallery (newest first). */
    public List<VideoEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Returns the sum of {@code sizeBytes} for all entries whose files still exist. */
    public long getTotalUsedBytes() {
        return entries.stream()
                .filter(e -> new File(e.path()).exists())
                .mapToLong(VideoEntry::sizeBytes)
                .sum();
    }

    /**
     * Registers a newly exported video file, deduplicating by absolute path.
     * Call {@link #enforcePolicy} after this to trim old entries if needed.
     */
    public VideoEntry registerVideo(Path path, String name, String sourceImagePath) {
        String abs = path.toAbsolutePath().toString();
        entries.removeIf(e -> new File(e.path()).getAbsolutePath().equals(abs));
        long size = 0;
        try { size = Files.size(path); } catch (IOException ignored) {}
        String src = sourceImagePath != null ? sourceImagePath : "";
        VideoEntry entry = new VideoEntry(
                UUID.randomUUID().toString(), abs, name, src, size, System.currentTimeMillis());
        entries.add(0, entry);
        save();
        return entry;
    }

    /** Replaces the stored entry (matched by id), e.g. after a rename. */
    public void updateEntry(VideoEntry updated) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(updated.id())) {
                entries.set(i, updated);
                save();
                return;
            }
        }
    }

    /**
     * Removes an entry from the gallery.
     * @param deleteFile if {@code true}, also deletes the file from disk.
     */
    public void removeEntry(String id, boolean deleteFile) {
        entries.removeIf(e -> {
            if (!e.id().equals(id)) return false;
            if (deleteFile) {
                try { Files.deleteIfExists(Path.of(e.path())); }
                catch (IOException ignored) {}
            }
            return true;
        });
        save();
    }

    /**
     * Trims the oldest entries until both the count and space limits in
     * {@code settings} are satisfied.  Removed files are deleted from disk.
     * Returns the ids of the entries that were removed.
     */
    public List<String> enforcePolicy(GallerySettings settings) {
        List<String> removed = new ArrayList<>();

        // Keep newest first
        entries.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));

        int maxCount = settings.getMaxVideoCount();
        if (maxCount >= 0) {
            while (entries.size() > maxCount) {
                VideoEntry oldest = entries.remove(entries.size() - 1);
                deleteFile(oldest);
                removed.add(oldest.id());
            }
        }

        long maxBytes = settings.getMaxSpaceBytes();
        if (maxBytes >= 0) {
            while (getTotalUsedBytes() > maxBytes && !entries.isEmpty()) {
                VideoEntry oldest = entries.remove(entries.size() - 1);
                deleteFile(oldest);
                removed.add(oldest.id());
            }
        }

        if (!removed.isEmpty()) save();
        return removed;
    }

    /** {@code true} when the used space equals or exceeds the configured limit. */
    public boolean isSpaceAtCapacity(GallerySettings settings) {
        long max = settings.getMaxSpaceBytes();
        return max >= 0 && getTotalUsedBytes() >= max;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void deleteFile(VideoEntry e) {
        try { Files.deleteIfExists(Path.of(e.path())); }
        catch (IOException ignored) {}
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void save() {
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < entries.size(); i++) {
                VideoEntry e = entries.get(i);
                sb.append("  {")
                  .append("\"id\":\"").append(escJson(e.id())).append("\",")
                  .append("\"path\":\"").append(escJson(e.path())).append("\",")
                  .append("\"name\":\"").append(escJson(e.name())).append("\",")
                  .append("\"sourceImagePath\":\"").append(escJson(e.sourceImagePath() != null ? e.sourceImagePath() : "")).append("\",")
                  .append("\"sizeBytes\":").append(e.sizeBytes()).append(",")
                  .append("\"createdAt\":").append(e.createdAt())
                  .append("}");
                if (i < entries.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("VideoManager save failed: " + ex.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(filePath)) return;
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8).trim();
            if (json.isEmpty() || json.equals("[]")) return;
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]"))  json = json.substring(0, json.length() - 1);
            int pos = 0;
            while (pos < json.length()) {
                int start = json.indexOf('{', pos);
                if (start == -1) break;
                int end = json.indexOf('}', start);
                if (end == -1) break;
                String block = json.substring(start + 1, end);
                String id      = extractStr(block,  "id");
                String path    = extractStr(block,  "path");
                String name    = extractStr(block,  "name");
                String srcImg  = extractStr(block,  "sourceImagePath");
                long   size    = extractLong(block, "sizeBytes");
                long   ts      = extractLong(block, "createdAt");
                // Skip entries whose files have been deleted outside the app
                if (id != null && path != null && new File(path).exists()) {
                    entries.add(new VideoEntry(
                            id, path,
                            name != null ? name : Path.of(path).getFileName().toString(),
                            srcImg != null ? srcImg : "",
                            size, ts));
                }
                pos = end + 1;
            }
        } catch (IOException ex) {
            System.err.println("VideoManager load failed: " + ex.getMessage());
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static String extractStr(String block, String key) {
        String search = "\"" + key + "\":\"";
        int idx = block.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < block.length(); i++) {
            char c = block.charAt(i);
            if (c == '\\' && i + 1 < block.length()) {
                char next = block.charAt(++i);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long extractLong(String block, String key) {
        String search = "\"" + key + "\":";
        int idx = block.indexOf(search);
        if (idx == -1) return 0L;
        int start = idx + search.length();
        while (start < block.length() && block.charAt(start) == ' ') start++;
        int end = start;
        while (end < block.length()
                && (Character.isDigit(block.charAt(end)) || block.charAt(end) == '-')) end++;
        try { return Long.parseLong(block.substring(start, end)); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static String escJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n",  "\\n")
                .replace("\r",  "\\r");
    }
}
