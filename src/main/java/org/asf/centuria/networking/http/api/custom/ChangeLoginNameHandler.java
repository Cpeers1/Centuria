package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Stream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.networking.chatserver.networking.SendMessage;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChangeLoginNameHandler extends HttpPushProcessor {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			if (!method.equalsIgnoreCase("post")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			getRequest().transferRequestBody(strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("new_login_name")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

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

			// Prepare response
			JsonObject response = new JsonObject();

			// Check if the name is in use
			String newName = payload.get("new_login_name").getAsString();
			if (manager.getAccount(newName) != null) {
				response.addProperty("status", "failure");
				response.addProperty("error", "login_name_in_use");
				response.addProperty("error_message", "Selected login name is already in use.");
				return; // Name is in use
			}

			// Save new name
			String oldName = acc.getLoginName();
			if (acc.updateLoginName(newName)) {
				// Wipe lock
				manager.releaseLoginName(oldName);
			} else {
				if (!newName.matches("^[A-Za-z0-9@._#]+$") || newName.contains(".cred")
						|| !newName.matches(".*[A-Za-z0-9]+.*") || newName.isBlank() || newName.length() > 320) {
					// Reply with error
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_login_name");
					response.addProperty("error_message", "Invalid login name.");
					setResponseContent("text/json", response.toString());
					return;
				}

				// Prevent blacklisted names from being used
				for (String nm : nameBlacklist) {
					if (newName.equalsIgnoreCase(nm)) {
						response.addProperty("status", "failure");
						response.addProperty("error", "invalid_login_name");
						response.addProperty("error_message",
								"Invalid login name: this name may not be used as it may not be appropriate.");
						setResponseContent("text/json", response.toString());
						return;
					}
				}

				// Prevent banned and filtered words
				for (String word : newName.split(" ")) {
					if (Stream.of(SendMessage.getInvalidWords())
							.anyMatch(t -> t.toLowerCase().equals(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase()))) {
						response.addProperty("status", "failure");
						response.addProperty("error", "invald_login_name");
						response.addProperty("error_message",
								"Invalid login name: this name may not be used as it may not be appropriate.");
						setResponseContent("text/json", response.toString());
						return;
					}
				}
			}

			// Send response
			response.addProperty("status", "success");
			response.addProperty("login_name", acc.getLoginName());
			response.addProperty("uuid", id);
			response.addProperty("updated", true);
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
		return new ChangeLoginNameHandler();
	}

	@Override
	public String path() {
		return "/centuria/updateloginname";
	}

}
