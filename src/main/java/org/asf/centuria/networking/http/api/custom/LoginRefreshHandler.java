package org.asf.centuria.networking.http.api.custom;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.networking.http.api.FallbackAPIProcessor;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LoginRefreshHandler extends HttpPushProcessor {
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

			// Parse token
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
			String id = payload.get("uuid").getAsString();

			// Check existence
			if (id == null) {
				// Invalid details
				this.setResponseContent("text/json", "{\"error\":\"invalid_credential\"}");
				this.setResponseStatus(401, "Unauthorized");
				return;
			}

			// Find account
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseStatus(401, "Unauthorized");
				return;
			}

			boolean changeName = false;
			// Check if the name is in use and not owned by the current user
			if (manager.isDisplayNameInUse(acc.getDisplayName())
					&& (manager.getUserByDisplayName(acc.getDisplayName()) == null
							|| !manager.getUserByDisplayName(acc.getDisplayName()).equals(acc.getAccountID()))) {
				// Name is in use, request change
				changeName = true;
			} else {
				// Lock display name
				manager.lockDisplayName(acc.getDisplayName(), acc.getAccountID());
			}

			// Build JWT
			JsonObject headers = new JsonObject();
			headers.addProperty("alg", "RS256");
			headers.addProperty("kid", FallbackAPIProcessor.KeyID);
			headers.addProperty("typ", "JWT");
			String headerD = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(headers.toString().getBytes("UTF-8"));

			payload = new JsonObject();
			payload.addProperty("iat", System.currentTimeMillis() / 1000);
			payload.addProperty("jti", UUID.randomUUID().toString());
			payload.addProperty("iss", "Centuria");
			payload.addProperty("sub", "Centuria");
			payload.addProperty("uuid", id);

			// Send response
			JsonObject response = new JsonObject();
			response.addProperty("uuid", id);

			// Generate refresh token
			payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (30 * 24 * 60 * 60));
			String payloadD = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(payload.toString().getBytes("UTF-8"));
			response.addProperty("refresh_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
					.withoutPadding().encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8"))));

			// Generate auth token
			payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (2 * 24 * 60 * 60));
			payload.addProperty("acs", "gameplay");
			payloadD = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes("UTF-8"));
			response.addProperty("auth_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder().withoutPadding()
					.encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8"))));

			// Add other fields
			response.addProperty("rename_required", !manager.hasPassword(id) || changeName || acc.isRenameRequired());
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
		return new LoginRefreshHandler();
	}

	@Override
	public String path() {
		return "/centuria/refreshtoken";
	}

}
