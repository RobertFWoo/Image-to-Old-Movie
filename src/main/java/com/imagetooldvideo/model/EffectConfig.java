package com.imagetooldvideo.model;

public class EffectConfig {

    /**
     * @param mode       whether this effect is active
     * @param variance   0-100: overall intensity / amplitude of the effect
     * @param frequency  0-100: how often / how fast the effect changes over time
     * @param randomness 0-100: how much the intensity and timing deviate from their
     *                   baseline each frame — 0 = deterministic, 100 = fully chaotic
     */
    public record EffectSetting(EffectMode mode, int variance, int frequency, int randomness) {
    }

    private final Preset preset;
    private final EffectSetting lightLeak;
    private final EffectSetting vignette;
    private final EffectSetting grain;
    private final EffectSetting dust;
    private final EffectSetting scratches;
    /** Horizontal tracking — sprocket jitter (side-to-side). */
    private final EffectSetting trackingH;
    /** Vertical tracking — film shudder / speed variation (up-and-down). */
    private final EffectSetting trackingV;
    private final EffectSetting flicker;

    public EffectConfig(
            Preset preset,
            EffectSetting lightLeak,
            EffectSetting vignette,
            EffectSetting grain,
            EffectSetting dust,
            EffectSetting scratches,
            EffectSetting trackingH,
            EffectSetting trackingV,
            EffectSetting flicker) {
        this.preset = preset;
        this.lightLeak = lightLeak;
        this.vignette = vignette;
        this.grain = grain;
        this.dust = dust;
        this.scratches = scratches;
        this.trackingH = trackingH;
        this.trackingV = trackingV;
        this.flicker = flicker;
    }

    public Preset getPreset() {
        return preset;
    }

    public EffectSetting getLightLeak() {
        return lightLeak;
    }

    public EffectSetting getVignette() {
        return vignette;
    }

    public EffectSetting getGrain() {
        return grain;
    }

    public EffectSetting getDust() {
        return dust;
    }

    public EffectSetting getScratches() {
        return scratches;
    }

    public EffectSetting getTrackingH() {
        return trackingH;
    }

    public EffectSetting getTrackingV() {
        return trackingV;
    }

    public EffectSetting getFlicker() {
        return flicker;
    }
}
