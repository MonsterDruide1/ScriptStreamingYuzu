package yuzu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import bot.DestUtil;
import bot.YuzuBot;
import config.UserSetting;
import in.ParseException;
import out.ChangePage;
import out.PlayerGo;
import out.script.Script;
import requests.IRequest;
import requests.ScreenshotRequest;
import requests.ScriptRequest;
import server.Main;
import server.Server;
import util.Util;

public class Yuzu {
	
	volatile LinkedBlockingQueue<IRequest<File>> requests;
	Server server;
	
	public Yuzu(int clientPort, int serverPort) throws SocketException {
		requests = new LinkedBlockingQueue<>();
		server = new Server(clientPort, serverPort);
		Main.server = server; //FIXME 
		server.startLoopThread();
		
		new Thread(() -> threadLoop()).start();
	}
	
	public void queueRequest(IRequest<File> request) throws InterruptedException {
		if(request instanceof ScriptRequest) {
			ScriptRequest sRequest = (ScriptRequest) request;
			sRequest.requestMessage.addReaction(sRequest.requestMessage.getGuild().getEmoteById(892015065702756362L)).queue();
			try {
				sRequest.setStatusReport(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		requests.put(request);
	}
	
	public int getRequestsCount() {
		return requests.size();
	}
	
	private void threadLoop() {
		while(true) {
			try {
				IRequest<File> request = requests.take();
				handle(request);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handle(IRequest<File> request) throws InterruptedException {
		togglePaused();
		if(request instanceof ScriptRequest)
			handle((ScriptRequest) request);
		else if(request instanceof ScreenshotRequest)
			handle((ScreenshotRequest) request);
		else
			throw new UnsupportedOperationException("Request not implemented: "+request.getClass().getCanonicalName());
		togglePaused();
	}
	
	public void togglePaused() {
		try {
			Runtime.getRuntime().exec("SendKeystroke.exe \""+getYuzuWindowName()+"\" \"Qt5152QWindowIcon\"");
			paused = !paused;
			Thread.sleep(10000);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handle(ScriptRequest request) throws InterruptedException {
		try {
			File file = request.scriptFile.get();
			request.setStatusReport(1);
			
			UserSetting setting = YuzuBot.userSettings.get(request.user.getName());
			if(setting == null || setting.getStage() == null || setting.getEntry() == null) {
				throw new ParseException("The warp destination has not been set properly!");
			}
			
			String[] stageNames = DestUtil.getEntries(setting.getStage(), setting.getScenario(), "ChangeStageName");
			String kingdomName = Arrays.stream(stageNames).filter(s -> s.contains("Home")).findFirst().orElse(null);
			if(kingdomName != null) {
				server.sendPacket(new PlayerGo(kingdomName, -1, setting.getEntry(), false));
				Thread.sleep(10000);
			}
			
			Script script = new Script(file);
			server.sendPacket(new ChangePage((byte)(setting.getPage()-1)));
			Thread.sleep(100);
			server.sendPackets(Util.sendScript(script, request.fileName));
			server.sendPacket(new PlayerGo(setting.getStage(), setting.getScenario(), setting.getEntry(), true));
			
			int time = setting.getRecord();
			if(time == -1)
				time = (script.frames.size()/60) + 8; // 8 seconds
			
			time -= setting.getRecordStart();
			
			Thread.sleep(setting.getRecordStart()*1000);

			request.setStatusReport(2);
			Path tempVideoFile = Files.createTempFile("tas_", ".mp4");
			File videoFile = tempVideoFile.toFile();
			String cmd = "ffmpeg -y ";
			cmd += getFFMPEGInputDetails();
			cmd += "-vcodec libx264 -crf 38 -b:a 72k -vf fps=30 -pix_fmt yuv420p -an -t "+time;
			cmd += " " + tempVideoFile.toString();
			FFMPEG ffmpeg = new FFMPEG(cmd);
			final int finalTime = time;
			ffmpeg.setProgressCallback((out_time) -> {
				try {
					request.setRecordStatusReport(out_time, finalTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			ffmpeg.execute();

			request.setStatusReport(3);
			
			File compressedVideoFile = videoFile;//compressVideoFile(videoFile);
			request.setStatusReport(4);
			
			request.onComplete(compressedVideoFile);
		} catch(Exception e) {
			request.sendStackTrace(e);
		}
	}
	private void handle(ScreenshotRequest request) throws InterruptedException {
		try {
			Path tempScreenshotPath = Files.createTempFile("screenshot_", ".png");
			File screenshotFile = tempScreenshotPath.toFile();
			String cmd = "ffmpeg -y ";
			cmd += getFFMPEGInputDetails();
			cmd += "-vframes 1 ";
			cmd += tempScreenshotPath.toString();
			FFMPEG ffmpeg = new FFMPEG(cmd);
			ffmpeg.execute();
			
			request.onComplete(screenshotFile);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getFFMPEGInputDetails() throws IOException, InterruptedException {
		String title = getYuzuWindowName();
		
		return "-video_size 1280x960 -framerate 30 -f gdigrab -i title=\""+title+"\" -draw_mouse 0 ";

		//return "-f x11grab -i :0.0+6,689 ";
	}

	public static String getYuzuWindowName() throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("tasklist /v /fi \"imagename eq yuzu.exe\" /fo csv /nh");
		p.waitFor();
		
		String result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().parallel().collect(Collectors.joining("\n"));
		
		String firstLine = result.split("\n")[0];
		String[] parts = firstLine.split(",");
		String title = parts[parts.length-1];
		
		return title.substring(1, title.length()-1);
	}

}
