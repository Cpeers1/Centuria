package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.HttpPushHandler;

import com.google.gson.JsonObject;

public class DirectorProcessor extends HttpPushHandler {

	@Override
	public void handle(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Send response
		JsonObject response = new JsonObject();
		response.addProperty("smartfoxServer", "localhost");
		setResponseContent("text/json", response.toString());
	}

	@Override
	public HttpPushHandler createNewInstance() {
		return new DirectorProcessor();
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
