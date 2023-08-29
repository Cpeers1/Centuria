package org.asf.centuria.networking.http.director;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;

public class GameServerRequestHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Send response
		JsonObject response = new JsonObject();
		response.addProperty("smartfoxServer", Centuria.discoveryAddress); // load discovery address
		setResponseContent("text/json", response.toString());
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new GameServerRequestHandler();
	}

	@Override
	public String path() {
		return "/v1/bestGameServer";
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
