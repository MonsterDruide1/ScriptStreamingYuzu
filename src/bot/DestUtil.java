package bot;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import compression.BYMLDecompress;
import compression.SARCDecompress;
import config.UserSetting;
import main.Util;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import out.PlayerGo;

public class DestUtil {
	
	public static final String PATH_TO_SMO_ROMFS = "D:\\Eigene_Dateien\\Downloads\\SwitchGames\\Super Mario Odyssey\\romfsv0\\";
	
	public static void onSetDest(SlashCommandEvent event) {
		OptionMapping stageOption = event.getOption("stage");
		String stage = stageOption != null ? stageOption.getAsString() : "";
		if(!Arrays.stream(PlayerGo.STAGES).anyMatch(stage::equals)) {
			System.out.println(event.getUser().getName()+" requests StageList by "+stage);
			if(stage.equals("")) {
				event.getHook().sendMessage("No stage supplied. Here is a list of all available stages:")
						.addFile(String.join("\n", PlayerGo.STAGES).getBytes(), "stages.txt").setEphemeral(true).queue();
				return;
			}
			
			String[] filteredList = Arrays.stream(PlayerGo.STAGES).filter(s -> s.toLowerCase().contains(stage.toLowerCase())).toArray(String[]::new);
			
			if(filteredList.length != 0) {
				event.getHook().sendMessage("Stage does not exist. Use this command without parameters to get a list of *all* available stages.\n"
						+ "Here is a filtered list based on your inputs:")
						.addFile(String.join("\n", filteredList).getBytes(), stage+"_stages.txt").setEphemeral(true).queue();
			}
			else {
				event.getHook().sendMessage("Invalid stage supplied, and could not match any existing stages. Here is a list of all available stages:")
						.addFile(String.join("\n", PlayerGo.STAGES).getBytes(), "stages.txt").setEphemeral(true).queue();
			}
			return;
		}
		
		OptionMapping scenarioOption = event.getOption("scenario");
		long scenario = scenarioOption != null ? scenarioOption.getAsLong() : 0; //otherwise any invalid one so the text gets printed
		if(scenario < 1 || scenario > 15) {
			System.out.println(event.getUser().getName()+" requests ScenarioGuide by "+stage);
			event.getHook().sendMessage("Please supply a scenario number between 1 and 15.\n"
					+ "Check this page for a list of Scenarios for the main kingdoms: https://docs.google.com/spreadsheets/d/1jZLsqrkyUCxXHCVHuWsm2-Ec1gbJwLCLKTw8iMBzbUk/edit#gid=0")
					.setEphemeral(true).queue();
			return;
		}

		OptionMapping entryOption = event.getOption("entry");
		String entry = entryOption != null ? entryOption.getAsString() : "";
		
		String[] entries = null;
		try {
			entries = getEntries(stage, (int)scenario-1, "ChangeStageId"); //1-indexed vs 0-indexed
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		if(entries == null) {
			System.out.println(event.getUser().getName()+" requests EntryList by "+entry+" FAILED for "+stage+" ("+scenario+")");
			event.getHook().sendMessage("Something went wrong while trying to fetch valid entrances for this stage. Please notify @MonsterDruide1.").setEphemeral(true).queue();
			return;
		}

		if(!Arrays.stream(entries).anyMatch(entry::equals)) {
			System.out.println(event.getUser().getName()+" requests EntryList for "+stage+" ("+scenario+") by "+entry);
			if(entry.equals("")) {
				event.getHook().sendMessage("No entry supplied. Here is a list of all available entries for this stage:")
						.addFile(String.join("\n", entries).getBytes(), "entries_"+stage+".txt").setEphemeral(true).queue();
				return;
			}
			
			String[] filteredList = Arrays.stream(entries).filter(s -> s.toLowerCase().contains(entry.toLowerCase())).toArray(String[]::new);
			
			if(filteredList.length != 0) {
				event.getHook().sendMessage("Entry does not exist. Use this command without parameters to get a list of *all* available entries for this stage.\n"
						+ "Here is a filtered list based on your inputs:")
						.addFile(String.join("\n", filteredList).getBytes(), entry+"_entries_"+stage+".txt").setEphemeral(true).queue();
			}
			else {
				event.getHook().sendMessage("Invalid entry supplied, and could not match any existing entries. Here is a list of all available entries for this stage:")
						.addFile(String.join("\n", entries).getBytes(), "entries_"+stage+".txt").setEphemeral(true).queue();
			}
			return;
		}
		
		UserSetting settings = YuzuBot.userSettings.get(event.getUser().getName());
		if(settings == null)
			settings = new UserSetting();
		settings.setDest(stage, (int) scenario, entry);
		YuzuBot.userSettings.put(event.getUser().getName(), settings);
		YuzuBot.storeUserSettings();
		
		System.out.println("Set "+event.getUser().getName()+" to "+stage+", "+scenario+", "+entry);
		event.getHook().sendMessage("Stored! Current destination: Stage: "+stage+", scenario: "+scenario+", entry: "+entry).setEphemeral(true).queue();
	}
	
	public static String[] getEntries(String stage, int scenario, String propertyName) throws IllegalArgumentException, IllegalAccessException {
		String mapName = stage+"Map";
		File mapFile = new File(PATH_TO_SMO_ROMFS+"StageData\\"+mapName+".szs");
		try {
			SARCDecompress sarc = new SARCDecompress(Util.readFileToByte(mapFile));
			SARCDecompress.Node[] matches = Stream.of(sarc.nodes).filter(node -> node.fileName.equals(mapName+".byml")).toArray(SARCDecompress.Node[]::new);

			if(matches.length != 1) {
				System.err.println(matches.length+" != 1 SARC nodes found with the name "+mapName+".byml in "+mapFile.toString());
				return null;
			}
			
			return getEntries(new BYMLDecompress(matches[0].content), scenario, propertyName);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String[] getEntries(BYMLDecompress byml, int scenario, String propertyName) throws IllegalArgumentException, IllegalAccessException {
		ArrayList<String> entries = new ArrayList<String>();
		
		HashMap<String, Object> scenarioMap = (HashMap<String, Object>) ((Object[])byml.rootNode.content)[scenario];
		if(scenarioMap.containsKey("ZoneList")) {
			Object[] zones = (Object[]) scenarioMap.get("ZoneList");
			for(Object zone : zones) {
				HashMap<String, Object> zoneMap = (HashMap<String, Object>) zone;
				String zoneName = (String) zoneMap.get("UnitConfigName");
				entries.addAll(Arrays.asList(getEntries(zoneName, scenario, propertyName)));
			}
		}
		
		getEntriesRecursiveSearch("", scenarioMap, entries, propertyName);
		return entries.stream().sorted().distinct().toArray(String[]::new);
	}
	
	private static void getEntriesRecursiveSearch(String name, Object object, ArrayList<String> matches, String propertyName) throws IllegalArgumentException, IllegalAccessException {
		if (object == null || object instanceof Boolean || object instanceof Integer
				|| object instanceof Float || object instanceof Short || object instanceof Byte
				|| object instanceof Long || object instanceof Enum<?>) {
			return; //ignore, are of wrong type
		} else if(object instanceof String) {
			if(name.equals(propertyName)) {
				matches.add((String) object); //MATCH!!!!!!
			}
		}
		else if (object instanceof List<?>) {
			List<?> list = (List<?>) object;
			for (int i = 0; i < list.size(); i++) {
				getEntriesRecursiveSearch(i + "", list.get(i), matches, propertyName);
			}
			return;
		} else if (object.getClass().isArray()) {
			if (object.getClass().getComponentType().isPrimitive()) {
				return; //primitive is also wrong, has to be String or complex object
			} else {
				Object[] array = (Object[]) object;
				for (int i = 0; i < array.length; i++) {
					getEntriesRecursiveSearch(i + "", array[i], matches, propertyName);
				}
				return;
			}
		} else if (object instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) object;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				getEntriesRecursiveSearch(entry.getKey().toString(), entry.getValue(), matches, propertyName);
			}
			return;
		} else {
			if (!object.toString().startsWith("compression.")) {
				System.out.println("unknown type(" + name + "): " + object);
			}
			getEntriesRecursiveSearchAllOf(object, object.getClass(), matches, propertyName);
			return;
		}
	}
	private static void getEntriesRecursiveSearchAllOf(Object object, Class<?> clas, ArrayList<String> matches, String propertyName) throws IllegalArgumentException, IllegalAccessException {
		Field[] allFields = clas.getDeclaredFields();
		for (Field field : allFields) {
			if (!field.getName().startsWith("$SWITCH_TABLE$") && !field.getName().startsWith("this$")) {
				field.setAccessible(true);
				getEntriesRecursiveSearch(field.getName(), field.get(object), matches, propertyName);
			}
		}
		
		if(clas.getSuperclass() != null) {
			getEntriesRecursiveSearchAllOf(object, clas.getSuperclass(), matches, propertyName);
		}
	}

}
