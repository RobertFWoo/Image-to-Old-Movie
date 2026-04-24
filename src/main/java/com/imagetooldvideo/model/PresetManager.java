package com.imagetooldvideo.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages custom presets, loading and saving them to a JSON file in the app directory.
 * Built-in presets (from Preset enum) are always available and cannot be deleted.
 */
public class PresetManager {
    private static final String PRESETS_FILE = "presets.json";
    private final Map<String, PresetData> customPresets = new LinkedHashMap<>();
    private final Path appDir;

    public record PresetData(
            String name,
            EffectMode lightLeakMode, int lightLeakVar, int lightLeakFreq, int lightLeakRand,
            EffectMode vignetteMode, int vignetteVar, int vignetteFreq, int vignetteRand,
            EffectMode grainMode, int grainVar, int grainFreq, int grainRand,
            EffectMode dustMode, int dustVar, int dustFreq, int dustRand,
            EffectMode scratchesMode, int scratchesVar, int scratchesFreq, int scratchesRand,
            EffectMode trackingMode, int trackingVar, int trackingFreq, int trackingRand,
            EffectMode flickerMode, int flickerVar, int flickerFreq, int flickerRand) {
    }

    public PresetManager(Path appDir) {
        this.appDir = appDir;
        loadCustomPresets();
    }

    /**
     * Loads custom presets from presets.json file.
     */
    private void loadCustomPresets() {
        Path presetsPath = appDir.resolve(PRESETS_FILE);
        if (!Files.exists(presetsPath)) {
            return;
        }

        try {
            String json = Files.readString(presetsPath);
            parsePresetsJson(json);
        } catch (IOException e) {
            System.err.println("Error loading presets: " + e.getMessage());
        }
    }

    /**
     * Simple JSON parser for presets array.
     * Expected format: {"presets": [{...}, {...}]}
     */
    private void parsePresetsJson(String json) {
        // Very simple manual JSON parsing
        int presetsStart = json.indexOf("[");
        int presetsEnd = json.lastIndexOf("]");
        if (presetsStart == -1 || presetsEnd == -1) return;

        String presetsArray = json.substring(presetsStart + 1, presetsEnd);
        // Split by },{  but be careful with nesting
        List<String> presetObjects = splitJsonObjects(presetsArray);

        for (String obj : presetObjects) {
            try {
                PresetData data = parsePresetObject(obj.trim());
                if (data != null) {
                    customPresets.put(data.name, data);
                }
            } catch (Exception e) {
                System.err.println("Error parsing preset: " + e.getMessage());
            }
        }
    }

    /**
     * Split JSON objects in array by },{ pattern, being careful with nesting.
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
                // Skip }, if present
                if (i + 1 < arrayContent.length() && arrayContent.charAt(i + 1) == ',') {
                    i++;
                }
                // Skip whitespace
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
     * Parse a single preset object from JSON.
     */
    private PresetData parsePresetObject(String json) {
        Map<String, String> values = extractJsonFields(json);
        if (!values.containsKey("name")) {
            return null;
        }

        String name = values.get("name");
        return new PresetData(
                name,
                EffectMode.valueOf(values.getOrDefault("lightLeakMode", "NONE")),
                parseInt(values, "lightLeakVar", 0),
                parseInt(values, "lightLeakFreq", 0),
                parseInt(values, "lightLeakRand", 0),
                EffectMode.valueOf(values.getOrDefault("vignetteMode", "NONE")),
                parseInt(values, "vignetteVar", 0),
                parseInt(values, "vignetteFreq", 0),
                parseInt(values, "vignetteRand", 0),
                EffectMode.valueOf(values.getOrDefault("grainMode", "NONE")),
                parseInt(values, "grainVar", 0),
                parseInt(values, "grainFreq", 0),
                parseInt(values, "grainRand", 0),
                EffectMode.valueOf(values.getOrDefault("dustMode", "NONE")),
                parseInt(values, "dustVar", 0),
                parseInt(values, "dustFreq", 0),
                parseInt(values, "dustRand", 0),
                EffectMode.valueOf(values.getOrDefault("scratchesMode", "NONE")),
                parseInt(values, "scratchesVar", 0),
                parseInt(values, "scratchesFreq", 0),
                parseInt(values, "scratchesRand", 0),
                EffectMode.valueOf(values.getOrDefault("trackingMode", "NONE")),
                parseInt(values, "trackingVar", 0),
                parseInt(values, "trackingFreq", 0),
                parseInt(values, "trackingRand", 0),
                EffectMode.valueOf(values.getOrDefault("flickerMode", "NONE")),
                parseInt(values, "flickerVar", 0),
                parseInt(values, "flickerFreq", 0),
                parseInt(values, "flickerRand", 0)
        );
    }

