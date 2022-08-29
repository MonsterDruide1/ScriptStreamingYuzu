package requests;

import java.io.File;
import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class ScreenshotRequest implements IRequest<File> {

	public TextChannel channel;
	public User user;
	public int index;

	public ScreenshotRequest(TextChannel channel, User user, int index) {
		this.channel = channel;
		this.user = user;
		this.index = index;
	}
	
	public void onComplete(File screenshotFile) throws InterruptedException, ExecutionException {
		channel.sendMessage("Screenshot of instance "+index+" (as requested by "+user.getAsMention()+"):")
				.addFile(screenshotFile, "screenshot.png")
				.complete();
	}
	
}
