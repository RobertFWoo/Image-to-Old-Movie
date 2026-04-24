package com.imagetooldvideo.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages projects (combination of image + effect settings) for the application.
 * Projects are persisted to a JSON file in the app directory.
 */
public class ProjectManager {
    private static final String PROJECTS_FILE = "projects.json";

    private final Map<String, ProjectData> projects = new LinkedHashMap<>();
    private String currentProjectName;
    private boolean hasUnsavedChanges;
    private final Path appDir;

    public record ProjectData(
            String name,
            String imagePath,
            String imageFilename,
            Preset preset,
            EffectMode lightLeakMode, int lightLeakVar, int lightLeakFreq, int lightLeakRand,
            EffectMode vignetteMode, int vignetteVar, int vignetteFreq, int vignetteRand,
            EffectMode grainMode, int grainVar, int grainFreq, int grainRand,
            EffectMode dustMode, int dustVar, int dustFreq, int dustRand,
            EffectMode scratchesMode, int scratchesVar, int scratchesFreq, int scratchesRand,
            EffectMode trackingMode, int trackingVar, int trackingFreq, int trackingRand,
            EffectMode flickerMode, int flickerVar, int flickerFreq, int flickerRand,
            int durationSeconds,
            long timestamp) {
    }

    public ProjectManager(Path appDir) {
        this.appDir = appDir;
        this.hasUnsavedChanges = false;
        loadProjects();
    }

    /**
     * Load projects from file.
     */
    private void loadProjects() {
        Path projectsPath = appDir.resolve(PROJECTS_FILE);
        if (!Files.exists(projectsPath)) {
            return;
        }

        try {
            String json = Files.readString(projectsPath);
            parseProjectsJson(json);
        } catch (IOException e) {
            System.err.println("Error loading projects: " + e.getMessage());
        }
    }

    /**
     * Parse projects JSON.
     */
    private void parseProjectsJson(String json) {
        int projectsStart = json.indexOf("[");
        int projectsEnd = json.lastIndexOf("]");
        if (projectsStart == -1 || projectsEnd == -1) return;

        String projectsArray = json.substring(projectsStart + 1, projectsEnd);
        List<String> projectObjects = splitJsonObjects(projectsArray);

        for (String obj : projectObjects) {
            try {
                ProjectData data = parseProjectObject(obj.trim());
                if (data != null) {
                    projects.put(data.name, data);
                }
            } catch (Exception e) {
                System.err.println("Error parsing project: " + e.getMessage());
            }
        }
    }

    /**
     * Parse a single project object.
     */
    private ProjectData parseProjectObject(String json) {
        Map<String, String> values = extractJsonFields(json);
        if (!values.containsKey("name")) {
            return null;
        }

        return new ProjectData(
                values.get("name"),
                values.get("imagePath"),
                values.get("imageFilename"),
                Preset.valueOf(values.getOrDefault("preset", "FIFTIES_FILM")),
                EffectMode.valueOf(values.getOrDefault("lightLeakMode", "NONE")), parseInt(values, "lightLeakVar", 0), parseInt(values, "lightLeakFreq", 0), parseInt(values, "lightLeakRand", 0),
                EffectMode.valueOf(values.getOrDefault("vignetteMode", "NONE")), parseInt(values, "vignetteVar", 0), parseInt(values, "vignetteFreq", 0), parseInt(values, "vignetteRand", 0),
                EffectMode.valueOf(values.getOrDefault("grainMode", "NONE")), parseInt(values, "grainVar", 0), parseInt(values, "grainFreq", 0), parseInt(values, "grainRand", 0),
                EffectMode.valueOf(values.getOrDefault("dustMode", "NONE")), parseInt(values, "dustVar", 0), parseInt(values, "dustFreq", 0), parseInt(values, "dustRand", 0),
                EffectMode.valueOf(values.getOrDefault("scratchesMode", "NONE")), parseInt(values, "scratchesVar", 0), parseInt(values, "scratchesFreq", 0), parseInt(values, "scratchesRand", 0),
                EffectMode.valueOf(values.getOrDefault("trackingMode", "NONE")), parseInt(values, "trackingVar", 0), parseInt(values, "trackingFreq", 0), parseInt(values, "trackingRand", 0),
                EffectMode.valueOf(values.getOrDefault("flickerMode", "NONE")), parseInt(values, "flickerVar", 0), parseInt(values, "flickerFreq", 0), parseInt(values, "flickerRand", 0),
                parseInt(values, "durationSeconds", 5),
                parseLong(values, "timestamp", System.currentTimeMillis())
        );
    }

