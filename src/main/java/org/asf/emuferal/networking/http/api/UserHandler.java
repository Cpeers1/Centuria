package org.asf.emuferal.networking.http.api;

import java.net.Socket;
import java.util.Base64;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {

		try {
			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());
			if (token.isBlank()) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseCode(403);
				this.setResponseMessage("Access denied");
				return;
			}

			JsonObject payload = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();

			// Find account
			EmuFeralAccount acc = manager.getAccount(payload.get("uuid").getAsString());
			if (acc == null) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			// Send a response
			JsonObject privacy = acc.getPrivacySettings();
			JsonObject response = new JsonObject();
			response.addProperty("country_code", "US");
			response.addProperty("display_name", acc.getDisplayName());
			response.addProperty("enhanced_customization", true);
			response.addProperty("language", "en");
			response.add("privacy", privacy);
			response.addProperty("username", acc.getLoginName());
			response.addProperty("uuid", acc.getAccountID());
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
		}
	}

	@Override
	public boolean supportsGet() {
		return true;
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new UserHandler();
	}

	@Override
	public String path() {
		return "/u/user";
	}

}
