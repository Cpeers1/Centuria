package org.asf.centuria.networking.http.api;

import java.io.IOException;
import java.util.Base64;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserHandler extends HttpPushProcessor {
	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());
			if (token.isBlank()) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			JsonObject payload = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();

			// Find account
			CenturiaAccount acc = manager.getAccount(payload.get("uuid").getAsString());
			if (acc == null) {
				this.setResponseStatus(401, "Unauthorized");
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
			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new UserHandler();
	}

	@Override
	public String path() {
		return "/u/user";
	}

}
