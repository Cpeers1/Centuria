package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AuthenticateHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			Centuria.logger.info("API CALL: " + getRequest().path);
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			String id = null;

			// Parse body
			JsonObject login = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (login.get("username").getAsString().equals("sys://fromtoken")) {
				// Token-based autologin or just from a token
				String token = login.get("password").getAsString();

				// Parse token
				if (token.isBlank()) {
					this.setResponseCode(403);
					this.setResponseMessage("Access denied");
					return;
				}

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
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
				id = payload.get("uuid").getAsString();
			} else {
				// Locate account
				id = manager.authenticate(login.get("username").getAsString(),
						login.get("password").getAsString().toCharArray());

			}

			// Check existence
			if (id == null) {
				// Check if registration is enabled, if not, prevent login
				if (!Centuria.allowRegistration || Centuria.gameServer.maintenance) {
					// Invalid details
					this.setBody("text/json", "{\"error\":\"invalid_credential\"}");
					this.setResponseCode(422);
					return;
				}

				// Create account
				id = manager.register(login.get("username").getAsString());
				if (id == null) {
					// Invalid details
					this.setBody("text/json", "{\"error\":\"invalid_credential\"}");
					this.setResponseCode(422);
					return;
				}
			}

			// Check password save request
			if (manager.isPasswordUpdateRequested(id)) {
				// Update password
				manager.updatePassword(id, login.get("password").getAsString().toCharArray());
			}

			// Find account
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
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

			JsonObject payload = new JsonObject();
			payload.addProperty("iat", System.currentTimeMillis() / 1000);
			payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (2 * 24 * 60 * 60));
			payload.addProperty("jti", UUID.randomUUID().toString());
			payload.addProperty("iss", "Centuria");
			payload.addProperty("sub", "Centuria");
			payload.addProperty("uuid", id);
			String payloadD = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(payload.toString().getBytes("UTF-8"));

			// Send response
			JsonObject response = new JsonObject();
			response.addProperty("uuid", id);
			response.addProperty("refresh_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
					.withoutPadding().encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
			response.addProperty("auth_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder().withoutPadding()
					.encodeToString(Centuria.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
			response.addProperty("rename_required", !manager.hasPassword(id) || changeName || acc.isRenameRequired());
			response.addProperty("rename_required_key", "");
			response.addProperty("email_update_required", false);
			response.addProperty("email_update_required_key", "");
			setBody("text/json", response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new AuthenticateHandler();
	}

	@Override
	public String path() {
		return "/a/authenticate";
	}

}
