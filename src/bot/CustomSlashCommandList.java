package bot;

import config.UserSetting;
import handler.ScriptHandlerMod;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import out.PlayerGo;
import requests.ScreenshotRequest;

public class CustomSlashCommandList extends SlashCommandList {
	
	public CustomSlashCommandList() {
		addCommands(
				new SlashCommand(new CommandData("dest", "Set destination of script playback warp")
								.addOption(OptionType.STRING, "stage", "Name of the stage. Type /stages to get a list of stages.")
								.addOption(OptionType.INTEGER, "scenario", "Scenario number. Has to be between 1 and 15.")
								.addOption(OptionType.STRING, "entry", "Name of the entry to warp to."), this::dest),
				new SlashCommand(new CommandData("get-dest", "Get current destination of script playback warp"), this::getDest),
				new SlashCommand(new CommandData("vdur", "Set the video duration manually if required, or go back to default using -1.")
						.addOption(OptionType.INTEGER, "duration", "Enter the video duration in seconds, or -1 for automatic.", true), this::vdur),
				new SlashCommand(new CommandData("vstart", "Set the starting offset of video recording.")
						.addOption(OptionType.INTEGER, "start", "Enter the video start offset in seconds, or 0 for none.", true), this::vstart),
				new SlashCommand(new CommandData("page", "Set the page that should be opened while playing back the TAS.")
						.addOption(OptionType.INTEGER, "page", "Enter the page ID.", true), this::page),
				new SlashCommand(new CommandData("reset", "Delete all data about your user from the bot"), this::reset),
				new SlashCommand(new CommandData("dump", "Dump all data about your user"), this::dump),
				new SlashCommand(new CommandData("stages", "Get a list of all available stages to warp to"), this::stages),
				new SlashCommand(new CommandData("help", "Get a usage message of this bot"), this::help),
				new SlashCommand(new CommandData("screenshot", "Create a screenshot of a running instance")
						.addOption(OptionType.INTEGER, "index", "Index of the running instance (0-based)", true), this::screenshot),
				new SlashCommand(new CommandData("toggle-pause", "Toggle paused-state of a running instance (fix \"crash\"")
						.addOption(OptionType.INTEGER, "index", "Index of the running instance (0-based)", true), this::togglePause)
		);
	}
	
	public String dest(SlashCommandEvent event) {
		DestUtil.onSetDest(event);
		return null; //messages are handled by method
	}
	
	public String getDest(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null) {
			return "No stage cached.";
		}
		
		String stage = settings.getStage();
		int scenario = settings.getScenario();
		String entry = settings.getEntry();
		System.out.println("Get-dest by "+event.getUser().getName()+": "+stage+", "+scenario+", "+entry);
		return "Your current destination: Stage: "+stage+", scenario: "+scenario+", entry: "+entry;
	}

	public String vdur(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null)
			settings = new UserSetting();
		
		int time = (int) event.getOption("duration").getAsLong();
		settings.setRecord(time);
		YuzuBot.storeUserSettings();
		System.out.println("Set record time for "+event.getUser().getName()+" to "+time);
		return "Stored! New recording time: "+(time == -1 ? "automatic" : time);
	}
	
	public String vstart(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null)
			settings = new UserSetting();
		
		int time = (int) event.getOption("start").getAsLong();
		settings.setRecordStart(time);
		YuzuBot.storeUserSettings();
		System.out.println("Set record start for "+event.getUser().getName()+" to "+time);
		return "Stored! New video start: "+time;
	}
	
	public String page(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null)
			settings = new UserSetting();
		
		int page = (int) event.getOption("page").getAsLong();
		settings.setPage(page);
		YuzuBot.storeUserSettings();
		System.out.println("Set page for "+event.getUser().getName()+" to "+page);
		return "Stored! New page: "+page;
	}

	public String reset(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.remove(event.getUser().getName());
		String dump = settings==null ? "" : settings.toString();
		YuzuBot.storeUserSettings();
		System.out.println("Reset all settings of "+event.getUser().getName()+" (dump: "+dump+")");
		return "dump: \""+dump+"\"\nDone: All your settings and information has been cleared.";
	}
	
	public String dump(SlashCommandEvent event) {
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null) {
			return "There is no data available for your user.";
		}

		System.out.println("Dump all settings of "+event.getUser().getName()+": "+settings.toString());
		return "Here is all data stored about your user: ```\n"+settings.toString()+"```";
	}
	
	public String stages(SlashCommandEvent event) {
		event.getHook().sendMessage("Here is a list of all available stages:")
				.addFile(String.join("\n", PlayerGo.STAGES).getBytes(), "stages.txt").setEphemeral(true).queue();
		return null; //sends message above
	}
	
	public String help(SlashCommandEvent event) {
		return "Set your destination with /dest first, then (if required) customize /record time. Send a script to start playback.";
	}
	
	public String screenshot(SlashCommandEvent event) {
		try {
			YuzuBot.handler.handleRequest(new ScreenshotRequest(event.getTextChannel(), event.getUser(), (int)event.getOption("index").getAsLong()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "Rendering.../Uploading..."; //responds publicly when done
	}
	
	public String togglePause(SlashCommandEvent event) {
		((ScriptHandlerMod)YuzuBot.handler).yuzuMgr.gameInstances.get((int)event.getOption("index").getAsLong()).togglePaused();
		event.getHook().sendMessage("Successfully toggled!").queue();
		return null;
	}

}
