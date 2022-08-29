package config;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UserSetting {
	
	private String stage = null;
	private int scenario = -1;
	private String entry = null;
	private int record = -1;
	private int recordStart = 0;
	private int page = 5;
	
	public UserSetting() {}
	
	public UserSetting(String configString) {
		for(String part : configString.split(",")) {
			if(part.isBlank()) continue;
			String value = URLDecoder.decode(part.split("=")[1], StandardCharsets.UTF_8);
			switch(part.split("=")[0]) {
			case "stage" -> stage = value;
			case "scenario" -> scenario = Integer.parseInt(value);
			case "entry" -> entry = value;
			case "record" -> record = Integer.parseInt(value);
			case "recordStart" -> recordStart = Integer.parseInt(value);
			case "page" -> page = Integer.parseInt(value);
			default -> System.err.println("Error parsing setting "+part.split("=")[0]+", value="+value);
			}
		}
	}
	
	@Override
	public String toString() {
		String string = "";
		string += "stage="+URLEncoder.encode(stage, StandardCharsets.UTF_8)+",";
		string += "scenario="+URLEncoder.encode(scenario+"", StandardCharsets.UTF_8)+",";
		string += "entry="+URLEncoder.encode(entry, StandardCharsets.UTF_8)+",";
		string += "record="+URLEncoder.encode(record+"", StandardCharsets.UTF_8)+",";
		string += "recordStart="+URLEncoder.encode(recordStart+"", StandardCharsets.UTF_8)+",";
		string += "page="+URLEncoder.encode(page+"", StandardCharsets.UTF_8);
		return string;
	}
	
	public void setDest(String stage, int scenario, String entry) {
		this.stage = stage;
		this.scenario = scenario;
		this.entry = entry;
	}
	public void setRecord(int record) {
		this.record = record;
	}
	public void setRecordStart(int record) {
		this.recordStart = record;
	}
	public void setPage(int page) {
		this.page = page;
	}

	public String getStage() {
		return stage;
	}
	public int getScenario() {
		return scenario;
	}
	public String getEntry() {
		return entry;
	}
	public int getRecord() {
		return record;
	}
	public int getRecordStart() {
		return recordStart;
	}
	public int getPage() {
		return page;
	}

}