    /**
     * Extract field=value pairs from a JSON object string.
     */
    private Map<String, String> extractJsonFields(String json) {
        Map<String, String> fields = new HashMap<>();
        json = json.replaceAll("[{}\\[\\]]", "");

        String[] pairs = json.split(",");
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

    /**
     * Get a list of all custom preset names.
     */
    public List<String> getCustomPresetNames() {
        return new ArrayList<>(customPresets.keySet());
    }

    /**
     * Get custom preset data by name.
     */
    public PresetData getCustomPreset(String name) {
        return customPresets.get(name);
    }

    /**
     * Save or update a custom preset.
     */
    public void savePreset(String name, PresetData data) {
        customPresets.put(name, data);
        persistPresets();
    }

    /**
     * Delete a custom preset by name.
     */
    public void deletePreset(String name) {
        customPresets.remove(name);
        persistPresets();
    }

    /**
     * Persist all custom presets to JSON file.
     */
    private void persistPresets() {
        try {
            Files.createDirectories(appDir);
            Path presetsPath = appDir.resolve(PRESETS_FILE);
            String json = buildPresetsJson();
            Files.writeString(presetsPath, json);
        } catch (IOException e) {
            System.err.println("Error saving presets: " + e.getMessage());
        }
    }

    /**
     * Build JSON string from custom presets.
     */
    private String buildPresetsJson() {
        StringBuilder sb = new StringBuilder("{\n  \"presets\": [\n");

        List<String> presetLines = new ArrayList<>();
        for (PresetData data : customPresets.values()) {
            presetLines.add(buildPresetJson(data));
        }

        sb.append(String.join(",\n", presetLines));
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    /**
     * Build JSON for a single preset.
     */
    private String buildPresetJson(PresetData data) {
        return String.format(
                "    {\"name\": \"%s\", \"lightLeakMode\": \"%s\", \"lightLeakVar\": %d, \"lightLeakFreq\": %d, \"lightLeakRand\": %d, " +
                        "\"vignetteMode\": \"%s\", \"vignetteVar\": %d, \"vignetteFreq\": %d, \"vignetteRand\": %d, " +
                        "\"grainMode\": \"%s\", \"grainVar\": %d, \"grainFreq\": %d, \"grainRand\": %d, " +
                        "\"dustMode\": \"%s\", \"dustVar\": %d, \"dustFreq\": %d, \"dustRand\": %d, " +
                        "\"scratchesMode\": \"%s\", \"scratchesVar\": %d, \"scratchesFreq\": %d, \"scratchesRand\": %d, " +
                        "\"trackingMode\": \"%s\", \"trackingVar\": %d, \"trackingFreq\": %d, \"trackingRand\": %d, " +
                        "\"flickerMode\": \"%s\", \"flickerVar\": %d, \"flickerFreq\": %d, \"flickerRand\": %d}",
                data.name,
                data.lightLeakMode, data.lightLeakVar, data.lightLeakFreq, data.lightLeakRand,
                data.vignetteMode, data.vignetteVar, data.vignetteFreq, data.vignetteRand,
                data.grainMode, data.grainVar, data.grainFreq, data.grainRand,
                data.dustMode, data.dustVar, data.dustFreq, data.dustRand,
                data.scratchesMode, data.scratchesVar, data.scratchesFreq, data.scratchesRand,
                data.trackingMode, data.trackingVar, data.trackingFreq, data.trackingRand,
                data.flickerMode, data.flickerVar, data.flickerFreq, data.flickerRand
        );
    }

    /**
     * Check if a preset name is a built-in preset (cannot be deleted).
     */
    public static boolean isBuiltInPreset(String name) {
        for (Preset p : Preset.values()) {
            if (p.toString().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
