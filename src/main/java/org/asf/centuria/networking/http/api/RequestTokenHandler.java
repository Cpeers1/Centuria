package org.asf.centuria.networking.http.api;

import java.net.Socket;

import org.asf.centuria.Centuria;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;

public class RequestTokenHandler extends HttpUploadProcessor {

	@Override
	public void process(String contentType, Socket client, String method) {
		Centuria.logger.info("API CALL: " + getRequest().path);
		// Hardcoded response as i have no clue how to do this
		String challenge = "kOLl8r71tG1343qobkIvdJSGuXxUZBQUtHTq7Npe91l51TrpaGLZf4nPIjSCNxniUdpdHvOfcCzV2TQRn5MXab08vwGizt0NiDmzAdWrzQMYDjgTYz7Xqbzqds2LaYTa";
		String iv = "03KJ2tNeasisn7vI42W49IJpObpQirvu";

		// Build json
		JsonObject res = new JsonObject();
		res.addProperty("challenge", challenge);
		res.addProperty("iv", iv);
		this.setBody("text/json", res.toString());
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new RequestTokenHandler();
	}

	@Override
	public String path() {
		return "/ca/request-token";
	}

}
