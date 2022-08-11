package org.asf.centuria.networking.http.director;

import java.net.Socket;

import org.asf.centuria.Centuria;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class GameServerRequestHandler extends HttpUploadProcessor {

	public void process(String contentType, Socket client, String method) {
		// Send response
		JsonObject response = new JsonObject();
		response.addProperty("smartfoxServer", Centuria.discoveryAddress); // load discovery address
		setBody(response.toString());
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new GameServerRequestHandler();
	}

	@Override
	public String path() {
		return "/v1/bestGameServer";
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
