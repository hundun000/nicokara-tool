package hundun.nicokaratool.layout;

import hundun.nicokaratool.base.SecretConfig;
import hundun.nicokaratool.japanese.TagTokenizer.Timestamp;
import hundun.nicokaratool.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VideoRender {

    static FFmpeg ffmpeg;
    static FFprobe ffprobe;

    static  {
        assert SecretConfig.ffmpegConfig != null;
        try {
            ffmpeg = new FFmpeg(SecretConfig.ffmpegConfig.get("ffmpegPath").asText());
            ffprobe = new FFprobe(SecretConfig.ffmpegConfig.get("ffprobePath").asText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class KeyFrame {
        String imagePath;
        String videoPath;
        Long durationLast;
        Long durationNext;
        Timestamp inpoint;
        Timestamp outpoint;
    }

    public static void prepare(String prepareFolder, List<KeyFrame> frames) {
        new File(prepareFolder).mkdirs();

        for (int i = 0; i < frames.size(); i++) {
            var frame = frames.get(i);
            long durationMs;
            if (frame.getDurationLast() == null) {
                // 首句片段时长：延长前奏
                durationMs = frame.getOutpoint().totalMs();
            } else if (frame.getDurationNext() == null) {
                // 尾句片段时长：延长前一间隔的一半
                durationMs = frame.getOutpoint().totalMs() - frame.getInpoint().totalMs() + frame.getDurationLast() / 2;
            } else {
                // 一般片段时长：延长前后间隔的一半
                durationMs = frame.getOutpoint().totalMs() - frame.getInpoint().totalMs() + (frame.getDurationLast() + frame.getDurationNext()) / 2;
            }
            String outPath = prepareFolder + i + ".mp4";
            FFmpegBuilder builder = new FFmpegBuilder()
                    .addInput(frame.getImagePath())

                    .addExtraArgs("-loop", "1")
                    .overrideOutputFiles(true) // Override the output if it exists

                    .addOutput(outPath)   // Filename for the destination
                    //.setFormat("mp4")        // Format is inferred from filename, or can be set


                    .addExtraArgs("-pix_fmt", "yuv420p")

                    .addExtraArgs("-c:v", "libx264")
                    //.setVideoCodec("libx264")

                    .setDuration(durationMs, TimeUnit.MILLISECONDS)
                    .done()
                    ;

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            FFmpegJob job = executor.createJob(builder);

            job.run();

            frame.setVideoPath(outPath);

        }

        String tempFile = prepareFolder + "ffmpeg-temp.txt";
        String text = frames.stream()
                .map(it -> {
                    String relative = new File(prepareFolder).toURI().relativize(new File(it.getVideoPath()).toURI()).getPath();
                    return "file " + relative;
                    //return "file " + relative + "\ninpoint  " + it.getInpoint().toFfmpegTime() + "\noutpoint  " + it.getOutpoint().toFfmpegTime();
                })
                .collect(Collectors.joining("\n"));
        Utils.writeAllLines(tempFile, text);
    }
    public static String concat(String prepareFolder, List<KeyFrame> frames) {
        String tempFile = prepareFolder + "ffmpeg-temp.txt";
        String outFile = prepareFolder + "concat.mp4";
        FFmpegBuilder builder = new FFmpegBuilder()
                .addExtraArgs("-safe", "0")
                .addExtraArgs("-f", "concat")
                .addInput(tempFile)
                //.addExtraArgs("-loop", "1")
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput(outFile)   // Filename for the destination
                //.setFormat("mp4")        // Format is inferred from filename, or can be set


                .addExtraArgs("-c", "copy")
                //.setVideoCodec("libx264")

                //.addExtraArgs("-t", "15")
                .done()
                ;

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        FFmpegJob job = executor.createJob(builder);

        job.run();
        return outFile;
    }

    public static void addAudio(String prepareFolder, String outFile, String audioFile) {
        String concatFile = prepareFolder + "concat.mp4";

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(concatFile)
                .addInput(audioFile)
                //.addExtraArgs("-loop", "1")
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput(outFile)   // Filename for the destination
                //.setFormat("mp4")        // Format is inferred from filename, or can be set
                .addExtraArgs("-shortest")
                .addExtraArgs("-c:v", "copy")
                .addExtraArgs("-c:a", "aac")
                //.setVideoCodec("libx264")

                //.addExtraArgs("-t", "15")
                .done()
                ;

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        FFmpegJob job = executor.createJob(builder);

        job.run();
    }
}
