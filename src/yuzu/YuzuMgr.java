package yuzu;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;

import requests.IRequest;
import requests.ScreenshotRequest;

public class YuzuMgr {
	
	public ArrayList<Yuzu> gameInstances;
	
	public YuzuMgr() {
		gameInstances = new ArrayList<>();
	}
	
	public void addInstance(int clientPort, int serverPort) throws SocketException {
		addInstance(new Yuzu(clientPort, serverPort));
	}
	public void addInstance(Yuzu instance) {
		gameInstances.add(instance);
	}
	
	public void queueRequest(IRequest<File> request) throws InterruptedException {
		if(request instanceof ScreenshotRequest) {
			int index = ((ScreenshotRequest)request).index;
			gameInstances.get(index).queueRequest(request);
		} else {
			findFewestInstance().queueRequest(request);
		}
	}
	
	private Yuzu findFewestInstance() {
		return gameInstances.stream().min((y1, y2) -> Integer.compare(y1.getRequestsCount(), y2.getRequestsCount())).get();
	}

}
