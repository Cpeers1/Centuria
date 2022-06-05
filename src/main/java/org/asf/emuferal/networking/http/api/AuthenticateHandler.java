package org.asf.emuferal.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AuthenticateHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		try {
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			JsonObject login = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

			// Locate account
			String id = manager.authenticate(login.get("username").getAsString(),
					login.get("password").getAsString().toCharArray());

			// Check existence
			if (id == null) {
				// Check if registration is enabled, if not, prevent login
				if (!EmuFeral.allowRegistration || EmuFeral.gameServer.maintenance) {
					// Not sure what to send but sending this causes an error in the next request
					// triggering the client into saying invalid password.
					this.setResponseCode(200);
					return;
				}

				// Create account
				id = manager.register(login.get("username").getAsString());
				if (id == null) {
					// Invalid details
					this.setResponseCode(200);
					return;
				}
			}

			// Check password save request
			if (manager.isPasswordUpdateRequested(id)) {
				// Update password
				manager.updatePassword(id, login.get("password").getAsString().toCharArray());
			}

			// Find account
			EmuFeralAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			boolean changeName = false;
			// Check if the name is in use and not owned by the current user
			if (manager.isDisplayNameInUse(acc.getDisplayName())
					&& !manager.getUserByDisplayName(acc.getDisplayName()).equals(acc.getAccountID())) {
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
			String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

			JsonObject payload = new JsonObject();
			payload.addProperty("iat", System.currentTimeMillis() / 1000);
			payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (7 * 24 * 60 * 60));
			payload.addProperty("jti", UUID.randomUUID().toString());
			payload.addProperty("iss", "EmuFeral");
			payload.addProperty("sub", "EmuFeral");
			payload.addProperty("uuid", id);
			String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

			// Send response
			JsonObject response = new JsonObject();
			response.addProperty("uuid", id);
			response.addProperty("refresh_token",
					UUID.randomUUID() + "-" + UUID.randomUUID() + "-" + UUID.randomUUID());
			response.addProperty("auth_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
					.encodeToString(EmuFeral.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
			response.addProperty("rename_required", !manager.hasPassword(id) || changeName || acc.isRenameRequired());
			response.addProperty("rename_required_key", "");
			response.addProperty("email_update_required", false);
			response.addProperty("email_update_required_key", "");
			setBody(response.toString());
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
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
