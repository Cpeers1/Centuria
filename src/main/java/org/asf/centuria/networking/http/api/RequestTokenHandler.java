package org.asf.centuria.networking.http.api;

import java.io.IOException;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;

public class RequestTokenHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Hardcoded response as i have no clue how to do this
		String challenge = "kOLl8r71tG1343qobkIvdJSGuXxUZBQUtHTq7Npe91l51TrpaGLZf4nPIjSCNxniUdpdHvOfcCzV2TQRn5MXab08vwGizt0NiDmzAdWrzQMYDjgTYz7Xqbzqds2LaYTa";
		String iv = "03KJ2tNeasisn7vI42W49IJpObpQirvu";

		// Build json
		JsonObject res = new JsonObject();
		res.addProperty("challenge", challenge);
		res.addProperty("iv", iv);
		this.setResponseContent("text/json", res.toString());
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new RequestTokenHandler();
	}

	@Override
	public String path() {
		return "/ca/request-token";
	}

}
