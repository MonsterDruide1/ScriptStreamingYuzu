package bot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

public class SlashCommandList {

	public HashMap<String, SlashCommand> commands;
	
	public SlashCommandList() {
		commands = new HashMap<>();
	}

	public void addCommand(SlashCommand command) {
		commands.put(command.getCommandData().getName(), command);
	}
	public void addCommands(SlashCommand... command) {
		Arrays.stream(command).forEach(c -> addCommand(c));
	}

	public void handleExecution(SlashCommandEvent event) {
		event.deferReply(true).queue();
		String message;
		if(commands.containsKey(event.getName()))
			message = commands.get(event.getName()).handleExecution(event);
		else
			message = "Unhandled slash-command. Please notify @MonsterDruide1 that /"+event.getName()+" is broken.";
		
		if(message != null)
			event.getHook().sendMessage(message).setEphemeral(true).queue();
	}
	
	public void applyCommands(Guild guild) throws InterruptedException, ExecutionException {
		CommandListUpdateAction action = guild.updateCommands();
		
		commands.values().forEach((c) -> action.addCommands(c.getCommandData()));
		
		List<Command> jdaCommands = action.submit().get();
		
		for(Command jdaCommand : jdaCommands) {
			SlashCommand slashCommand = commands.get(jdaCommand.getName());
			if(!slashCommand.publicCommand) {
				jdaCommand.updatePrivileges(guild, CommandPrivilege.enableUser(403475380532543488L)).queue(); //just myself
			}
		}
	}

}
