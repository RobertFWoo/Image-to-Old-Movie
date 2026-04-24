package com.imagetooldvideo.processing;

import com.imagetooldvideo.model.EffectConfig;
import com.imagetooldvideo.model.EffectMode;
import com.imagetooldvideo.model.Preset;

import java.awt.image.BufferedImage;
import java.util.Random;

public class ImageProcessor {

    private record DustParticle(int x, int y, int radius, boolean bright) {
    }

    /**
     * Applies a sepia tone filter to the given image.
     * Uses the standard sepia matrix to transform each pixel's RGB values.
     *
     * @param source the original image
     * @return a new BufferedImage with the sepia effect applied
     */
    public BufferedImage applySepia(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int sepiaR = clamp((int) (r * 0.393 + g * 0.769 + b * 0.189));
                int sepiaG = clamp((int) (r * 0.349 + g * 0.686 + b * 0.168));
                int sepiaB = clamp((int) (r * 0.272 + g * 0.534 + b * 0.131));

                result.setRGB(x, y, (sepiaR << 16) | (sepiaG << 8) | sepiaB);
            }
        }

        return result;
    }

    /**
     * Crops the image so both width and height are divisible by 2.
     * Required by H.264 encoding (macroblocks must have even dimensions).
     *
     * @param image the source image
     * @return the original image if already even; otherwise a cropped copy
     */
    public BufferedImage ensureEvenDimensions(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int newW = w - (w % 2);
        int newH = h - (h % 2);
        if (newW == w && newH == h) {
            return image;
        }
        return image.getSubimage(0, 0, newW, newH);
    }

    private int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }

    private static float clampUnit(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int clampEffect(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int sampled(BufferedImage source, int x, int y, int dx, int dy) {
        int sx = Math.max(0, Math.min(source.getWidth() - 1, x + dx));
        int sy = Math.max(0, Math.min(source.getHeight() - 1, y + dy));
        return source.getRGB(sx, sy);
    }

    /**
     * Dispatches to the correct effect method for the given preset.
     *
     * @param source the even-dimensioned source image
     * @param preset the chosen effect preset
     * @return the processed image
     */
    public BufferedImage applyPreset(BufferedImage source, Preset preset) {
        return switch (preset) {
            case NONE -> source;
            case TWENTIES_REEL -> applyFaded(source);
            case FIFTIES_FILM -> applyFaded(source);
            case EIGHTIES_VHS -> applyFaded(source);
            case SEPIA -> applySepia(source);
            case FADED -> applyFaded(source);
        };
    }

    /**
     * Applies the "Faded" effect: keeps original colors but lifts the blacks,
     * adds a warm yellow tint, slight desaturation, and film grain.
     *
     * @param source the original image
     * @return a new BufferedImage with the faded effect applied
     */
    public BufferedImage applyFaded(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42); // fixed seed so preview is stable

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Slight desaturation: blend 85% original colour, 15% luminance
                int lum = (int) (r * 0.299 + g * 0.587 + b * 0.114);
                r = (int) (r * 0.85 + lum * 0.15);
                g = (int) (g * 0.85 + lum * 0.15);
                b = (int) (b * 0.85 + lum * 0.15);

                // Fade: lift blacks by blending towards 240 by 7%
                r = r + (int) ((240 - r) * 0.07);
                g = g + (int) ((240 - g) * 0.07);
                b = b + (int) ((240 - b) * 0.07);

                // Warm yellow tint
                r = clamp(r + 18);
                g = clamp(g + 8);
                b = clamp(b - 22);

                // Film grain: uniform noise applied equally across channels
                int grain = random.nextInt(25) - 12;
                r = clamp(r + grain);
                g = clamp(g + grain);
                b = clamp(b + grain);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * Applies the selected preset plus modular per-effect controls for the given frame.
     * This supports time-varying effects like flicker and tracking for exported videos.
     */
    public BufferedImage applyEffects(
            BufferedImage source,
            EffectConfig config,
            int frameIndex,
            int totalFrames) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        EffectConfig.EffectSetting lightLeak = withPresetDefault(config.getLightLeak(), config.getPreset(), "lightLeak");
        EffectConfig.EffectSetting vignette = withPresetDefault(config.getVignette(), config.getPreset(), "vignette");
        EffectConfig.EffectSetting grain = withPresetDefault(config.getGrain(), config.getPreset(), "grain");
        EffectConfig.EffectSetting dust = withPresetDefault(config.getDust(), config.getPreset(), "dust");
        EffectConfig.EffectSetting scratches = withPresetDefault(config.getScratches(), config.getPreset(), "scratches");
        EffectConfig.EffectSetting trackingH = withPresetDefault(config.getTrackingH(), config.getPreset(), "trackingH");
        EffectConfig.EffectSetting trackingV = withPresetDefault(config.getTrackingV(), config.getPreset(), "trackingV");
        EffectConfig.EffectSetting flicker = withPresetDefault(config.getFlicker(), config.getPreset(), "flicker");

        float time = totalFrames <= 1 ? 0f : (float) frameIndex / (float) (totalFrames - 1);
        int trackingShiftX = computeTrackingShiftH(trackingH, frameIndex);
        int trackingShiftY = computeTrackingShiftV(trackingV, frameIndex);

        float flickerMultiplier = 1f;
        if (flicker.mode() == EffectMode.ON) {
            float amp = clampEffect(flicker.variance()) / 100f * 0.35f;
            float hz = 0.5f + clampEffect(flicker.frequency()) / 100f * 7f;
            float rndFactor = clampEffect(flicker.randomness()) / 100f;
            float det = (float) Math.sin(frameIndex * 0.12f * hz) * amp;
            float rndComp = (new Random(43L + frameIndex * 97L).nextFloat() * 2 - 1) * amp;
            flickerMultiplier += det * (1f - rndFactor) + rndComp * rndFactor;
            flickerMultiplier = clampUnit(flickerMultiplier);
        }

        float leakStrength = lightLeak.mode() == EffectMode.ON ? clampEffect(lightLeak.variance()) / 100f : 0f;
        float leakFreq = lightLeak.mode() == EffectMode.ON ? clampEffect(lightLeak.frequency()) / 100f : 0f;
        float leakRndFactor = lightLeak.mode() == EffectMode.ON ? clampEffect(lightLeak.randomness()) / 100f : 0f;
        Random leakRng = new Random(55L + frameIndex * 113L);
        float detLeakCx = width * (0.15f + 0.7f * (0.5f + 0.5f * (float) Math.sin(time * (2f + leakFreq * 8f))));
        float detLeakCy = height * (0.2f + 0.6f * (0.5f + 0.5f * (float) Math.cos(time * (1.5f + leakFreq * 6f))));
        float leakCx = detLeakCx * (1f - leakRndFactor) + width * leakRng.nextFloat() * leakRndFactor;
        float leakCy = detLeakCy * (1f - leakRndFactor) + height * leakRng.nextFloat() * leakRndFactor;
        float leakRadius = Math.max(width, height) * (0.22f + leakStrength * 0.25f);

        // Grain: randomness varies the amplitude each frame around the baseline
        float grainRndFactor = grain.mode() == EffectMode.ON ? clampEffect(grain.randomness()) / 100f : 0f;
        float grainFrameMult = 1f + (new Random(777L + frameIndex * 41L).nextFloat() * 2 - 1) * grainRndFactor * 0.6f;
        Random grainRandom = new Random(91L + frameIndex * 977L);

        boolean baseMonochrome = config.getPreset() == Preset.TWENTIES_REEL;
        boolean baseSepia = config.getPreset() == Preset.SEPIA;
        boolean baseFaded = config.getPreset() == Preset.FADED
                || config.getPreset() == Preset.FIFTIES_FILM
                || config.getPreset() == Preset.EIGHTIES_VHS;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = sampled(source, x, y, trackingShiftX, trackingShiftY);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (baseMonochrome) {
                    // True B&W with a very faint warm paper tone for aged film
                    int lum = clamp((int) (r * 0.299 + g * 0.587 + b * 0.114));
                    r = clamp(lum + 6);
                    g = clamp(lum + 2);
                    b = lum;
                } else if (baseSepia) {
                    int sepiaR = clamp((int) (r * 0.393 + g * 0.769 + b * 0.189));
                    int sepiaG = clamp((int) (r * 0.349 + g * 0.686 + b * 0.168));
                    int sepiaB = clamp((int) (r * 0.272 + g * 0.534 + b * 0.131));
                    r = sepiaR;
                    g = sepiaG;
                    b = sepiaB;
                } else if (baseFaded) {
                    int lum = (int) (r * 0.299 + g * 0.587 + b * 0.114);
                    r = (int) (r * 0.85 + lum * 0.15);
                    g = (int) (g * 0.85 + lum * 0.15);
                    b = (int) (b * 0.85 + lum * 0.15);
                    r = r + (int) ((240 - r) * 0.07);
                    g = g + (int) ((240 - g) * 0.07);
                    b = b + (int) ((240 - b) * 0.07);
                    r = clamp(r + 14);
                    g = clamp(g + 6);
                    b = clamp(b - 18);
                }

                // Vignette
                if (vignette.mode() == EffectMode.ON) {
                    float cx = width / 2f;
                    float cy = height / 2f;
                    float nx = (x - cx) / cx;
                    float ny = (y - cy) / cy;
                    float d = (float) Math.sqrt(nx * nx + ny * ny);
                    float edge = clampUnit((d - 0.45f) / 0.75f);
                    float strength = clampEffect(vignette.variance()) / 100f;
                    float darken = 1f - edge * (0.55f * strength);
                    r = clamp((int) (r * darken));
                    g = clamp((int) (g * darken));
                    b = clamp((int) (b * darken));
                }

                // Light leak
                if (lightLeak.mode() == EffectMode.ON) {
                    float dx = x - leakCx;
                    float dy = y - leakCy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float glow = clampUnit(1f - dist / leakRadius) * leakStrength;
                    r = clamp((int) (r + glow * 120));
                    g = clamp((int) (g + glow * 60));
                    b = clamp((int) (b + glow * 20));
                }

                // Grain — amplitude varied per-frame by randomness
                if (grain.mode() == EffectMode.ON) {
                    int amp = Math.max(1, (int) (clampEffect(grain.variance()) / 5f * grainFrameMult));
                    int n = grainRandom.nextInt(amp * 2 + 1) - amp;
                    r = clamp(r + n);
                    g = clamp(g + n);
                    b = clamp(b + n);
                }

                r = clamp((int) (r * flickerMultiplier));
                g = clamp((int) (g * flickerMultiplier));
                b = clamp((int) (b * flickerMultiplier));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        if (dust.mode() == EffectMode.ON) {
            overlayDustAndLint(result, dust, frameIndex, totalFrames);
        }

        if (scratches.mode() == EffectMode.ON) {
            overlayMovingScratches(result, scratches, frameIndex, totalFrames);
        }

        // VHS scan lines: darken every 3rd horizontal line
        if (config.getPreset() == Preset.EIGHTIES_VHS) {
            for (int y = 1; y < height; y += 3) {
                for (int x = 0; x < width; x++) {
                    int rgb = result.getRGB(x, y);
                    int r = clamp(((rgb >> 16) & 0xFF) - 20);
                    int g = clamp(((rgb >> 8) & 0xFF) - 20);
                    int b = clamp((rgb & 0xFF) - 20);
                    result.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
        }

        return result;
    }

    /**
     * Builds a transparent RGBA layer that visualizes only the effect contribution.
     * Alpha is derived from per-pixel difference between source and processed output.
     */
    public BufferedImage applyEffectsOnlyAlphaLayer(
            BufferedImage source,
            EffectConfig config,
            int frameIndex,
            int totalFrames) {
        BufferedImage processed = applyEffects(source, config, frameIndex, totalFrames);
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage layer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int src = source.getRGB(x, y);
                int prc = processed.getRGB(x, y);

                int sr = (src >> 16) & 0xFF;
                int sg = (src >> 8) & 0xFF;
                int sb = src & 0xFF;

                int pr = (prc >> 16) & 0xFF;
                int pg = (prc >> 8) & 0xFF;
                int pb = prc & 0xFF;

                int dr = Math.abs(pr - sr);
                int dg = Math.abs(pg - sg);
                int db = Math.abs(pb - sb);
                int diff = Math.max(dr, Math.max(dg, db));

                // Emphasize subtle changes so effect layers remain visible.
                int alpha = clamp((int) (diff * 1.8f));
                if (alpha < 8) {
                    alpha = 0;
                }

                int argb = (alpha << 24) | (pr << 16) | (pg << 8) | pb;
                layer.setRGB(x, y, argb);
            }
        }

        return layer;
    }

    /**
     * Horizontal tracking shift — mimics sprocket-hole jitter.
     * The film holes are slightly oversized so the image rattles left/right
     * at a relatively high frequency with occasional larger snaps.
     * Max shift at variance=100 is ±12 px.
     */
    private int computeTrackingShiftH(EffectConfig.EffectSetting s, int frame) {
        if (s.mode() == EffectMode.NONE) return 0;
        float amp = clampEffect(s.variance()) / 100f * 12f;
        float hz  = 0.5f + clampEffect(s.frequency()) / 100f * 18f;
        float rndFactor = clampEffect(s.randomness()) / 100f;
        // Multi-harmonic sum gives that mechanical rattle character
        float det = (float) (Math.sin(frame * 0.18f * hz)        * 0.55f
                           + Math.sin(frame * 0.43f * hz + 1.3f) * 0.30f
                           + Math.sin(frame * 0.91f * hz + 2.7f) * 0.15f) * amp;
        float rnd = (new Random(701L + frame * 37L).nextFloat() * 2f - 1f) * amp;
        return Math.round(det * (1f - rndFactor) + rnd * rndFactor);
    }

    /**
     * Vertical tracking shift — mimics film-speed / shutter-speed variation.
     * The image drifts slowly up and down, like an old projector motor running
     * slightly uneven.  Much slower and smoother than the horizontal jitter.
     * Max shift at variance=100 is ±6 px.
     */
    private int computeTrackingShiftV(EffectConfig.EffectSetting s, int frame) {
        if (s.mode() == EffectMode.NONE) return 0;
        float amp = clampEffect(s.variance()) / 100f * 6f;
        float hz  = 0.05f + clampEffect(s.frequency()) / 100f * 1.5f;
        float rndFactor = clampEffect(s.randomness()) / 100f;
        // Slow, two-harmonic undulation
        float det = (float) (Math.sin(frame * 0.08f * hz)        * 0.75f
                           + Math.sin(frame * 0.23f * hz + 0.9f) * 0.25f) * amp;
        // Random drift sampled in larger chunks so it feels like a slow wander
        float rnd = (new Random(811L + (frame / 4) * 59L).nextFloat() * 2f - 1f) * amp;
        return Math.round(det * (1f - rndFactor) + rnd * rndFactor);
    }

    private void overlayDustAndLint(
            BufferedImage image,
            EffectConfig.EffectSetting dust,
            int frameIndex,
            int totalFrames) {
        int width = image.getWidth();
        int height = image.getHeight();
        int density = clampEffect(dust.variance());
        int duration = clampEffect(dust.frequency());
        int randomness = clampEffect(dust.randomness());
        int segmentSize = Math.max(1, 1 + duration / 12);
        int segment = frameIndex / segmentSize;
        int localFrame = frameIndex % segmentSize;
        Random segmentRng = new Random(5000L + segment * 131L + density * 13L + randomness * 31L);

        // Some segments have no dust at all: "maybe yes, maybe no"
        float activeSegmentChance = 0.18f + density / 100f * 0.62f;
        if (segmentRng.nextFloat() > activeSegmentChance) {
            return;
        }

        int particleCount = 1 + segmentRng.nextInt(Math.max(1, density / 22 + 1));
        for (int i = 0; i < particleCount; i++) {
            int x = segmentRng.nextInt(Math.max(1, width));
            int y = segmentRng.nextInt(Math.max(1, height));
            int jitter = Math.max(0, randomness / 28);
            if (jitter > 0) {
                Random jitterRng = new Random(6100L + i * 67L + frameIndex * 17L);
                x += jitterRng.nextInt(jitter * 2 + 1) - jitter;
                y += jitterRng.nextInt(jitter * 2 + 1) - jitter;
            }

            int radius = 1 + segmentRng.nextInt(1 + density / 35);
            boolean bright = segmentRng.nextBoolean();
            float opacity = 0.22f + segmentRng.nextFloat() * 0.33f;
            // Soften frame transitions for short-lived particles
            if (segmentSize > 1) {
                float fadeIn = Math.min(1f, (localFrame + 1) / 2f);
                float fadeOut = Math.min(1f, (segmentSize - localFrame) / 2f);
                opacity *= Math.min(fadeIn, fadeOut);
            }

            DustParticle particle = new DustParticle(x, y, radius, bright);
            drawDustParticle(image, particle, opacity);

            // Occasional lint fiber
            if (segmentRng.nextFloat() < 0.24f) {
                int lintLength = 5 + segmentRng.nextInt(8 + density / 10);
                float angle = (float) (segmentRng.nextFloat() * Math.PI * 2);
                int dx = Math.round((float) Math.cos(angle) * lintLength);
                int dy = Math.round((float) Math.sin(angle) * lintLength * 0.35f);
                int brightness = bright ? 228 : 24;
                drawLine(image, x, y, x + dx, y + dy, 1, brightness, opacity * 0.75f);
            }
        }
    }

    private void drawDustParticle(BufferedImage image, DustParticle particle, float opacity) {
        int brightness = particle.bright() ? 235 : 20;
        for (int dy = -particle.radius(); dy <= particle.radius(); dy++) {
            for (int dx = -particle.radius(); dx <= particle.radius(); dx++) {
                if (dx * dx + dy * dy > particle.radius() * particle.radius()) {
                    continue;
                }
                blendPixel(image, particle.x() + dx, particle.y() + dy, brightness, opacity);
            }
        }
    }

    private void overlayMovingScratches(
            BufferedImage image,
            EffectConfig.EffectSetting scratches,
            int frameIndex,
            int totalFrames) {
        int width = image.getWidth();
        int height = image.getHeight();
        int intensity = clampEffect(scratches.variance());
        int activity = clampEffect(scratches.frequency());
        int randomness = clampEffect(scratches.randomness());
        int scratchEvents = Math.max(1, intensity / 16);

        for (int i = 0; i < scratchEvents; i++) {
            Random eventRng = new Random(7000L + i * 211L + intensity * 17L + randomness * 29L);

            int life = 2 + activity / 15 + eventRng.nextInt(Math.max(1, 4 + randomness / 20));
            int startFrame = totalFrames <= 1 ? 0 : eventRng.nextInt(Math.max(1, totalFrames));
            if (frameIndex < startFrame || frameIndex >= startFrame + life) {
                continue;
            }

            int age = frameIndex - startFrame;

            // Scratch protocol inputs per event
            float startX = eventRng.nextInt(Math.max(1, width));
            int startY = eventRng.nextInt(Math.max(1, height));
            float severity = clampUnit((intensity / 100f) * 0.65f + eventRng.nextFloat() * 0.55f);

            // Angle measured from vertical. Near 0 => slow crawl, steeper => faster crawl.
            float maxAngleDeg = 5f + randomness * 0.35f;
            float angleDeg = (eventRng.nextFloat() * 2f - 1f) * maxAngleDeg;
            float slope = (float) Math.tan(Math.toRadians(angleDeg)); // dx per +1 vertical pixel

            // Event-level appearance traits
            boolean brightScratch = eventRng.nextFloat() < 0.80f;
            int brightness = brightScratch ? 220 : 34;
            int baseThickness = 1 + (severity > 0.62f ? 1 : 0) + (severity > 0.82f ? 1 : 0);
            float baseOpacity = 0.10f + severity * 0.45f;

            // Optional early on-screen ending in final frame.
            boolean taperEndOnLastFrame = eventRng.nextFloat() < 0.48f;
            int randomEndY = (int) (height * (0.20f + eventRng.nextFloat() * 0.60f));

            float segStartX;
            int segStartY;
            float segEndX;
            int segEndY;

            if (age == 0) {
                // First frame starts at random (x, y) and runs to the top of this frame.
                segStartX = startX;
                segStartY = startY;
                segEndY = 0;
                segEndX = segStartX + slope * segStartY;
            } else {
                // Continue from bottom at the exact x where previous frame reached top.
                float firstTopX = startX + slope * startY;
                float frameTopDelta = slope * (height - 1);
                float prevTopX = firstTopX + frameTopDelta * (age - 1);
                segStartX = prevTopX;
                segStartY = height - 1;
                segEndY = 0;
                segEndX = segStartX + frameTopDelta;
            }

            if (age == life - 1 && taperEndOnLastFrame && segEndX >= 0 && segEndX < width) {
                segEndY = randomEndY;
            }

            // Add a small gouge at the event start frame to sell film damage origin.
            if (age == 0) {
                drawDustParticle(
                        image,
                        new DustParticle(Math.round(segStartX), segStartY, 1 + eventRng.nextInt(2), brightScratch),
                        baseOpacity * 1.1f);
            }

            drawScratchSegment(
                    image,
                    segStartX,
                    segStartY,
                    segEndX,
                    segEndY,
                    baseThickness,
                    brightness,
                    baseOpacity,
                    age == life - 1);
        }
    }

    private void drawScratchSegment(
            BufferedImage image,
            float xStart,
            int yStart,
            float xEnd,
            int yEnd,
            int thickness,
            int brightness,
            float opacity,
            boolean isLastFrameOfEvent) {
        int span = Math.max(1, yStart - yEnd);
        for (int i = 0; i <= span; i++) {
            float t = i / (float) span;
            int y = yStart - i;
            float x = xStart + (xEnd - xStart) * t;

            float taper = 1f;
            if (isLastFrameOfEvent && t > 0.65f) {
                taper = Math.max(0f, 1f - (t - 0.65f) / 0.35f);
            }

            int localThickness = Math.max(1, Math.round(thickness * (0.55f + 0.45f * taper)));
            float localOpacity = opacity * taper;
            for (int ox = -localThickness; ox <= localThickness; ox++) {
                blendPixel(image, Math.round(x) + ox, y, brightness, localOpacity);
            }
        }
    }

    private void drawLine(BufferedImage image, int x1, int y1, int x2, int y2, int thickness, int brightness, float opacity) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            blendPixel(image, x1, y1, brightness, opacity);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(x1 + (x2 - x1) * t);
            int y = Math.round(y1 + (y2 - y1) * t);
            for (int oy = -thickness; oy <= thickness; oy++) {
                for (int ox = -thickness; ox <= thickness; ox++) {
                    if (Math.abs(ox) + Math.abs(oy) > thickness + 1) {
                        continue;
                    }
                    blendPixel(image, x + ox, y + oy, brightness, opacity);
                }
            }
        }
    }

    private void blendPixel(BufferedImage image, int x, int y, int targetBrightness, float opacity) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float clampedOpacity = clampUnit(opacity);
        int current = (int) ((r + g + b) / 3f);
        int blended = (int) (targetBrightness * clampedOpacity + current * (1f - clampedOpacity));
        blended = clamp(blended);
        image.setRGB(x, y, (blended << 16) | (blended << 8) | blended);
    }

    private EffectConfig.EffectSetting withPresetDefault(
            EffectConfig.EffectSetting provided,
            Preset preset,
            String effectName) {
        if (provided.mode() == EffectMode.ON) {
            return provided;
        }
        return switch (preset) {
            // 20s: heavy B&W grain, strong vignette, irregular flicker and scratches
            case TWENTIES_REEL -> switch (effectName) {
                case "vignette"  -> new EffectConfig.EffectSetting(EffectMode.ON, 70,  0, 10);
                case "grain"     -> new EffectConfig.EffectSetting(EffectMode.ON, 65, 50, 45);
                case "dust"      -> new EffectConfig.EffectSetting(EffectMode.ON, 45, 65, 55);
                case "scratches" -> new EffectConfig.EffectSetting(EffectMode.ON, 50, 55, 60);
                case "flicker"   -> new EffectConfig.EffectSetting(EffectMode.ON, 40, 45, 65);
                default -> provided;
            };
            // 50s: faded color, gentle grain, mild vignette, subtle flicker
            case FIFTIES_FILM -> switch (effectName) {
                case "vignette" -> new EffectConfig.EffectSetting(EffectMode.ON, 28,  0, 10);
                case "grain"    -> new EffectConfig.EffectSetting(EffectMode.ON, 35, 35, 30);
                case "dust"     -> new EffectConfig.EffectSetting(EffectMode.ON, 20, 35, 20);
                case "flicker"  -> new EffectConfig.EffectSetting(EffectMode.ON, 14, 30, 25);
                default -> provided;
            };
            // 80s VHS: scan lines (handled in post-pass), tracking jitter, fast grain, flicker
            case EIGHTIES_VHS -> switch (effectName) {
                case "trackingH" -> new EffectConfig.EffectSetting(EffectMode.ON, 35, 75, 70);
                case "trackingV" -> new EffectConfig.EffectSetting(EffectMode.ON, 25, 60, 50);
                case "flicker"   -> new EffectConfig.EffectSetting(EffectMode.ON, 22, 65, 50);
                case "grain"     -> new EffectConfig.EffectSetting(EffectMode.ON, 26, 70, 55);
                default -> provided;
            };
            default -> provided;
        };
    }
}
