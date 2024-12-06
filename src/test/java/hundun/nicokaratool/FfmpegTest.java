package hundun.nicokaratool;

import hundun.nicokaratool.base.SecretConfig;
import hundun.nicokaratool.japanese.TagTokenizer.Timestamp;
import hundun.nicokaratool.layout.VideoRender;
import hundun.nicokaratool.layout.VideoRender.KeyFrame;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FfmpegTest {
    List<KeyFrame> frames = List.of(
            KeyFrame.builder()
                    .imagePath(Constants.TEST_INPUT_FOLDER + "avatar1.png")
                    .inpoint(Timestamp.parse("[00:01.000]"))
                    .outpoint(Timestamp.parse("[00:05.500]"))
                    .build(),
            KeyFrame.builder()
                    .imagePath(Constants.TEST_INPUT_FOLDER + "avatar2.png")
                    .inpoint(Timestamp.parse("[00:07.00]"))
                    .outpoint(Timestamp.parse("[00:09.500]"))
                    .build()
    );
    String prepareFolder = Constants.TEST_OUTPUT_FOLDER + "prepare-temp/";
    FFmpeg ffmpeg;
    FFprobe ffprobe;

    public FfmpegTest() throws IOException {
        assert SecretConfig.ffmpegConfig != null;
        ffmpeg = new FFmpeg(SecretConfig.ffmpegConfig.get("ffmpegPath").asText());
        ffprobe = new FFprobe(SecretConfig.ffmpegConfig.get("ffprobePath").asText());
    }

    @Test
    public void testFun() throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe("data/input.mp4");

        FFmpegBuilder builder = new FFmpegBuilder()

                .setInput(probeResult)     // Filename, or a FFmpegProbeResult
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput("data/output.mp4")   // Filename for the destination
                .setFormat("mp4")        // Format is inferred from filename, or can be set


                .setTargetSize(250_000)  // Aim for a 250KB file

                .disableSubtitle()       // No subtiles

                .setAudioChannels(1)         // Mono audio
                .setAudioCodec("aac")        // using the aac codec
                .setAudioSampleRate(48_000)  // at 48KHz
                .setAudioBitRate(32768)      // at 32 kbit/s

                .setVideoCodec("libx264")     // Video using x264
                .setVideoFrameRate(24, 1)     // at 24 frames per second
                .setVideoResolution(640, 480) // at 640x480 resolution

                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

// Run a one-pass encode
        executor.createJob(builder).run();

// Or run a two-pass encode (which is better quality at the cost of being slower)
        executor.createTwoPassJob(builder).run();

    }

    @Test
    public void testImage2Video() throws IOException {

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput("test-input/avatar1.png")

                .addExtraArgs("-loop", "1")
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput("test-output/avatar1.mp4")   // Filename for the destination
                //.setFormat("mp4")        // Format is inferred from filename, or can be set


                .addExtraArgs("-pix_fmt", "yuv420p")

                .addExtraArgs("-c:v", "libx264")
                //.setVideoCodec("libx264")

                .addExtraArgs("-t", "15")
                //.setDuration(15, TimeUnit.SECONDS)
                .done()
                ;

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        FFmpegJob job = executor.createJob(builder, new ProgressListener() {

            // Using the FFmpegProbeResult determine the duration of the input

            @Override
            public void progress(Progress progress) {


                // Print out interesting information about the progress
                System.out.println(String.format(
                        "status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
                        progress.status,
                        progress.frame,
                        FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                        progress.fps.doubleValue(),
                        progress.speed
                ));
            }
        });

        job.run();
    }

    @Test
    public void test_concat() throws IOException {
        VideoRender.concat(
                prepareFolder,
                frames
        );
    }

    @Test
    public void test_prepare() throws IOException {
        VideoRender.prepare(prepareFolder, frames);
    }

    @Test
    public void test_addAudio() throws IOException {
        String outFile = Constants.TEST_OUTPUT_FOLDER + "out.mp4";
        VideoRender.addAudio(prepareFolder, outFile, Constants.TEST_INPUT_FOLDER + "loop.wav");
    }
}
