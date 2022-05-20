package org.asf.emuferal.networking.http;

import java.net.Socket;

import org.asf.emuferal.EmuFeral;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class DirectorProcessor extends HttpUploadProcessor {

	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();

		try {
			switch (path) {
			case "/v1/bestGameServer": {
				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("smartfoxServer", EmuFeral.discoveryAddress); // load discovery address
				setBody(response.toString());
				break;
			}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new DirectorProcessor();
	}

	@Override
	public String path() {
		return "/";
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
