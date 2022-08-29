package handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import requests.IRequest;

public abstract class ScriptHandler {

	public abstract void handleRequest(IRequest<File> request) throws InterruptedException;
	
	public static String waitForCommand(String command) {
		try {
			Process process = Runtime.getRuntime().exec(command);
			StringBuilder builder = new StringBuilder();
			BufferedReader inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while(process.isAlive()) {
				if(inReader.ready()) {
					String line = inReader.readLine();
					if(line!=null) {
						builder.append(line+"\n");
					}
				}
				if(errReader.ready()) {
					String line = errReader.readLine();
					if(line!=null) {
						builder.append(line+"\n");
					}
				}
			}
			return builder.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
