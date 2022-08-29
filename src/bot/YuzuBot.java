package bot;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.security.auth.login.LoginException;

import config.UserSetting;
import handler.ScriptHandler;
import handler.ScriptHandlerMod;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import requests.ScriptRequest;
import server.Main;

public class YuzuBot extends ListenerAdapter {
	
	//TODO ideas: cancel-button below request, /aspam /bspam
	
	public static ScriptHandler handler = new ScriptHandlerMod();
	public static HashMap<String, UserSetting> userSettings = new HashMap<>();
	public static CustomSlashCommandList slashCommands = new CustomSlashCommandList();
	
	public static void main(String[] args) throws LoginException, InterruptedException, IOException {
		String secret = Files.readString(Path.of("Secret.txt"));
		JDA jda = JDABuilder.createDefault(secret).build();
		jda.addEventListener(new YuzuBot());
		loadUserSettings();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> storeUserSettings()));
		jda.awaitReady();
		System.out.println("READY!");
		
		/*
		UserSetting setting = new UserSetting();
		setting.setDest("RollingExStage", 3, "rollingstart");
		userSettings.put("I'm lazy so this sends my script", setting);
		*/
		
		// responds to commands like tp or go, as well as exit/quit
		new Main().startLoop();
	}
	
	public static void loadUserSettings() {
		String userSettingsString = Preferences.userRoot().node(YuzuBot.class.getName()).get("user_settings", "");
		for(String user : userSettingsString.split(",")) {
			if(user.isBlank()) continue;
			String username = URLDecoder.decode(user.split("=")[0], StandardCharsets.UTF_8);
			String settings = URLDecoder.decode(user.split("=")[1], StandardCharsets.UTF_8);
			
			userSettings.put(username, new UserSetting(settings));
		}
	}
	
	private static String storeUserSettingsToString() {
		return String.join(",", 
				userSettings.entrySet().stream().map(
						e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8)
				).toList()
		);
	}
	public static void storeUserSettings() {
		String userSettingsString = storeUserSettingsToString();
		
		Preferences.userRoot().node(YuzuBot.class.getName()).put("user_settings", userSettingsString);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.isFromType(ChannelType.PRIVATE)) {
			System.out.printf("[PM] %s: %s\n", event.getAuthor().getName(), event.getMessage().getContentDisplay());
		} else {
			if(event.getMessage().getContentStripped().equals("createSlashCommands")) {
				try {
					slashCommands.applyCommands(event.getGuild());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
				event.getChannel().sendMessage("Done!").queue();
			}
			
			if(event.getTextChannel().getName().startsWith("script-streaming-")) {
				String text = event.getMessage().getContentStripped();
				
				if(isMessageURL(text)) {
					String[] parts = text.split("\\/");
					CompletableFuture<Message> referencedMessage = event.getGuild().getTextChannelById(parts[5]).retrieveMessageById(parts[6]).submit();
					referencedMessage.thenAccept(this::checkScriptAndExecute);
				} else {
					checkScriptAndExecute(event.getMessage());
				}
			}
			
		}
	}
	
	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		slashCommands.handleExecution(event);
	}
	
	public void checkScriptAndExecute(Message message) {
		if(message.getAttachments().size() > 0 && 
				!message.getAttachments().get(0).isImage() &&
				message.getAttachments().get(0).getFileExtension().equals("txt") &&
				message.getAttachments().get(0).getContentType().contains("text")) {
			System.out.printf("[%s][%s] %s: %s\n", message.getGuild().getName(), message.getTextChannel().getName(),
					message.getAuthor().getName(), message.getContentDisplay());
			
			try {
				handler.handleRequest(new ScriptRequest(
						message.getGuild().getTextChannelsByName("script-streaming-text", false).get(0),
						message));
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// does not check if it is valid, just roughly compares its format using the starting part and length (to check if *only* the URL is sent)
	public static boolean isMessageURL(String text) {
		// https://discord.com/channels/829256569354321951/829956528735125504/892417337351864382
		return text.startsWith("https://discord.com/channels/") && (text.length() == 85 || text.length() == 86);
	}
}
