package org.asf.centuria.tools.legacyclienttools;

import java.net.Socket;

import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DirectorProcessor extends HttpUploadProcessor {

	public void process(String contentType, Socket client, String method) {
		// Send response
		JsonObject response = new JsonObject();
		response.addProperty("smartfoxServer", "localhost");
		setBody(response.toString());
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new DirectorProcessor();
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
