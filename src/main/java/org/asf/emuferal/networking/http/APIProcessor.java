package org.asf.emuferal.networking.http;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class APIProcessor extends HttpUploadProcessor {

	public static String KeyID = UUID.randomUUID().toString();

	@Override
	public void process(String contentType, Socket client, String method) {
		String path = this.getRequestPath();
		AccountManager manager = AccountManager.getInstance();

		try {
			// Parse body (if present)
			byte[] body = new byte[0];
			if (method.toUpperCase().equals("POST")) {
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
				body = strm.toByteArray();
				strm.close();
			}

			switch (path) {
			case "/ca/request-token": {
				// Hardcoded response as i have no clue how to do this
				String challenge = "kOLl8r71tG1343qobkIvdJSGuXxUZBQUtHTq7Npe91l51TrpaGLZf4nPIjSCNxniUdpdHvOfcCzV2TQRn5MXab08vwGizt0NiDmzAdWrzQMYDjgTYz7Xqbzqds2LaYTa";
				String iv = "03KJ2tNeasisn7vI42W49IJpObpQirvu";

				// Build json
				JsonObject res = new JsonObject();
				res.addProperty("challenge", challenge);
				res.addProperty("iv", iv);
				this.setBody(res.toString().getBytes("UTF-8"));
				break;
			}
			case "/a/authenticate": {
				// Parse body
				JsonObject login = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

				// Locate account
				String id = manager.authenticate(login.get("username").getAsString(),
						login.get("password").getAsString().toCharArray());

				// Check existence
				if (id == null) {
					// Check if registration is enabled, if not, prevent login
					if (!EmuFeral.allowRegistration) {
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
					this.setResponseCode(403);
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
				headers.addProperty("kid", KeyID);
				headers.addProperty("typ", "JWT");
				String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

				JsonObject payload = new JsonObject();
				payload.addProperty("iat", System.currentTimeMillis() / 1000);
				payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (24 * 60 * 60));
				payload.addProperty("iss", "EmuFeral");
				payload.addProperty("sub", "EmuFeral");
				payload.addProperty("uuid", id);
				String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

				// Send response
				JsonObject response = new JsonObject();
				response.addProperty("uuid", id);
				response.addProperty("refresh_token", id);
				response.addProperty("auth_token", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
						.encodeToString(EmuFeral.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
				response.addProperty("rename_required", !manager.hasPassword(id) || changeName); // request user name if
																									// new or migrating
				response.addProperty("rename_required_key", "");
				response.addProperty("email_update_required", false);
				response.addProperty("email_update_required_key", "");
				setBody(response.toString());
				break;
			}
			case "/dn/update_display_name": {
				// Parse body
				JsonObject change = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
				String newName = change.get("new_display_name").getAsString();

				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					return;
				}

				JsonObject jP = JsonParser
						.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
						.getAsJsonObject();

				// Find account
				EmuFeralAccount acc = manager.getAccount(jP.get("uuid").getAsString());
				if (acc == null) {
					this.setResponseCode(403);
					this.setResponseMessage("Access denied");
					return;
				}

				// Check if the name is in use
				if (manager.isDisplayNameInUse(newName)
						&& !manager.getUserByDisplayName(newName).equals(acc.getAccountID())) {
					return; // Name is in use
				}

				// Save new name
				String oldName = acc.getDisplayName();
				if (acc.updateDisplayName(newName)) {
					// Unlock old name
					manager.releaseDisplayName(oldName);

					// Tell authorization to save password
					manager.makePasswordUpdateRequested(acc.getAccountID());
				}

				break;
			}
			case "/u/user": {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
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
					this.setResponseCode(403);
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
				break;
			}
			case "/u/settings": {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
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
					this.setResponseCode(403);
					this.setResponseMessage("Access denied");
					return;
				}

				// Parse body
				JsonObject settings = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
				// TODO

				token = token;

				break;
			}
			case "/xp/xp-details": {
				setBody(""); // should cause the question mark // TODO: levels
				break;
			}
			case "/i/display_names": {
				// Parse JWT payload
				String token = this.getHeader("Authorization").substring("Bearer ".length());

				// Verify signature
				String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
				String sig = token.split("\\.")[2];
				if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
					this.setResponseCode(403);
					this.setResponseMessage("Access denied");
					return;
				}

				// Parse body
				JsonObject req = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();

				// Send response
				JsonObject response = new JsonObject();
				JsonArray found = new JsonArray();
				JsonArray unrecognized = new JsonArray();
				for (JsonElement uuid : req.get("uuids").getAsJsonArray()) {
					// Find account
					String id = uuid.getAsString();
					EmuFeralAccount acc = manager.getAccount(id);
					if (acc != null) {
						JsonObject d = new JsonObject();
						d.addProperty("display_name", acc.getDisplayName());
						d.addProperty("uuid", id);
						found.add(d);
					} else {
						unrecognized.add(id);
					}
				}
				response.add("found", found);
				response.add("not_found", (unrecognized.size() == 0 ? null : unrecognized));
				setBody(response.toString());

				break;
			}
			default:
				if (path.startsWith("/ca/auth/")) {
					// Build the JWT
					JsonObject headers = new JsonObject();
					headers.addProperty("alg", "RS256");
					headers.addProperty("kid", KeyID);
					headers.addProperty("typ", "JWT");
					String headerD = Base64.getUrlEncoder().encodeToString(headers.toString().getBytes("UTF-8"));

					JsonObject payload = new JsonObject();
					payload.addProperty("iat", System.currentTimeMillis() / 1000);
					payload.addProperty("exp", (System.currentTimeMillis() / 1000) + (24 * 60 * 60));
					payload.addProperty("iss", "EmuFeral");
					payload.addProperty("sub", "EmuFeral");
					String payloadD = Base64.getUrlEncoder().encodeToString(payload.toString().getBytes("UTF-8"));

					// Send response
					JsonObject response = new JsonObject();
					response.addProperty("autorization_key", headerD + "." + payloadD + "." + Base64.getUrlEncoder()
							.encodeToString(EmuFeral.sign((headerD + "." + payloadD).getBytes("UTF-8"))));
					setBody(response.toString());
				} else {
					setResponseCode(400);
					path = path;
					setBody("{}");
				}
				break;
			}
		} catch (Exception e) {
			e = e;
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new APIProcessor();
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
