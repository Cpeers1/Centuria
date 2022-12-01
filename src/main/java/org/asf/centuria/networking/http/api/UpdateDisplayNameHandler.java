package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.stream.Stream;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.networking.chatserver.networking.SendMessage;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateDisplayNameHandler extends HttpUploadProcessor {

	private static String[] nameBlacklist = new String[] { "kit", "kitsendragn", "kitsendragon", "fera", "fero",
			"wwadmin", "ayli", "komodorihero", "wwsam", "blinky", "fer.ocity" };

	@Override
	public void process(String contentType, Socket client, String method) {
		Centuria.logger.info("API CALL: " + getRequest().path);
		try {
			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			ConnectiveHTTPServer.transferRequestBody(getHeaders(), getRequestBodyStream(), strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse body
			JsonObject change = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			String newName = change.get("new_display_name").getAsString().replace("+", " ");

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			JsonObject jP = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();

			// Find account
			CenturiaAccount acc = manager.getAccount(jP.get("uuid").getAsString());
			if (acc == null) {
				this.setResponseCode(401);
				this.setResponseMessage("Access denied");
				return;
			}

			// Check if the name is in use
			if (manager.isDisplayNameInUse(newName) && !manager.getUserByDisplayName(newName).equals(acc.getAccountID())
					|| (manager.isDisplayNameInUse(newName) && acc.isRenameRequired())) {
				setBody("application/json", "{\"error\":\"display_name_already_taken\"}");
				return; // Name is in use
			}

			// Save new name
			boolean requiredRename = acc.isRenameRequired();
			String oldName = acc.getDisplayName();
			if (acc.updateDisplayName(newName)) {
				if (!requiredRename) {
					// Unlock old name
					manager.releaseDisplayName(oldName);
				} else {
					// Wipe uuid tag of old display name
					manager.releaseDisplayName(oldName);
					manager.lockDisplayName(oldName, "-1");
				}

				// Tell authorization to save password
				manager.makePasswordUpdateRequested(acc.getAccountID());
			} else {
				JsonObject response = new JsonObject();
				if (!newName.matches("^[0-9A-Za-z\\-_. ]+") || newName.length() > 16 || newName.length() < 2) {
					response.addProperty("error", "display_name_invalid_format");
					setBody(response.toString());
					return;
				}

				// Prevent blacklisted names from being used
				for (String nm : nameBlacklist) {
					if (newName.equalsIgnoreCase(nm)) {
						response.addProperty("error", "display_name_sift_rejected");
						setBody(response.toString());
						return;
					}
				}

				// Prevent banned and filtered words
				for (String word : newName.split(" ")) {
					if (Stream.of(SendMessage.getInvalidWords())
							.anyMatch(t -> t.toLowerCase().equals(word.replaceAll("[^A-Za-z0-9]", "").toLowerCase()))) {
						response.addProperty("error", "display_name_sift_rejected");
						setBody(response.toString());
						return;
					}
				}
			}
		} catch (Exception e) {
			setResponseCode(500);
			setResponseMessage("Internal Server Error");
			Centuria.logger.error(getRequest().path + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public HttpUploadProcessor createNewInstance() {
		return new UpdateDisplayNameHandler();
	}

	@Override
	public String path() {
		return "/dn/update_display_name";
	}

}
