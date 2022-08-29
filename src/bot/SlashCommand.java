package bot;

import java.util.function.Function;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class SlashCommand {
	
	private CommandData data;
	Function<SlashCommandEvent, String> handler;
	boolean publicCommand;

	public SlashCommand(CommandData data, Function<SlashCommandEvent, String> handler) {
		this(data, handler, true);
	}
	public SlashCommand(CommandData data, Function<SlashCommandEvent, String> handler, boolean publicCommand) {
		this.data = data;
		this.handler = handler;
		this.publicCommand = publicCommand;
	}
	
	public String handleExecution(SlashCommandEvent event) {
		return handler.apply(event);
	}
	
	public CommandData getCommandData() {
		if(!publicCommand) data.setDefaultEnabled(false);
		return data;
	}

}
