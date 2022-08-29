package handler;

import java.io.File;
import java.net.SocketException;

import requests.IRequest;
import yuzu.YuzuMgr;

public class ScriptHandlerMod extends ScriptHandler {
	
	public YuzuMgr yuzuMgr;
	
	public ScriptHandlerMod() {
		yuzuMgr = new YuzuMgr();
		try {
			yuzuMgr.addInstance(7901, 7902);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void handleRequest(IRequest<File> request) throws InterruptedException {
		yuzuMgr.queueRequest(request);
	}

}
