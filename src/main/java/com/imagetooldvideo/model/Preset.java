package com.imagetooldvideo.model;

public enum Preset {
    NONE("None"),
    TWENTIES_REEL("20s Reel"),
    FIFTIES_FILM("50s Film"),
    EIGHTIES_VHS("80s VHS"),
    FADED("Faded"),
    SEPIA("Sepia");

    private final String label;

    Preset(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