    /**
     * Split JSON objects.
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
     * Extract JSON fields.
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
     * Get all project names.
     */
    public List<String> getProjectNames() {
        return new ArrayList<>(projects.keySet());
    }

    /**
     * Get current project name.
     */
    public String getCurrentProjectName() {
        return currentProjectName;
    }

    /**
     * Set current project.
     */
    public void setCurrentProject(String name) {
        currentProjectName = name;
        hasUnsavedChanges = false;
    }

    /**
     * Get project data by name.
     */
    public ProjectData getProject(String name) {
        return projects.get(name);
    }

    /**
     * Create a new project.
     */
    public void createProject(String name, ProjectData data) {
        projects.put(name, data);
        setCurrentProject(name);
        persistProjects();
    }

    /**
     * Update current project with new data.
     */
    public void updateCurrentProject(ProjectData data) {
        if (currentProjectName != null) {
            projects.put(currentProjectName, data);
            persistProjects();
        }
    }

    /**
     * Delete a project.
     */
    public void deleteProject(String name) {
        projects.remove(name);
        if (name.equals(currentProjectName)) {
            currentProjectName = null;
        }
        persistProjects();
    }

    /**
     * Check if there are unsaved changes.
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * Mark that changes have been made.
     */
    public void markUnsavedChanges() {
        hasUnsavedChanges = true;
    }

    /**
     * Clear unsaved changes flag.
     */
    public void clearUnsavedChanges() {
        hasUnsavedChanges = false;
    }

    /**
     * Persist projects to file.
     */
    private void persistProjects() {
        try {
            Files.createDirectories(appDir);
            Path projectsPath = appDir.resolve(PROJECTS_FILE);
            String json = buildProjectsJson();
            Files.writeString(projectsPath, json);
        } catch (IOException e) {
            System.err.println("Error saving projects: " + e.getMessage());
        }
    }

    /**
     * Build JSON from projects.
     */
    private String buildProjectsJson() {
        StringBuilder sb = new StringBuilder("{\n  \"projects\": [\n");

        List<String> projectLines = new ArrayList<>();
        for (ProjectData data : projects.values()) {
            projectLines.add(buildProjectJson(data));
        }

        sb.append(String.join(",\n", projectLines));
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    /**
     * Build JSON for a single project.
     */
    private String buildProjectJson(ProjectData data) {
        return String.format(
                "    {\"name\": \"%s\", \"imagePath\": \"%s\", \"imageFilename\": \"%s\", \"preset\": \"%s\", " +
                        "\"lightLeakMode\": \"%s\", \"lightLeakVar\": %d, \"lightLeakFreq\": %d, \"lightLeakRand\": %d, " +
                        "\"vignetteMode\": \"%s\", \"vignetteVar\": %d, \"vignetteFreq\": %d, \"vignetteRand\": %d, " +
                        "\"grainMode\": \"%s\", \"grainVar\": %d, \"grainFreq\": %d, \"grainRand\": %d, " +
                        "\"dustMode\": \"%s\", \"dustVar\": %d, \"dustFreq\": %d, \"dustRand\": %d, " +
                        "\"scratchesMode\": \"%s\", \"scratchesVar\": %d, \"scratchesFreq\": %d, \"scratchesRand\": %d, " +
                        "\"trackingMode\": \"%s\", \"trackingVar\": %d, \"trackingFreq\": %d, \"trackingRand\": %d, " +
                        "\"flickerMode\": \"%s\", \"flickerVar\": %d, \"flickerFreq\": %d, \"flickerRand\": %d, " +
                        "\"durationSeconds\": %d, \"timestamp\": %d}",
                data.name,
                data.imagePath.replace("\\", "\\\\").replace("\"", "\\\""),
                data.imageFilename.replace("\"", "\\\""),
                data.preset,
                data.lightLeakMode, data.lightLeakVar, data.lightLeakFreq, data.lightLeakRand,
                data.vignetteMode, data.vignetteVar, data.vignetteFreq, data.vignetteRand,
                data.grainMode, data.grainVar, data.grainFreq, data.grainRand,
                data.dustMode, data.dustVar, data.dustFreq, data.dustRand,
                data.scratchesMode, data.scratchesVar, data.scratchesFreq, data.scratchesRand,
                data.trackingMode, data.trackingVar, data.trackingFreq, data.trackingRand,
                data.flickerMode, data.flickerVar, data.flickerFreq, data.flickerRand,
                data.durationSeconds,
                data.timestamp
        );
    }
}
