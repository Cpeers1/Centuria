package org.asf.centuria.networking.http.api;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Base64;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.processors.HttpUploadProcessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UpdateDisplayNameHandler extends HttpUploadProcessor {
	@Override
	public void process(String contentType, Socket client, String method) {
		Centuria.logger.debug("API CALL: " + getRequest().path);
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
			String newName = change.get("new_display_name").getAsString();

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
				return; // Name is in use
			}

			// Save new name
			String oldName = acc.getDisplayName();
			if (acc.updateDisplayName(newName)) {
				if (!acc.isRenameRequired()) {
					// Unlock old name
					manager.releaseDisplayName(oldName);
				}

				// Tell authorization to save password
				manager.makePasswordUpdateRequested(acc.getAccountID());
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
