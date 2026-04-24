package com.imagetooldvideo.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages the user's image gallery.
 * Entries are persisted to {@code gallery.json} in the working directory.
 * Uses hand-rolled JSON to avoid any external dependency.
 */
public class GalleryManager {

    private static final String GALLERY_FILE = "gallery.json";

    private final Path filePath;
    private final List<GalleryEntry> entries = new ArrayList<>();

    public GalleryManager() {
        this.filePath = Path.of(GALLERY_FILE);
        load();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns an unmodifiable snapshot of the current gallery (most-recently-added first). */
    public List<GalleryEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Adds an image to the gallery.  If an entry with the same absolute path
     * already exists it is moved to the front and its name is updated.
     */
    public void addEntry(String absolutePath, String displayName) {
        entries.removeIf(e -> e.path().equals(absolutePath));
        entries.add(0, new GalleryEntry(
                UUID.randomUUID().toString(),
                absolutePath,
                displayName,
                "",
                System.currentTimeMillis()));
        save();
    }

    /** Replaces the stored entry that matches by id. */
    public void updateEntry(GalleryEntry updated) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(updated.id())) {
                entries.set(i, updated);
                save();
                return;
            }
        }
    }

    /** Removes the entry with the given id. */
    public void removeEntry(String id) {
        entries.removeIf(e -> e.id().equals(id));
        save();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void save() {
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < entries.size(); i++) {
                GalleryEntry e = entries.get(i);
                sb.append("  {")
                  .append("\"id\":\"").append(escJson(e.id())).append("\",")
                  .append("\"path\":\"").append(escJson(e.path())).append("\",")
                  .append("\"name\":\"").append(escJson(e.name())).append("\",")
                  .append("\"description\":\"").append(escJson(e.description())).append("\",")
                  .append("\"added\":").append(e.added())
                  .append("}");
                if (i < entries.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Gallery save failed: " + ex.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(filePath)) return;
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8).trim();
            if (json.isEmpty() || json.equals("[]")) return;
            // Strip outer brackets
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]"))  json = json.substring(0, json.length() - 1);
            int pos = 0;
            while (pos < json.length()) {
                int start = json.indexOf('{', pos);
                if (start == -1) break;
                int end = json.indexOf('}', start);
                if (end == -1) break;
                String block = json.substring(start + 1, end);
                String id   = extractStr(block, "id");
                String path = extractStr(block, "path");
                String name = extractStr(block, "name");
                String desc = extractStr(block, "description");
                long added  = extractLong(block, "added");
                if (id != null && path != null) {
                    entries.add(new GalleryEntry(
                            id, path,
                            name != null ? name : Path.of(path).getFileName().toString(),
                            desc != null ? desc : "",
                            added));
                }
                pos = end + 1;
            }
        } catch (IOException ex) {
            System.err.println("Gallery load failed: " + ex.getMessage());
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
        while (end < block.length() &&
               (Character.isDigit(block.charAt(end)) || block.charAt(end) == '-')) end++;
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
