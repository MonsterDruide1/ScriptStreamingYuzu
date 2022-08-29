package yuzu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMPEG {

	// https://github.com/bramp/ffmpeg-cli-wrapper/blob/master/src/main/java/net/bramp/ffmpeg/progress/Progress.java
	public class Progress {

		public long frame = 0;
		// public Fraction fps = Fraction.ZERO;
		public long bitrate = 0;
		public long total_size = 0;
		public long out_time_ns = 0;
		public long dup_frames = 0;
		public long drop_frames = 0;
		public float speed = 0;

		protected void parseLine(String line) {
			line = line.trim();
			if (line.isEmpty() || !line.contains("frame=")) {
				return;
			}

			final String[] args = line.split("(\\s+)|(=)(\\s*)");
			if (args.length % 2 != 0) {
				// invalid argument, so skip
				return;
			}

			for (int i = 0; i < args.length; i += 2) {

				final String key = args[i];
				final String value = args[i + 1];

				switch (key) {
				case "frame":
					frame = Long.parseLong(value);
					break;

				case "fps":
					// fps = Fraction.getFraction(value);
					break;

				case "bitrate":
					if (value.equals("N/A")) {
						bitrate = -1;
					} else {
						// bitrate = FFmpegUtils.parseBitrate(value);
					}
					break;

				case "size":
					if (value.equals("N/A")) {
						total_size = -1;
					} else {
						//numberformatexception: has a suffix like `kB`
						//total_size = Long.parseLong(value);
					}
					break;

				case "out_time_ms":
					// This is a duplicate of the "out_time" field, but expressed as a int instead
					// of string.
					// Note this value is in microseconds, not milliseconds, and is based on
					// AV_TIME_BASE which
					// could change.
					// out_time_ns = Long.parseLong(value) * 1000;
					break;

				case "time":
					out_time_ns = fromTimecode(value);
					break;

				case "dup_frames":
					dup_frames = Long.parseLong(value);
					break;

				case "drop_frames":
					drop_frames = Long.parseLong(value);
					break;

				case "speed":
					if (value.equals("N/A")) {
						speed = -1;
					} else {
						speed = Float.parseFloat(value.replace("x", ""));
					}
					break;

				case "progress":
					// TODO After "end" stream is closed
					// status = Status.of(value);
					break; // The status field is always last in the record
					
				case "q": //quality?
				case "Lsize": //seems to be final file size after finishing
					break; //ignore

				default:
					if (key.startsWith("stream_")) {
						// TODO handle stream_0_0_q=0.0:
						// stream_%d_%d_q= file_index, index, quality
						// stream_%d_%d_psnr_%c=%2.2f, file_index, index, type{Y, U, V}, quality //
						// Enable with
						// AV_CODEC_FLAG_PSNR
						// stream_%d_%d_psnr_all
					} else {
						System.err.printf("FFMPEG-Progress: skipping unhandled key: %s = %s\n", key, value);
					}

					break; // Either way, not supported
				}
			}
		}

		static final Pattern TIME_REGEX = Pattern.compile("(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");

		public static long fromTimecode(String time) {
			Matcher m = TIME_REGEX.matcher(time);
			if (!m.find()) {
				throw new IllegalArgumentException("invalid time '" + time + "'");
			}

			long hours = Long.parseLong(m.group(1));
			long mins = Long.parseLong(m.group(2));
			double secs = Double.parseDouble(m.group(3));

			return TimeUnit.HOURS.toNanos(hours) + TimeUnit.MINUTES.toNanos(mins)
					+ (long) (TimeUnit.SECONDS.toNanos(1) * secs);
		}
	}

	public interface ProgressCallback {
		void callback(int out_time);
	}

	private String command;
	private ProgressCallback callback;

	public FFMPEG(String command) {
		this.command = command;
	}

	public void setProgressCallback(ProgressCallback callback) {
		this.callback = callback;
	}

	public void execute() {
		try {
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream())); //FFMPEG only uses error stream, so only read that
			
			Progress progress = new Progress();
			long oldOutTime = Integer.MIN_VALUE;
			
			while (process.isAlive()) {
				if (errReader.ready()) {
					String line = errReader.readLine();
					if (line != null && callback != null) {
						progress.parseLine(line);
						int seconds = (int) TimeUnit.NANOSECONDS.toSeconds(progress.out_time_ns);
						if(seconds > oldOutTime) {
							callback.callback(seconds);
							oldOutTime = seconds;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
