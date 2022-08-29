package requests;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class ScriptRequest implements IRequest<File> {

	public CompletableFuture<File> scriptFile;
	public String fileName;
	public User user;
	public TextChannel channel;
	public Message requestMessage;

	volatile LinkedBlockingQueue<Integer> progressUpdates;
	public Message statusReport;
	
	volatile int recordTime, totalTime;

	public ScriptRequest(TextChannel channel, Message requestMessage) throws IOException {
		Attachment scriptAttachment = requestMessage.getAttachments().get(0);
		this.scriptFile = scriptAttachment.downloadToFile(File.createTempFile("script-", ".txt"));
		this.fileName = scriptAttachment.getFileName();
		this.user = requestMessage.getAuthor();
		this.channel = channel;
		this.requestMessage = requestMessage;
		
		progressUpdates = new LinkedBlockingQueue<>();
		new Thread(() -> {
			Integer progress = 0;
			while(progress != getProgressSteps().length && progress != -1) {
				try {
					progress = progressUpdates.take();
					sendStatusReport(progress);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void setStatusReport(int status) throws InterruptedException {
		progressUpdates.put(status);
	}
	public void setRecordStatusReport(int recordTime, int totalTime) throws InterruptedException {
		this.recordTime = recordTime;
		this.totalTime = totalTime;
		
		setStatusReport(2); //FIXME find a better way
	}
	public void sendStatusReport(int status) throws InterruptedException, ExecutionException {
		EmbedBuilder builder = new EmbedBuilder();
		String title = fileName+" by "+user.getName();
		builder.setTitle(title);
		//builder.setAuthor(user.getName());
		builder.setColor(getColor(status));
		//builder.setFooter("Footer");
		builder.setDescription("Script can be found [here]("+requestMessage.getJumpUrl()+")!");
		builder.setTimestamp(new Date().toInstant());
		builder.setThumbnail(user.getEffectiveAvatarUrl());
		builder.addField("Progress:", getProgressString(status), false);
		if(statusReport == null) {
			System.out.println("sending initial message");
			statusReport = channel.sendMessage("Bot is online and has received your submission!").submit().get();
		}
		statusReport.editMessageEmbeds(builder.build()).submit();
	}
	
	public void sendStackTrace(Exception e) throws InterruptedException {
		e.printStackTrace();
		setStatusReport(-1);
		while(statusReport == null) {
			Thread.sleep(50);
		}
		statusReport.editMessage("Your request failed to execute! For serious issues (not a timeout or too big file), ask <@403475380532543488> to fix it.\n" +
				"To try again, please send a message with the following content: ```"+requestMessage.getJumpUrl()+"```\n" +
				"Please ignore the following if you don't know what it means (`StackTrace`):```"+e.getMessage()+"```").complete();
		requestMessage.removeReaction(requestMessage.getGuild().getEmoteById(892015065702756362L)).queue();
		requestMessage.addReaction("U+274C").queue(); //red cross
	}
	
	public void onComplete(File videoFile) throws InterruptedException {
		channel.sendMessage("The execution of your script has finished! (<@"+user.getId()+">)\nThe original script file can be found here: "+requestMessage.getJumpUrl())
				.addFile(videoFile, fileName+"."+videoFile.getName().split("\\.")[videoFile.getName().split("\\.").length-1])
					.complete();

		setStatusReport(getProgressSteps().length);
		requestMessage.removeReaction(requestMessage.getGuild().getEmoteById(892015065702756362L)).queue();
		requestMessage.addReaction("U+2705").queue(); //white check mark on green background
		Thread.sleep(5000);
		statusReport.delete().queue();
	}
	
	Color getColor(int status) {
		int totalSteps = getProgressSteps().length;
		if(status == -1) return Color.red;
		if(status == totalSteps) return Color.green;
		
		return Color.blue;
	}
	
	String getProgressString(int status) {
		StringBuilder builder = new StringBuilder();
		
		String[] steps = getProgressSteps();
		for(int i=0; i<steps.length; i++) {
			builder.append(steps[i]);
			builder.append(check(status, i+1));
			
			if(status == 2 && i == 2) { //FIXME find a better way
				builder.append(" ("+recordTime+" / "+totalTime+")");
			}
			
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	String check(int status, int required) {
		if(status == -1)
			return ":x:";
		if(status >= required)
			return ":white_check_mark:";
		if(status == required-1)
			return "<a:loading:892015065702756362>";
		return ":zzz:";
	}
	
	public User getUser() {
		return user;
	}
	
	public String[] getProgressSteps() {
		return new String[] {
				"Waiting in Queue:  ",
				"Preparing script:  ",
				"Capturing video footage:  ",
				"Compressing video file:  ",
				"Uploading file:  "
		};
	}
	
}
