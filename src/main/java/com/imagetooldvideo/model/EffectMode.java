package com.imagetooldvideo.model;

public enum EffectMode {
    NONE("None"),
    ON("On");

    private final String label;

    EffectMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
