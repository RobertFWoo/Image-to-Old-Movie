package com.imagetooldvideo.processing;

import com.imagetooldvideo.model.VideoSettings;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VideoCreator {

    private static final int FRAME_RATE = 24;

    @FunctionalInterface
    public interface FrameGenerator {
        BufferedImage createFrame(int frameIndex, int totalFrames);
    }

    /**
     * Encodes MP4 video frames generated from the provided frame generator.
     *
     * @param settings       the video settings (duration, output directory)
     * @param frameGenerator callback that returns a frame image for each frame index
     * @return the Path to the generated MP4 file
     * @throws Exception if encoding fails
     */
    public Path createVideo(VideoSettings settings, FrameGenerator frameGenerator) throws Exception {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return createVideo(settings, "old_video_" + ts + ".mp4", frameGenerator);
    }

    public Path createVideo(VideoSettings settings, String filename, FrameGenerator frameGenerator) throws Exception {
        Path outputDir = settings.getOutputDirectory();
        Files.createDirectories(outputDir);

        // Sanitise filename so it is safe on all platforms
        String safeName = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        File outputFile = outputDir.resolve(safeName).toFile();

        int totalFrames = settings.getDurationSeconds() * FRAME_RATE;

        SeekableByteChannel channel = NIOUtils.writableFileChannel(outputFile.getAbsolutePath());
        try {
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(channel, Rational.R(FRAME_RATE, 1));
            for (int i = 0; i < totalFrames; i++) {
                encoder.encodeImage(frameGenerator.createFrame(i, totalFrames));
            }
            encoder.finish();
        } finally {
            NIOUtils.closeQuietly(channel);
        }

        return outputFile.toPath();
    }
}
